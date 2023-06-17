package org.jeecg.chatgpt.dto;

/**
 * 
 * @author liuliu
 *
 */
public class Choice {
    private String text;
    private Integer index;
    private String logprobs;
    private String finish_reason;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getLogprobs() {
        return logprobs;
    }

    public void setLogprobs(String logprobs) {
        this.logprobs = logprobs;
    }

    public String getFinish_reason() {
        return finish_reason;
    }

    public void setFinish_reason(String finish_reason) {
        this.finish_reason = finish_reason;
    }

    @Override
    public String toString() {
        return "Choice [text=" + text + ", index=" + index + ", logprobs=" + logprobs + ", finish_reason="
                + finish_reason + "]";
    }

}
