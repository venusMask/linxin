package org.linxin.server.ai.core.context;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.linxin.server.ai.core.dto.ChatMessage;

/**
 * 维护单次任务的对话窗口
 */
public class ConversationContext {
    @Getter
    private final List<ChatMessage> messages = new ArrayList<>();

    public ConversationContext(String systemPrompt) {
        messages.add(ChatMessage.system(systemPrompt));
    }

    public void addUserMessage(String content) {
        messages.add(ChatMessage.user(content));
    }

    public void addAssistantMessage(String content, List<ChatMessage.ToolCall> toolCalls) {
        messages.add(ChatMessage.builder()
                .role("assistant")
                .content(content != null ? content : "")
                .toolCalls(toolCalls)
                .build());
    }

    public void addToolMessage(String toolCallId, String content) {
        messages.add(ChatMessage.tool(toolCallId, content));
    }
}
