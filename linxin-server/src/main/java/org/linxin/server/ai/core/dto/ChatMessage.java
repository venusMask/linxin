package org.linxin.server.ai.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {
    /**
     * 角色: system, user, assistant, tool
     */
    private String role;

    /**
     * 消息文本内容
     */
    private String content;

    /**
     * 仅当 role=tool 时，关联的工具调用 ID
     */
    @JsonProperty("tool_call_id")
    private String toolCallId;

    /**
     * 仅当 role=assistant 时，模型生成的工具调用列表
     */
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        private String id;
        private String type; // 通常为 "function"
        private Function function;

        public String getId() {
            return id;
        }
        public Function getFunction() {
            return function;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Function {
        private String name;
        private String arguments; // JSON 字符串

        public String getName() {
            return name;
        }
        public String getArguments() {
            return arguments;
        }
    }

    // 快捷创建方法
    public static ChatMessage system(String content) {
        return ChatMessage.builder().role("system").content(content).build();
    }
    public static ChatMessage user(String content) {
        return ChatMessage.builder().role("user").content(content).build();
    }
    public static ChatMessage assistant(String content) {
        return ChatMessage.builder().role("assistant").content(content).build();
    }
    public static ChatMessage tool(String toolCallId, String content) {
        return ChatMessage.builder().role("tool").toolCallId(toolCallId).content(content).build();
    }
}
