package org.jeecg.chatgpt.service;

import org.jeecg.chatgpt.dto.chat.MultiChatMessage;
import org.jeecg.chatgpt.dto.image.ImageFormat;
import org.jeecg.chatgpt.dto.image.ImageSize;

import java.util.List;

/**
 * @Description: AI聊天工具客户端
 * @Author: chenrui
 * @Date: 2024/1/12 15:47
 */
public interface AiChatService {

    /**
     * 问答
     *
     * @param message 问题
     * @return String 回答
     * @author chenrui
     * @date 2024/1/12 16:01
     */
    String completions(String message);

    /**
     * 多角色问答
     *
     * @param messages 问题
     * @return String 回答
     * @author chenrui
     * @date 2024/1/12 18:07
     */
    String multiCompletions(List<MultiChatMessage> messages);

    /**
     * 通过AI生成模块表设计
     *
     * @param prompt 提示
     * @return String 表设计
     * @author chenrui
     * @date 2024/1/9 20:12
     */
    String genSchemaModules(String prompt);

    /**
     * 通过AI生成软文-markdown格式
     *
     * @param prompt 提示
     * @return String 软文内容
     * @author chenrui
     * @date 2024/1/9 20:12
     */
    String genArticleWithMd(String prompt);

    /**
     * 图片生成
     *
     * @param prompt 提示
     * @return image url
     * @author chenrui
     * @date 2024/1/12 20:14
     */
    String imageGenerate(String prompt);

    /**
     * 图片生成
     *
     * @param prompt 提示 required
     * @param n      生成数量 1-10 default 1
     * @param size   图片大小 {@link org.jeecg.chatgpt.dto.image.ImageSize}
     * @param format 返回图片格式化类型 {@link org.jeecg.chatgpt.dto.image.ImageFormat}
     * @return List url or base64
     * @author chenrui
     * @date 2024/1/12 20:14
     */
    List<String> imageGenerate(String prompt, Integer n, ImageSize size, ImageFormat format);

}
