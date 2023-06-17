package org.jeecg.chatgpt.autoconfig;

import org.jeecg.chatgpt.property.ChatgptProperties;
import org.jeecg.chatgpt.service.ChatgptService;
import org.jeecg.chatgpt.service.impl.DefaultChatgptService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @author liuliu
 *
 */
@Configuration
@EnableConfigurationProperties(ChatgptProperties.class)
public class ChatgptAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatgptService.class)
    public ChatgptService chatgptService(ChatgptProperties chatgptProperties) {
        return new DefaultChatgptService(chatgptProperties);
    }

}
