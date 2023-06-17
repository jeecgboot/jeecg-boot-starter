package org.jeecg.chatgpt.dto;

import java.util.Map;

/**
 * 
 * @author liuliu
 *
 */
public class ChatRequest {

    private String model;

    private String prompt;

    private Integer max_tokens;

    private Double temperature;

    private Double top_p;

    private String suffix;

    private Integer n;

    private Boolean stream;

    private Integer logprobs;

    private Boolean echo;

    private String stop;

    private Double presence_penalty;

    private Integer best_of;

    private Map<String, Integer> logit_bias;

    private String user;

    public ChatRequest(String model, String prompt, Integer maxTokens, Double temperature, Double topP) {
        this.model = model;
        this.prompt = prompt;
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

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
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

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
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

    public Integer getLogprobs() {
        return logprobs;
    }

    public void setLogprobs(Integer logprobs) {
        this.logprobs = logprobs;
    }

    public Boolean getEcho() {
        return echo;
    }

    public void setEcho(Boolean echo) {
        this.echo = echo;
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

    public Integer getBest_of() {
        return best_of;
    }

    public void setBest_of(Integer best_of) {
        this.best_of = best_of;
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
        return "ChatRequest [model=" + model + ", prompt=" + prompt + ", max_tokens=" + max_tokens + ", temperature="
                + temperature + ", top_p=" + top_p + ", suffix=" + suffix + ", n=" + n + ", stream=" + stream
                + ", logprobs=" + logprobs + ", echo=" + echo + ", stop=" + stop + ", presence_penalty="
                + presence_penalty + ", best_of=" + best_of + ", logit_bias=" + logit_bias + ", user=" + user + "]";
    }

}
