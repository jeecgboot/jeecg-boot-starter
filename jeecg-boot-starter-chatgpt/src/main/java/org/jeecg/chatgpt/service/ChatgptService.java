package org.jeecg.chatgpt.service;

import java.util.List;

import org.jeecg.chatgpt.dto.ChatRequest;
import org.jeecg.chatgpt.dto.ChatResponse;
import org.jeecg.chatgpt.dto.chat.MultiChatMessage;
import org.jeecg.chatgpt.dto.chat.MultiChatRequest;
import org.jeecg.chatgpt.dto.chat.MultiChatResponse;
import org.jeecg.chatgpt.dto.image.ImageFormat;
import org.jeecg.chatgpt.dto.image.ImageRequest;
import org.jeecg.chatgpt.dto.image.ImageResponse;
import org.jeecg.chatgpt.dto.image.ImageSize;

public interface ChatgptService {

    String sendMessage(String message);

    ChatResponse sendChatRequest(ChatRequest request);

    String multiChat(List<MultiChatMessage> messages);

    MultiChatResponse multiChatRequest(MultiChatRequest multiChatRequest);

    String imageGenerate(String prompt);

    List<String> imageGenerate(String prompt, Integer n, ImageSize size, ImageFormat format);

    ImageResponse imageGenerateRequest(ImageRequest imageRequest);
    
}
