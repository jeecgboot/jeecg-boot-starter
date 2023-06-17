package org.jeecg.chatgpt.dto.chat;

import java.time.LocalDate;
import java.util.List;
import org.jeecg.chatgpt.dto.Usage;


/**
 * 
 * @author liuliu
 *
 */
public class MultiChatResponse {
    private String id;
    private String object;
    private LocalDate created;
    private String model;
    private List<MultiResponseChoice> choices;
    private Usage usage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<MultiResponseChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<MultiResponseChoice> choices) {
        this.choices = choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    @Override
    public String toString() {
        return "MultiChatResponse [id=" + id + ", object=" + object + ", created=" + created + ", model=" + model
                + ", choices=" + choices + ", usage=" + usage + "]";
    }
}
