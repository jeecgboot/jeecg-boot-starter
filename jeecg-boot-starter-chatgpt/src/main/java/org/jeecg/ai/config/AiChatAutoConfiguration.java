package org.jeecg.ai.config;

import org.jeecg.ai.handler.LLMHandler;
import org.jeecg.ai.prop.AiChatProperties;
import org.jeecg.chatgpt.service.AiChatService;
import org.jeecg.chatgpt.service.impl.ChatGptService;
import org.jeecg.chatgpt.service.impl.DefaultAiChatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI聊天工具自动装配
 *
 * @author chenrui
 * @date 2024/1/12 15:45
 */
@Configuration
@EnableConfigurationProperties(AiChatProperties.class)
public class AiChatAutoConfiguration {


    /**
     * llm工具栏
     *
     * @param aiChatProperties
     * @return
     * @author chenrui
     * @date 2025/3/11 14:14
     */
    @Bean
    @ConditionalOnProperty(prefix = AiChatProperties.PREFIX, name = "enabled", havingValue = "true")
    public LLMHandler llmHandler(AiChatProperties aiChatProperties) {
        return new LLMHandler(aiChatProperties);
    }

    /**
     * 默认的llm工具栏
     *
     * @return
     * @author chenrui
     * @date 2025/3/11 14:14
     */
    @Bean
    public LLMHandler defaultLlmHandler() {
        return new LLMHandler();
    }


    /**
     * ChatGpt聊天Service
     *
     * @return
     * @author chenrui
     * @date 2024/1/12 17:09
     */
    @Bean
    @ConditionalOnProperty(prefix = AiChatProperties.PREFIX, name = "enabled", havingValue = "true")
    public AiChatService chatGptAiChatService(LLMHandler llmHandler) {
        return new ChatGptService(llmHandler);
    }

    /**
     * 默认的AI聊天Service
     *
     * @return
     * @author chenrui
     * @date 2024/1/12 17:08
     */
    @Bean
    @ConditionalOnMissingBean(AiChatService.class)
    public AiChatService defaultAiChatService() {
        return new DefaultAiChatService();
    }

}
