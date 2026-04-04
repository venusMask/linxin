package org.linxin.server.ai.dto;

import java.util.List;
import lombok.Data;

@Data
public class ExecuteRequest {
    private List<ChatResponse.ToolCall> toolCalls;
}
