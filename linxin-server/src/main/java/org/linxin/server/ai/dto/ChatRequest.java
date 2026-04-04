package org.linxin.server.ai.dto;

import java.util.Map;
import lombok.Data;

@Data
public class ChatRequest {
    private String userInput;
    private String content; // 兼容 AIService 调用
    private String conversationId;
    private Map<String, Object> context;
    private Long userId;

    public String getContent() {
        return content != null ? content : userInput;
    }

    public void setContent(String content) {
        this.content = content;
        this.userInput = content;
    }
}
