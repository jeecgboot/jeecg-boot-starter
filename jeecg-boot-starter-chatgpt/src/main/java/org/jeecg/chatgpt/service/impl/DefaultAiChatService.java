package org.jeecg.chatgpt.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.chatgpt.dto.chat.MultiChatMessage;
import org.jeecg.chatgpt.dto.image.ImageFormat;
import org.jeecg.chatgpt.dto.image.ImageSize;
import org.jeecg.chatgpt.service.AiChatService;

import java.util.List;

/**
 * @Description: 默认的AI聊天工具
 * @Author: chenrui
 * @Date: 2024/1/12 15:52
 */
@Slf4j
public class DefaultAiChatService implements AiChatService {

    /**
     * 无配置提示
     */
    private static final String NO_PROP_WARN_MSG = "If you want to use AI to chat, set up the response configuration first!";

    public DefaultAiChatService() {
    }

    @Override
    public String completions(String message) {
        return generalReturn();
    }

    @Override
    public String multiCompletions(List<MultiChatMessage> messages) {
        return generalReturn();
    }

    @Override
    public String genSchemaModules(String prompt) {
        return generalReturn();
    }

    @Override
    public String genArticleWithMd(String prompt) {
        return generalReturn();
    }

    @Override
    public String imageGenerate(String prompt) {
        return generalReturn();
    }

    @Override
    public List<String> imageGenerate(String prompt, Integer n, ImageSize size, ImageFormat format) {
        return generalReturn();
    }

    private <T> T generalReturn(){
        log.warn(NO_PROP_WARN_MSG);
        return null;
    }
}
