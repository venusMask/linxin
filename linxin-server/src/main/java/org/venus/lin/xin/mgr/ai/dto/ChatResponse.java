package org.venus.lin.xin.mgr.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatResponse {
    private String intent;
    private List<ToolCall> toolCalls;
    private String aiText;
    private boolean needConfirm;
    private String status;

    @Data
    public static class ToolCall {
        private String toolId;
        private String toolName;
        private Map<String, Object> params;
        private String description;
    }
}