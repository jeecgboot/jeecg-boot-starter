package org.jeecg.chatgpt.dto.chat;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author liuliu
 *
 */
public class MultiChatRequest {
    private String model;

    private List<MultiChatMessage> messages;

    private Integer max_tokens;

    private Double temperature;

    private Double top_p;

    private Integer n;

    private Boolean stream;

    private String stop;
    
    private Double presence_penalty;

    private Double frequency_penalty;

    private Map<String, Integer> logit_bias;

    private String user;

    public MultiChatRequest(String model, List<MultiChatMessage> messages, Integer maxTokens, Double temperature,
            Double topP) {
        this.model = model;
        this.messages = messages;
        this.max_tokens = maxTokens;
        this.temperature = temperature;
        this.top_p = topP;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<MultiChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<MultiChatMessage> messages) {
        this.messages = messages;
    }

    public Integer getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(Integer max_tokens) {
        this.max_tokens = max_tokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTop_p() {
        return top_p;
    }

    public void setTop_p(Double top_p) {
        this.top_p = top_p;
    }

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public String getStop() {
        return stop;
    }

    public void setStop(String stop) {
        this.stop = stop;
    }

    public Double getPresence_penalty() {
        return presence_penalty;
    }

    public void setPresence_penalty(Double presence_penalty) {
        this.presence_penalty = presence_penalty;
    }

    public Double getFrequency_penalty() {
        return frequency_penalty;
    }

    public void setFrequency_penalty(Double frequency_penalty) {
        this.frequency_penalty = frequency_penalty;
    }

    public Map<String, Integer> getLogit_bias() {
        return logit_bias;
    }

    public void setLogit_bias(Map<String, Integer> logit_bias) {
        this.logit_bias = logit_bias;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "MultiChatRequest [model=" + model + ", messages=" + messages + ", max_tokens=" + max_tokens
                + ", temperature=" + temperature + ", top_p=" + top_p + ", n=" + n + ", stream=" + stream + ", stop="
                + stop + ", presence_penalty=" + presence_penalty + ", frequency_penalty=" + frequency_penalty
                + ", logit_bias=" + logit_bias + ", user=" + user + "]";
    }
    
    

}
