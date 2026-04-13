package org.jeecg.ai.stream;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.invocation.InvocationContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
* @Description: 流式输出自定义类
*
* @author: wangshuai
* @date: 2025/12/18 12:01
*/
@Slf4j
public class InternalTokenStream implements TokenStream {
    private final StreamingChatModel model;
    private final List<ToolSpecification> toolSpecifications;
    private final Map<String, ToolExecutor> toolExecutors;
    private final ChatMemory chatMemory;
    private final List<Content> retrievedContents;

    private Consumer<String> onPartialResponse;
    @Setter
    @Getter
    private Consumer<PartialThinking> onPartialThinking;
    private Consumer<Throwable> onError;
    private Runnable onComplete;
    private Consumer<List<Content>> onRetrieved;
    private Consumer<ToolExecution> onToolExecuted;
    private Consumer<BeforeToolExecution> beforeToolExecutionHandler;
    private Consumer<ChatResponse> onCompleteResponse;
    private Consumer<ChatResponse> onIntermediateResponse;
    /** 当前轮用户消息快照：用于 ChatMemory 淘汰策略把 UserMessage 删掉后，在 sanitize 阶段回填，避免工具调用丢失原始意图 */
    private UserMessage currentTurnUserMessage;

    public InternalTokenStream(StreamingChatModel model,
                               List<ToolSpecification> toolSpecifications,
                               Map<String, ToolExecutor> toolExecutors,
                               ChatMemory chatMemory,
                               List<Content> retrievedContents) {
        this.model = model;
        this.toolSpecifications = toolSpecifications;
        this.toolExecutors = toolExecutors;
        this.chatMemory = chatMemory;
        this.retrievedContents = retrievedContents;
    }

    /**
     * 设置部分响应监听器
     *
     * @param onPartialResponse 部分响应监听器
     * @return 当前 TokenStream 实例
     */
    @Override
    public TokenStream onPartialResponse(Consumer<String> onPartialResponse) {
        this.onPartialResponse = onPartialResponse;
        return this;
    }

    /**
     * 设置部分思考过程监听器
     *
     * @param onPartialThinking 部分思考过程监听器
     * @return 当前 TokenStream 实例
     */
    @Override
    public TokenStream onPartialThinking(Consumer<dev.langchain4j.model.chat.response.PartialThinking> onPartialThinking) {
        this.onPartialThinking = onPartialThinking;
        return this;
    }

    /**
     * 设置错误监听器
     *
     * @param onError 错误监听器
     * @return 当前 TokenStream 实例
     */
    @Override
    public TokenStream onError(Consumer<Throwable> onError) {
        this.onError = onError;
        return this;
    }

    /**
     * 忽略错误
     *
     * @return 当前 TokenStream 实例
     */
    @Override
    public TokenStream ignoreErrors() {
        return this;
    }

    /**
     * 设置检索内容监听器
     *
     * @param onRetrieved 检索内容监听器
     * @return 当前 TokenStream 实例
     */
    @Override
    public TokenStream onRetrieved(Consumer<List<Content>> onRetrieved) {
        this.onRetrieved = onRetrieved;
        return this;
    }

    /**
     * 设置工具执行之前监听器
     *
     * @param beforeToolExecutionHandler 工具执行之前监听器
     * @return 当前 TokenStream 实例
     * @author sjlei
     */
    @Override
    public TokenStream beforeToolExecution(Consumer<BeforeToolExecution> beforeToolExecutionHandler) {
        this.beforeToolExecutionHandler = beforeToolExecutionHandler;
        return this;
    }

    /**
     * 设置工具执行监听器
     *
     * @param onToolExecuted 工具执行监听器
     * @return 当前 TokenStream 实例
     */
    @Override
    public TokenStream onToolExecuted(Consumer<ToolExecution> onToolExecuted) {
        this.onToolExecuted = onToolExecuted;
        return this;
    }

    /**
     * 设置完整响应监听器
     *
     * @param onCompleteResponse 完整响应监听器
     * @return 当前 TokenStream 实例
     */
    @Override
    public TokenStream onCompleteResponse(Consumer<ChatResponse> onCompleteResponse) {
        this.onCompleteResponse = onCompleteResponse;
        return this;
    }

    /**
     * 设置中间响应监听器
     *
     * @param onIntermediateResponse 中间响应监听器
     * @return 当前 TokenStream 实例
     */
    @Override
    public TokenStream onIntermediateResponse(Consumer<ChatResponse> onIntermediateResponse) {
        this.onIntermediateResponse = onIntermediateResponse;
        return this;
    }

    /**
     * 启动 TokenStream
     */
    @Override
    public void start() {
        if (onRetrieved != null && retrievedContents != null) {
            onRetrieved.accept(retrievedContents);
        }
        //update-begin---wangshuai---date:20260413  for：[issue/1560]/[issues/9527]AI应用调用千问qwen-plus 大模型 提示messages and prompt must not all null #18-----------
        // 快照当前轮的 UserMessage（chatMemory 里最后一条 UserMessage），供 sanitize 兜底回填使用
        List<ChatMessage> initial = chatMemory.messages();
        for (int i = initial.size() - 1; i >= 0; i--) {
            if (initial.get(i) instanceof UserMessage) {
                this.currentTurnUserMessage = (UserMessage) initial.get(i);
                break;
            }
        }
        //update-end---author:wangshuai ---date:20260413  for：[issue/1560]/[issues/9527]AI应用调用千问qwen-plus 大模型 提示messages and prompt must not all null #18------------
        doChat();
    }

    /**
     * 执行聊天
     */
    private void doChat() {
        ChatRequest.Builder requestBuilder = ChatRequest.builder()
                .messages(sanitizeForRequest(chatMemory.messages()));

        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            requestBuilder.toolSpecifications(toolSpecifications);
        }

        //流式输出
        model.chat(requestBuilder.build(), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                try {
                    if (onPartialResponse != null) {
                        onPartialResponse.accept(token);
                    }
                } catch (Exception e) {
                    log.warn("Error processing partial response: {}", e.getMessage());
                }
            }

            /**
             * 推送思考过程
             * 
             * @param partialThinking
             */
            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                try {
                    if (onPartialThinking != null) {
                        onPartialThinking.accept(partialThinking);
                    }
                } catch (Exception e) {
                    log.error("Error processing partial thinking: {}", e.getMessage());
                }
            }

            /**
             * 消息完成
             * @param completeResponse
             */
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                AiMessage aiMessage = completeResponse.aiMessage();
                chatMemory.add(aiMessage);

                if (aiMessage.hasToolExecutionRequests()) {
                    if (onIntermediateResponse != null) {
                        try {
                            onIntermediateResponse.accept(completeResponse);
                        } catch (Exception e) {
                            log.error("Error in onIntermediateResponse callback: {}", e.getMessage());
                        }
                    }
                    //找对应的工具类
                    for (ToolExecutionRequest toolExecReq : aiMessage.toolExecutionRequests()) {
                        ToolExecutor executor = toolExecutors.get(toolExecReq.name());
                        if (executor == null) {
                            if (onError != null) {
                                onError.accept(new IllegalStateException("未找到工具执行器: " + toolExecReq.name()));
                            }
                            return;
                        }

                        // 工具执行前回调
                        if (beforeToolExecutionHandler != null) {
                            try {
                                BeforeToolExecution beforeToolExecution = BeforeToolExecution.builder().request(toolExecReq).build();
                                beforeToolExecutionHandler.accept(beforeToolExecution);
                            } catch (Exception e) {
                                log.error("Error in beforeToolExecutionHandler callback: {}", e.getMessage());
                            }
                        }

                        log.info("[LLMHandler] Executing tool: {} ", toolExecReq.name());
                        String result;
                        try {
                            //update-begin---author:wangshuai---date:2026-03-17---for:【QQYUN-14935】构建skills插件---
                            InvocationContext ctx = InvocationContext.builder()
                                    .chatMemoryId(chatMemory.id())
                                    .timestampNow()
                                    .build();
                            result = executor.executeWithContext(toolExecReq, ctx).resultText();
                            //update-end---author:wangshuai---date:2026-03-17---for:【QQYUN-14935】构建skills插件---
                            //update-begin---wangshuai---date:20260413  for：[issue/1560]/[issues/9527]AI应用调用千问qwen-plus 大模型 提示messages and prompt must not all null #18-----------
                            // 防止工具返回 null 或空串导致部分厂商（如 DashScope）API 报错
                            if (result == null || result.isEmpty()) {
                                result = "(empty)";
                            //update-end---wangshuai---date:20260413  for：[issue/1560]/[issues/9527]AI应用调用千问qwen-plus 大模型 提示messages and prompt must not all null #18-----------
                            }
                        } catch (Exception e) {
                            log.error("Tool execution failed: {}", e.getMessage(), e);
                            result = "Tool execution failed: " + e.getMessage();
                        }

                        if (onToolExecuted != null) {
                            try {
                                onToolExecuted.accept(ToolExecution.builder()
                                        .request(toolExecReq)
                                        .result(ToolExecutionResult.builder().resultText(result).build())
                                        .build());
                            } catch (Exception e) {
                                log.error("Error in onToolExecuted callback: {}", e.getMessage());
                            }
                        }

                        ToolExecutionResultMessage resultMsg = ToolExecutionResultMessage.from(toolExecReq, result);
                        chatMemory.add(resultMsg);
                    }
                    doChat();
                } else {
                    if (onCompleteResponse != null) {
                        onCompleteResponse.accept(completeResponse);
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                if (onError != null) {
                    onError.accept(error);
                }
            }
        });
    }

    private List<ChatMessage> sanitizeForRequest(List<ChatMessage> source) {
        return reinjectUserMessageIfEvicted(source, currentTurnUserMessage);
    }

    /**
     * 发请求前兜底：若 ChatMemory 的淘汰策略在工具调用多轮交互中删掉了当前轮的 UserMessage，
     * 把调用方提供的 UserMessage 快照回填到 SystemMessage 之后、第一条 Ai/Tool 消息之前。
     * 不改动 chatMemory 本身，避免 Qwen/OpenAI/Claude 校验失败并保留用户原始意图。
     */
    public static List<ChatMessage> reinjectUserMessageIfEvicted(List<ChatMessage> source, UserMessage snapshot) {
        if (snapshot == null || source == null) {
            return source;
        }
        for (ChatMessage m : source) {
            if (m instanceof UserMessage) {
                return source;
            }
        }
        int insertAt = 0;
        while (insertAt < source.size() && source.get(insertAt) instanceof SystemMessage) {
            insertAt++;
        }
        List<ChatMessage> result = new ArrayList<>(source.size() + 1);
        result.addAll(source.subList(0, insertAt));
        result.add(snapshot);
        result.addAll(source.subList(insertAt, source.size()));
        return result;
    }

}
