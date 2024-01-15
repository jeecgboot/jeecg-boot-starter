package org.jeecg.chatgpt.dto.chat;

import lombok.Data;
import lombok.Getter;

/**
 * 多角色聊天消息
 *
 * @author chenrui
 * @date 2024/1/12 17:47
 */
@Data
public class MultiChatMessage {
    /**
     * 消息角色
     */
    private String role;
    /**
     * 消息正文
     */
    private String content;
    /**
     * 参与者的可选名称
     */
    private String name;

    public MultiChatMessage() {
    }

    public MultiChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public MultiChatMessage(Builder builder) {
        this.setContent(builder.content);
        this.setRole(builder.role);
        this.setName(builder.name);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        /**
         * 角色
         */
        private String role;

        /**
         * 正文
         */
        private String content;

        /**
         * 参与者姓名
         */
        private String name;

        public Builder() {
        }

        /**
         * 角色
         *
         * @param role
         * @return
         * @author chenrui
         * @date 2024/1/12 17:50
         */
        public MultiChatMessage.Builder role(MultiChatMessage.Role role) {
            this.role = role.getName();
            return this;
        }

        public MultiChatMessage.Builder role(String role) {
            this.role = role;
            return this;
        }

        public MultiChatMessage.Builder content(String content) {
            this.content = content;
            return this;
        }

        public MultiChatMessage.Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 构造message
         * @return
         * @author chenrui
         * @date 2024/1/12 17:54
         */
        public MultiChatMessage build() {
            return new MultiChatMessage(this);
        }
    }

    /**
     * 角色枚举
     * @author chenrui
     * @date 2024/1/12 20:21
     */
    @Getter
    public static enum Role {
        /**
         * 系统消息
         */
        SYSTEM("system"),
        /**
         * 用户消息
         */
        USER("user"),
        /**
         * 助理消息
         */
        ASSISTANT("assistant"),
        /**
         * 函数
         */
        FUNCTION("function"),
        /**
         * 工具
         */
        TOOL("tool");

        private final String name;

        private Role(String name) {
            this.name = name;
        }
    }

}
