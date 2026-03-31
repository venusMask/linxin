package org.venus.lin.xin.mgr.ai.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ChatRequest {
    private String userInput;
    private String conversationId;
    private Map<String, Object> context;
    private Long userId;
}