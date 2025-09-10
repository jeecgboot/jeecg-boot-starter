package org.jeecg.ai.handler;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServiceTokenStream;
import dev.langchain4j.service.AiServiceTokenStreamParameters;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.output.ServiceOutputParser;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jeecg.ai.assistant.AiStreamChatAssistant;
import org.jeecg.ai.factory.AiModelFactory;
import org.jeecg.ai.factory.AiModelOptions;
import org.jeecg.ai.prop.AiChatProperties;

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

        String resp = "";
        log.info("[LLMHandler] send message to AI server. message: {}", chatMessage);
        while (true) {
            ChatRequest chatRequest = ChatRequest.builder().messages(chatMessage.chatMemory.messages())
                    .toolSpecifications(toolSpecifications)
                    .build();

            ChatResponse response = chatModel.chat(chatRequest);

            AiMessage aiMessage = response.aiMessage();
            chatMessage.chatMemory.add(aiMessage);

            // 没有工具调用，则解析文本并结束
            if (aiMessage.toolExecutionRequests() == null || aiMessage.toolExecutionRequests().isEmpty()) {
                resp = (String) serviceOutputParser.parse(new Response<>(response.aiMessage()), String.class);
                break;
            }

            // 有工具调用：逐个执行并将结果以 ToolExecutionResultMessage 追加到历史
            for (ToolExecutionRequest toolExecReq : aiMessage.toolExecutionRequests()) {
                ToolExecutor executor = toolExecutors.get(toolExecReq.name());
                if (executor == null) {
                    throw new IllegalStateException("未找到工具执行器: " + toolExecReq.name());
                }
                log.info("[LLMHandler] Executing tool: {} ", toolExecReq.name());
                String result = executor.execute(toolExecReq, chatMessage.chatMemory.id());
                ToolExecutionResultMessage resultMsg = ToolExecutionResultMessage.from(toolExecReq, result);
                chatMessage.chatMemory.add(resultMsg);
            }
        }
        log.info("[LLMHandler] Received the AI's response . message: {}", resp);
        return resp;
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
        AiServiceContext context = new AiServiceContext(AiStreamChatAssistant.class);
        context.streamingChatModel = streamingChatModel;
        log.info("[LLMHandler] send message to AI server. message: {}", chatMessage);
        return new AiServiceTokenStream(
                chatMessage.chatMemory.messages(),
                toolSpecifications,
                toolExecutors,
                chatMessage.augmentationResult != null ? chatMessage.augmentationResult.contents() : null,
                context,
                "default"
        );
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
                List<Content> contents = new ArrayList<>(userMessage.contents());
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
                contents.add(TextContent.from(textUserMessage.singleText()));
                userMessage = UserMessage.from(contents);
            }
        }
        // 用户消息
        chatMemory.add(userMessage);
        return new CollateMsgResp(chatMemory, augmentationResult);
    }

    /**
     * 消息整理返回值
     * @author chenrui
     * @date 2025/3/18 16:53
     */
    private static class CollateMsgResp {
        public final ChatMemory chatMemory;
        public final AugmentationResult augmentationResult;

        public CollateMsgResp(ChatMemory chatMemory, AugmentationResult augmentationResult) {
            this.chatMemory = chatMemory;
            this.augmentationResult = augmentationResult;
        }

        @Override
        public String toString() {
            // 返回完整的消息内容
            return "{messages=" + (chatMemory != null ? chatMemory.messages() : "null") + "}";
        }
    }

}
