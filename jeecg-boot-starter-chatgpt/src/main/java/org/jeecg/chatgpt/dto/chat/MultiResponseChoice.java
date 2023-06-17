package org.jeecg.chatgpt.dto.chat;

/**
 * 
 * @author liuliu
 *
 */
public class MultiResponseChoice {
    private MultiChatMessage message;

    private String finish_reason;

    private Integer index;

    public MultiChatMessage getMessage() {
        return message;
    }

    public void setMessage(MultiChatMessage message) {
        this.message = message;
    }

    public String getFinish_reason() {
        return finish_reason;
    }

    public void setFinish_reason(String finish_reason) {
        this.finish_reason = finish_reason;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "MultiResponseChoice [message=" + message + ", finish_reason=" + finish_reason + ", index=" + index
                + "]";
    }
}
