package org.jeecg.ai.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI聊天助理
 *
 * @Author: chenrui
 * @Date: 2025/2/13 20:13
 */
public interface AiStreamChatAssistant {

    TokenStream chat(@UserMessage String prompt);

    @SystemMessage("{{systemMessage}}")
    TokenStream chat(@V("systemMessage") String systemMessage, @UserMessage String prompt);

}
