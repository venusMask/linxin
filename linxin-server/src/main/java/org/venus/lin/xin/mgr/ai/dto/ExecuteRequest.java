package org.venus.lin.xin.mgr.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExecuteRequest {
    private List<ChatResponse.ToolCall> toolCalls;
}