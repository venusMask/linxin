package org.linxin.server.ai.service;

import java.util.List;
import org.linxin.server.ai.core.dto.ChatMessage;
import org.linxin.server.ai.dto.ChatRequest;
import org.linxin.server.ai.dto.ChatResponse;

public interface AIService {

    ChatResponse processUserInput(ChatRequest request, List<ChatMessage> previousMessages);

    ChatResponse modifyParams(ChatResponse originalResponse, String modification);

    String getToolsVersion();

    java.util.Map<String, Object> getUsageStatistics(Long userId, Integer days);
}
