package org.jeecg.chatgpt.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jeecg.ai.handler.LLMHandler;
import org.jeecg.chatgpt.dto.chat.MultiChatMessage;
import org.jeecg.chatgpt.dto.image.ImageFormat;
import org.jeecg.chatgpt.dto.image.ImageSize;
import org.jeecg.chatgpt.service.AiChatService;

import java.util.ArrayList;
import java.util.Arrays;
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


    LLMHandler llmHandler;


    public ChatGptService(LLMHandler llmHandler) {
        this.llmHandler = llmHandler;
    }


    //*****************************************chat begin*********************************************/

    @Override
    public String completions(String message) {
        if (StringUtils.isEmpty(message)) {
            return "";
        }
        return llmHandler.completions(message);
    }

    @Override
    public String multiCompletions(List<MultiChatMessage> messages) {
        if (null == messages || messages.isEmpty()) {
            return "";
        }
        List<ChatMessage> chatMessages = messages.stream()
                .map(m ->{
                    if(MultiChatMessage.Role.SYSTEM.getName().equalsIgnoreCase(m.getRole())){
                        return new SystemMessage(m.getContent());
                    }else if(MultiChatMessage.Role.ASSISTANT.getName().equalsIgnoreCase(m.getRole())){
                        return new AiMessage(m.getContent());
                    }else{
                        return new UserMessage(m.getContent());
                    }
                })
                .collect(Collectors.toList());
        return llmHandler.completions(chatMessages, null);
    }

    @Override
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
        if (gptResp.contains("</think>")) {
            String[] thinkSplit = gptResp.split("</think>");
            gptResp = thinkSplit[thinkSplit.length - 1];
        }
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
    @Override
    public String genArticleWithMd(String prompt) {
        if (StringUtils.isEmpty(prompt)) {
            return "";
        }
        List<MultiChatMessage> messages = new ArrayList<>();
        messages.add(MultiChatMessage.builder().role(MultiChatMessage.Role.SYSTEM).content("根据文章内容描述用MarkDown写一篇软文；只输出MarkDown,不要其他的描述。").build());
        messages.add(MultiChatMessage.builder().role(MultiChatMessage.Role.USER).content("文章内容描述如下:" + prompt).build());
        String gptResp = multiCompletions(messages);
        if (gptResp.contains("</think>")) {
            String[] thinkSplit = gptResp.split("</think>");
            gptResp = thinkSplit[thinkSplit.length - 1];
        }
        return gptResp;
    }


    //*****************************************chat end*********************************************/

    
    //*****************************************generate begin*********************************************/

    @Override
    public String imageGenerate(String prompt) {
        // TODO author: chenrui for:图像生成 date:2025/3/11
        log.warn("暂不支持图像生成");
        return null;
    }

    @Override
    public List<String> imageGenerate(String prompt, Integer n, ImageSize size, ImageFormat format) {
        // TODO author: chenrui for:图像生成 date:2025/3/11
        log.warn("暂不支持图像生成");
        return null;
    }

    //*****************************************generate end*********************************************/


}
