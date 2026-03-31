package org.linxin.server.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExecuteRequest {
    private List<ChatResponse.ToolCall> toolCalls;
}