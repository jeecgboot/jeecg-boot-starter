package org.jeecg.ai.handler;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jeecg.ai.assistant.AiChatAssistant;
import org.jeecg.ai.assistant.AiStreamChatAssistant;
import org.jeecg.ai.factory.AiModelFactory;
import org.jeecg.ai.factory.AiModelOptions;
import org.jeecg.ai.prop.AiChatProperties;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 大模型聊天工具类
 *
 * @Author: chenrui
 * @Date: 2025/2/18 14:31
 */
@Slf4j
public class LLMHandler {


    private AiChatProperties aiChatProperties;


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
        if (null == params || StringUtils.isEmpty(params.getApiKey())) {
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
        ChatLanguageModel chatModel = AiModelFactory.createChatModel(modelOp);
        AiServices<AiChatAssistant> chatAssistantBuilder = AiServices.builder(AiChatAssistant.class);
        chatAssistantBuilder.chatLanguageModel(chatModel);

        // RAG
        if (null != params.getQueryRouter()) {
            chatAssistantBuilder.retrievalAugmentor(DefaultRetrievalAugmentor.builder().queryRouter(params.getQueryRouter()).build());
        }

        // 整理消息
        CollateMsgResp chatMessage = collateMessage(messages, params);
        // 整理消息
        if (null != chatMessage.chatMemory) {
            chatAssistantBuilder.chatMemory(chatMessage.chatMemory);
        }
        AiChatAssistant chatAssistant = chatAssistantBuilder.build();
        log.info("[LLMHandler] send message to AI server. message: {}", chatMessage);
        String resp = "";
        if (StringUtils.isNotEmpty(chatMessage.systemMessage)) {
            resp = chatAssistant.chat(chatMessage.systemMessage, chatMessage.prompt);
        } else {
            resp = chatAssistant.chat(chatMessage.prompt);
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
        StreamingChatLanguageModel streamingChatModel = AiModelFactory.createStreamingChatModel(modelOp);
        AiServices<AiStreamChatAssistant> chatAssistantBuilder = AiServices.builder(AiStreamChatAssistant.class);
        chatAssistantBuilder.streamingChatLanguageModel(streamingChatModel);

        // RAG
        if (null != params.getQueryRouter()) {
            chatAssistantBuilder.retrievalAugmentor(DefaultRetrievalAugmentor.builder().queryRouter(params.getQueryRouter()).build());
        }

        // 整理消息
        CollateMsgResp chatMessage = collateMessage(messages, params);
        // 整理消息
        if (null != chatMessage.chatMemory) {
            chatAssistantBuilder.chatMemory(chatMessage.chatMemory);
        }

        AiStreamChatAssistant chatAssistant = chatAssistantBuilder.build();
        log.info("[LLMHandler] send message to AI server. message: {}", chatMessage);
        if (null != chatMessage.systemMessage && !chatMessage.systemMessage.isEmpty()) {
            return chatAssistant.chat(chatMessage.systemMessage, chatMessage.prompt);
        } else {
            return chatAssistant.chat(chatMessage.prompt);
        }
    }

    /**
     * 整理消息
     *
     * @param messages
     * @param params
     * @return
     * @author chenrui
     * @date 2025/2/14 15:02
     */
    @NotNull
    private CollateMsgResp collateMessage(List<ChatMessage> messages, AIParams params) {
        if (null == params) {
            params = new AIParams();
        }
        String systemMessage = "";
        String prompt = "";
        ChatMemory chatMemory = null;
        if (null != messages && !messages.isEmpty()) {
            List<ChatMessage> messagesCopy = new ArrayList<>(messages); // <--- 新增拷贝操作

            // 系统消息
            systemMessage = messagesCopy.stream()
                    .filter(chatMessage -> ChatMessageType.SYSTEM.equals(chatMessage.type()))
                    .map(chatMessage -> ((SystemMessage) chatMessage).text())
                    .collect(Collectors.joining("\n"));
            // 用户消息
            for (int i = messagesCopy.size() - 1; i >= 0; i--) {
                ChatMessage tempMsg = messagesCopy.get(i);
                if (ChatMessageType.USER.equals(tempMsg.type())) {
                    prompt = ((UserMessage) tempMsg).singleText();
                    messagesCopy.remove(i);
                    break;
                }
            }
            if (StringUtils.isNotEmpty(params.getKnowledgeTxt())) {
                prompt = String.format("%s\n\n用以下信息回答问题:\n%s\n\n", prompt, params.getKnowledgeTxt());
            }
            prompt = prompt.replaceAll("\\{\\{(.*?)}}", "$1");
            // 历史消息
            // 最大消息数: 系统消息和用户消息也会计数,所以最大消息增加两个
            int maxMsgNumber = 4 + 2;
            if (null != params.getMaxMsgNumber()) {
                maxMsgNumber = params.getMaxMsgNumber() + 2;
            }
            chatMemory = MessageWindowChatMemory.builder().maxMessages(maxMsgNumber).build();
            for (ChatMessage chatMessage : messagesCopy) {
                if (null == chatMessage || chatMessage.type().equals(ChatMessageType.SYSTEM)) {
                    continue;
                }
                chatMemory.add(chatMessage);
            }
        }
        return new CollateMsgResp(systemMessage, prompt, chatMemory);
    }

    /**
     * 整理后的消息
     *
     * @author chenrui
     * @date 2025/2/14 16:34
     */
    private static class CollateMsgResp {
        public final String systemMessage;
        public final String prompt;
        public final ChatMemory chatMemory;

        public CollateMsgResp(String systemMessage, String prompt, ChatMemory chatMemory) {
            this.systemMessage = systemMessage;
            this.prompt = prompt;
            this.chatMemory = chatMemory;
        }

        @Override
        public String toString() {
            // 返回完整的消息内容
            return "{" +
                    "systemMessage='" + systemMessage + '\'' +
                    ", prompt='" + prompt + '\'' +
                    ", chatMemory=" + (chatMemory != null ? chatMemory.messages() : "null") +
                    '}';
        }
    }

}
