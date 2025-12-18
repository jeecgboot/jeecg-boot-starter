package org.jeecg.ai.handler;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.exception.ToolExecutionException;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.output.ServiceOutputParser;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jeecg.ai.factory.AiModelFactory;
import org.jeecg.ai.factory.AiModelOptions;
import org.jeecg.ai.prop.AiChatProperties;
import org.jeecg.ai.stream.InternalTokenStream;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 大模型聊天工具类
 *
 * @Author: chenrui
 * @Date: 2025/2/18 14:31
 */
@Slf4j
public class LLMHandler {


    private AiChatProperties aiChatProperties;

    private final ServiceOutputParser serviceOutputParser = new ServiceOutputParser();


    public LLMHandler(AiChatProperties aiChatProperties) {
        this.aiChatProperties = aiChatProperties;
    }

    public LLMHandler() {
    }


    /**
     * 确保aiParams
     *
     * @param params
     * @return
     * @author chenrui
     * @date 2025/3/12 15:22
     */
    private AIParams ensureParams(AIParams params) {
        if (null == params || StringUtils.isEmpty(params.getBaseUrl())) {
            params = getDefaultModel(params);
        }
        if (null == params) {
            throw new IllegalArgumentException("大语言模型参数为空");
        }
        return params;
    }

    /**
     * 获取默认大模型数据
     *
     * @return
     * @author chenrui
     * @date 2025/2/25 19:26
     */
    private AIParams getDefaultModel(AIParams params) {
        if (null == aiChatProperties) {
            log.warn("未配置默认大预言模型");
            return null;
        }
        if (params == null) {
            params = new AIParams();
        }
        params.setProvider(aiChatProperties.getProvider());
        params.setModelName(aiChatProperties.getModel());
        params.setBaseUrl(aiChatProperties.getApiHost());
        params.setApiKey(aiChatProperties.getApiKey());
        params.setSecretKey(aiChatProperties.getCredential().getSecretKey());
        params.setTimeout(aiChatProperties.getTimeout());
        return params;
    }

    public String completions(String message) {
        return completions(Collections.singletonList(UserMessage.from(message)), null);
    }


    /**
     * 问答
     *
     * @param messages
     * @param params
     * @return
     * @author chenrui
     * @date 2025/2/24 17:30
     */
    public String completions(List<ChatMessage> messages, AIParams params) {
        params = ensureParams(params);

        AiModelOptions modelOp = params.toModelOptions();
        ChatModel chatModel = AiModelFactory.createChatModel(modelOp);

        // 整理消息
        CollateMsgResp chatMessage = collateMessage(messages, params);

        // 工具定义
        List<ToolSpecification> toolSpecifications = new ArrayList<>();
        // 工具执行器
        Map<String, ToolExecutor> toolExecutors = new HashMap<>();
        if(null != params.getTools() && !params.getTools().isEmpty()) {
            toolSpecifications = new ArrayList<>(params.getTools().keySet());
            params.getTools().forEach((tool,executor) -> {
                toolExecutors.put(tool.name(),executor);
            });
        }

        // MCP工具解析
        fillMcpTools(params, chatMessage, toolSpecifications, toolExecutors);

        String resp = "";
        log.info("[LLMHandler] send message to AI server. message: {}", chatMessage);
        while (true) {
            ChatRequest.Builder requestBuilder = ChatRequest.builder()
                    .messages(chatMessage.chatMemory.messages());

            // 判断模型是否支持工具调用
            if(isSupportTools(chatModel.defaultRequestParameters())) {
                requestBuilder = requestBuilder.toolSpecifications(toolSpecifications);
            }
            //update-begin---author:wangshuai---date:2025-12-18---for: 工具调用失败后打印日志，不影响整个流程---
            ChatResponse response = null;
            try {
                response = chatModel.chat(requestBuilder.build());
            } catch (ToolExecutionException e) {
                log.error("工具调用失败：{}", e.getMessage(), e);
                break;
            }
            //update-end---author:wangshuai---date:2025-12-18---for: 工具调用失败后打印日志，不影响整个流程---
            AiMessage aiMessage = response.aiMessage();
            chatMessage.chatMemory.add(aiMessage);

            // 没有工具调用，则解析文本并结束
            if (aiMessage.toolExecutionRequests() == null || aiMessage.toolExecutionRequests().isEmpty()) {
                resp = (String) serviceOutputParser.parse(response, String.class);
                break;
            }

            // 有工具调用：逐个执行并将结果以 ToolExecutionResultMessage 追加到历史
            for (ToolExecutionRequest toolExecReq : aiMessage.toolExecutionRequests()) {
                ToolExecutor executor = toolExecutors.get(toolExecReq.name());
                if (executor == null) {
                    throw new IllegalStateException("未找到工具执行器: " + toolExecReq.name());
                }
                log.info("[LLMHandler] Executing tool: {} ", toolExecReq.name());
                try{
                    String result = executor.execute(toolExecReq, chatMessage.chatMemory.id());
                    ToolExecutionResultMessage resultMsg = ToolExecutionResultMessage.from(toolExecReq, result);
                    chatMessage.chatMemory.add(resultMsg);
                }catch (ToolExecutionException e){
                    log.info("插件运行失败，原因：{}",e.getMessage(),e);
                }
   
            }
        }


        log.info("[LLMHandler] Received the AI's response . message: {}", resp);
        return resp;
    }

    /**
     * 模型是否支持工具调用
     * @param parameters
     * @return
     * @author chenrui
     * @date 2025/10/14 16:27
     */
    private boolean isSupportTools(ChatRequestParameters parameters) {
        String modelName = parameters.modelName();
        boolean isMultimodalModel = modelName.contains("-vl-") || modelName.contains("-audio-") || modelName.contains("-omni-");
        // 多模态模型不支持工具调用
        return !isMultimodalModel;
    }

    /**
     * 聊天(流式)
     *
     * @param messages
     * @param params
     * @return
     * @author chenrui
     * @date 2025/2/24 17:29
     */
    public TokenStream chat(List<ChatMessage> messages, AIParams params) {
        params = ensureParams(params);
        if (null == params) {
            throw new IllegalArgumentException("大语言模型参数为空");
        }

        // model
        AiModelOptions modelOp = params.toModelOptions();
        StreamingChatModel streamingChatModel = AiModelFactory.createStreamingChatModel(modelOp);

        // 工具定义
        List<ToolSpecification> toolSpecifications = new ArrayList<>();
        // 工具执行器
        Map<String, ToolExecutor> toolExecutors = new HashMap<>();
        if(null != params.getTools() && !params.getTools().isEmpty()) {
            toolSpecifications = new ArrayList<>(params.getTools().keySet());
            params.getTools().forEach((tool,executor) -> {
                toolExecutors.put(tool.name(),executor);
            });
        }

        CollateMsgResp chatMessage = collateMessage(messages, params);

        // MCP工具解析
        fillMcpTools(params, chatMessage, toolSpecifications, toolExecutors);

        //update-begin---author:wangshuai---date:2025-12-18---for:【QQYUN-14048】【AI】langchain4j 升级到1.9.1---
        return new InternalTokenStream(
                streamingChatModel,
                toolSpecifications,
                toolExecutors,
                chatMessage.chatMemory,
                chatMessage.augmentationResult != null ? chatMessage.augmentationResult.contents() : null
        );
        //update-end---author:wangshuai---date:2025-12-18---for:【QQYUN-14048】【AI】langchain4j 升级到1.9.1---
    }


    /**
     * 将 MCP ToolProviders 解析并填充到工具规格与执行器集合中
     * @param params AI参数
     * @param chatMessage 整理后的消息对象
     * @param toolSpecifications 现有工具规格集合(将追加)
     * @param toolExecutors 现有工具执行器集合(将追加)
     *
     * @param params
     * @param chatMessage
     * @param toolSpecifications
     * @param toolExecutors
     * @author chenrui
     * @date 2025/10/21 17:34
     */
    private void fillMcpTools(AIParams params,
                              CollateMsgResp chatMessage,
                              List<ToolSpecification> toolSpecifications,
                              Map<String, ToolExecutor> toolExecutors) {
        if (params.getMcpToolProviders() == null || params.getMcpToolProviders().isEmpty()) {
            return;
        }
        for (McpToolProvider toolProvider : params.getMcpToolProviders()) {
            //update-begin---author:wangshuai---date:2025-12-18---for:【QQYUN-14048】【AI】langchain4j 升级到1.9.1---
            ToolProviderRequest request = new ToolProviderRequest(chatMessage.chatMemory.id(),chatMessage.userMessage);
            ToolProviderResult result = toolProvider.provideTools(request);
            if (result != null && result.tools() != null) {
                for (Map.Entry<ToolSpecification, ToolExecutor> entry : result.tools().entrySet()) {
                    toolSpecifications.add(entry.getKey());
                    toolExecutors.put(entry.getKey().name(), entry.getValue());
            //update-end---author:wangshuai---date:2025-12-18---for:【QQYUN-14048】【AI】langchain4j 升级到1.9.1---
                }
            }
        }
    }

    /**
     * 整理消息
     *
     * @param messages
     * @param params
     * @return
     * @author chenrui
     * @date 2025/3/18 16:52
     */
    private CollateMsgResp collateMessage(List<ChatMessage> messages, AIParams params) {
        if (null == params) {
            params = new AIParams();
        }

        // 获取用户消息
        List<ChatMessage> messagesCopy = new ArrayList<>(messages);
        UserMessage userMessage = null;
        ChatMessage lastMessage = messagesCopy.get(messagesCopy.size() - 1);
        if (lastMessage.type().equals(ChatMessageType.USER)) {
            userMessage = (UserMessage) messagesCopy.remove(messagesCopy.size() - 1);
        } else {
            throw new IllegalArgumentException("最后一条消息必须是用户消息");
        }

        // 最大消息数: 系统消息和用户消息也会计数,所以最大消息增加两个
        int maxMsgNumber = 4 + 2;
        if (null != params.getMaxMsgNumber()) {
            maxMsgNumber = params.getMaxMsgNumber() + 2;
        }


        // 系统消息
        AtomicReference<SystemMessage> systemMessageAto = new AtomicReference<>();
        messagesCopy.removeIf(tempMsg -> {
            if (ChatMessageType.SYSTEM.equals(tempMsg.type())) {
                if (systemMessageAto.get() == null) {
                    systemMessageAto.set((SystemMessage) tempMsg);
                } else {
                    SystemMessage systemMessage = systemMessageAto.get();
                    String text = systemMessage.text() + "\n" + ((SystemMessage) tempMsg).text();
                    systemMessageAto.set(SystemMessage.from(text));
                }
                return true;
            }
            return false;
        });

        // 消息缓存
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(maxMsgNumber).build();

        // 添加系统消息到
        if (null != systemMessageAto.get()) {
            chatMemory.add(systemMessageAto.get());
        }
        // 添加历史消息
        messagesCopy.forEach(chatMemory::add);


        // RAG
        AugmentationResult augmentationResult = null;
        if (null != params.getQueryRouter()) {
            DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder().queryRouter(params.getQueryRouter()).build();
            if (retrievalAugmentor != null) {
                StringBuilder userQuestion = new StringBuilder();
                List<dev.langchain4j.data.message.Content> contents = new ArrayList<>(userMessage.contents());
                for (int i = contents.size() - 1; i >= 0; i--) {
                    if (contents.get(i) instanceof TextContent) {
                        userQuestion.append(((TextContent) contents.remove(i)).text());
                        userQuestion.append("\n");
                    }
                }
                UserMessage textUserMessage = UserMessage.from(userQuestion.toString());
                Metadata metadata = Metadata.from(textUserMessage, "default", chatMemory.messages());
                AugmentationRequest augmentationRequest = new AugmentationRequest(textUserMessage, metadata);
                augmentationResult = retrievalAugmentor.augment(augmentationRequest);
                textUserMessage = (UserMessage) augmentationResult.chatMessage();
                contents.add(dev.langchain4j.data.message.TextContent.from(textUserMessage.singleText()));
                userMessage = UserMessage.from(contents);
            }
        }
        // 用户消息
        chatMemory.add(userMessage);
        return new CollateMsgResp(chatMemory, augmentationResult, userMessage);
    }

    /**
     * 消息整理返回值
     * @author chenrui
     * @date 2025/3/18 16:53
     */
    private static class CollateMsgResp {
        public final ChatMemory chatMemory;
        public final AugmentationResult augmentationResult;
        public final UserMessage userMessage;

        public CollateMsgResp(ChatMemory chatMemory, AugmentationResult augmentationResult, UserMessage userMessage) {
            this.chatMemory = chatMemory;
            this.augmentationResult = augmentationResult;
            this.userMessage = userMessage;
        }

        @Override
        public String toString() {
            // 返回完整的消息内容
            return "{messages=" + (chatMemory != null ? chatMemory.messages() : "null") + "}";
        }
    }

}
