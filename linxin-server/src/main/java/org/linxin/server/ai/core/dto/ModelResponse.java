package org.linxin.server.ai.core.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelResponse {
    /**
     * 模型回复的文本 (如果有)
     */
    private String content;

    /**
     * 模型请求的工具调用 (如果有)
     */
    private List<ChatMessage.ToolCall> toolCalls;

    /**
     * Token 消耗统计
     */
    private Usage usage;

    @Data
    @Builder
    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
