package org.linxin.server.ai.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ChatResponse {
    private String intent;
    private List<ToolCall> toolCalls;
    private String aiText;
    private String reply; // 增加 reply 字段
    private boolean needConfirm;
    private Usage usage;

    public String getReply() {
        return reply != null ? reply : aiText;
    }

    @Data
    public static class ToolCall {
        private String toolId;
        private String toolName;
        private String functionName; // 增加此字段
        private Map<String, Object> params;
        private Map<String, Object> arguments; // 增加此字段
        private String description;

        public String getFunctionName() {
            return functionName != null ? functionName : toolName;
        }

        public Map<String, Object> getArguments() {
            return arguments != null ? arguments : params;
        }
    }

    @Data
    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }
}
