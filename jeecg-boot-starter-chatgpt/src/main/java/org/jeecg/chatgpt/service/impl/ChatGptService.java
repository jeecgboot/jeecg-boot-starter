package org.jeecg.chatgpt.service.impl;

import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse;
import com.unfbx.chatgpt.entity.chat.Message;
import com.unfbx.chatgpt.entity.images.Image;
import com.unfbx.chatgpt.entity.images.ImageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jeecg.chatgpt.dto.chat.MultiChatMessage;
import org.jeecg.chatgpt.dto.image.ImageFormat;
import org.jeecg.chatgpt.dto.image.ImageSize;
import org.jeecg.chatgpt.prop.AiChatProperties;
import org.jeecg.chatgpt.service.AiChatService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Description: chatGptService
 * @Author: chenrui
 * @Date: 2024/1/12 15:52
 */
@Slf4j
public class ChatGptService implements AiChatService {


    /**
     * openAiClient
     */
    OpenAiClient client;

    /**
     * 配置文件
     */
    AiChatProperties aiChatProperties;

    private ChatGptService() {
    }

    public ChatGptService(OpenAiClient openAiClient, AiChatProperties aiChatProperties) {
        client = openAiClient;
        this.aiChatProperties = aiChatProperties;
    }


    //*****************************************chat begin*********************************************/

    @Override
    public String completions(String message) {
        if (StringUtils.isEmpty(message)) {
            return "";
        }
        Message userMsg = Message.builder().role(Message.Role.USER).content(message).build();
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .messages(Collections.singletonList(userMsg))
                .model(aiChatProperties.getModel())
                .build();
        ChatCompletionResponse chatCompletionResp = client.chatCompletion(chatCompletion);
        return chatCompletionResp.getChoices().stream().map(chatChoice -> chatChoice.getMessage().getContent()).collect(Collectors.joining());
    }

    @Override
    public String multiCompletions(List<MultiChatMessage> messages) {
        if (null == messages || messages.isEmpty()) {
            return "";
        }
        List<Message> gptMessage = messages.stream()
                .map(m -> Message.builder().role(m.getRole()).content(m.getContent()).name(m.getName()).build())
                .collect(Collectors.toList());
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .messages(gptMessage)
                .model(aiChatProperties.getModel())
                .build();
        ChatCompletionResponse chatCompletionResponse = client.chatCompletion(chatCompletion);
        return chatCompletionResponse.getChoices().stream().map(chatChoice -> chatChoice.getMessage().getContent()).collect(Collectors.joining());
    }

    public String genSchemaModules(String prompt) {
        if (StringUtils.isEmpty(prompt)) {
            return "";
        }
        String sysMsgContent = "根据业务需求设计一套表单；只回答json数据不要有其他描述。" +
                "整体是一个json数组，每个表是一个json对象，属性包含：中文名（comment)，英文名(tableName)，字段列表(fields);" +
                "字段列表是一个json数组，包含字段英文名(field)、字段中文名(comment)、字段数据库类型(fieldDbType)、字段组件(component)。" +
                "可用的组件包含：input、textarea、number、money、radio、checkbox、select、switch、phone、email、file、date、time。" +
                "参考json：[{\\\"tableName\\\":\\\"order\\\",\\\"comment\\\":\\\"订单表\\\",\\\"fields\\\":[{\\\"field\\\":\\\"name\\\",\\\"comment\\\":\\\"姓名\\\",\\\"fieldDbType\\\":\\\"varchar\\\",\\\"component\\\":\\\"input\\\"}]}]。";
        MultiChatMessage sysMsg = MultiChatMessage.builder().role(MultiChatMessage.Role.USER).content(sysMsgContent).build();
        MultiChatMessage userMsg = MultiChatMessage.builder().role(MultiChatMessage.Role.USER).content("业务需求如下:" + prompt).build();
        String gptResp =  multiCompletions(Arrays.asList(sysMsg,userMsg));
        Pattern pattern = Pattern.compile("\\[.*?].*$", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(gptResp);
        String returnData = "";
        if (matcher.find()) {
            returnData = matcher.group(0);
        }
        return returnData;
    }

    /**
     * 通过AI生成软文-markdown格式
     *
     * @param descr 描述
     * @return
     * @author chenrui
     * @date 2024/1/9 20:12
     */
    public String genArticleWithMd(String prompt) {
        if (StringUtils.isEmpty(prompt)) {
            return "";
        }
        List<MultiChatMessage> messages = new ArrayList<>();
        messages.add(MultiChatMessage.builder().role(MultiChatMessage.Role.SYSTEM).content("根据文章内容描述用MarkDown写一篇软文；只输出MarkDown,不要其他的描述。").build());
        messages.add(MultiChatMessage.builder().role(MultiChatMessage.Role.USER).content("文章内容描述如下:" + prompt).build());
        return multiCompletions(messages);
    }


    //*****************************************chat end*********************************************/

    
    //*****************************************generate begin*********************************************/

    @Override
    public String imageGenerate(String prompt) {
        ImageResponse imageResponse = client.genImages(prompt);
        try {
            return imageResponse.getData().get(0).getUrl();
        } catch (Exception e) {
            log.error("parse image url error", e);
            throw e;
        }
    }

    @Override
    public List<String> imageGenerate(String prompt, Integer n, ImageSize size, ImageFormat format) {
        Image image = Image.builder().prompt(prompt).n(n).responseFormat(format.getFormat()).size(size.getSize()).build();
        ImageResponse imageResponse = client.genImages(image);
        try {
            List<String> list = new ArrayList<>();
            imageResponse.getData().forEach(imageData -> {
                if (format.equals(ImageFormat.URL)) {
                    list.add(imageData.getUrl());
                } else {
                    list.add(imageData.getB64Json());
                }
            });
            return list;
        } catch (Exception e) {
            log.error("parse image url error", e);
            throw e;
        }
    }

    //*****************************************generate end*********************************************/


}
