package org.linxin.server.ai.service;

import org.linxin.server.ai.dto.ChatRequest;
import org.linxin.server.ai.dto.ChatResponse;

public interface AIService {

    ChatResponse processUserInput(ChatRequest request);

    ChatResponse modifyParams(ChatResponse originalResponse, String modification);

    String getToolsVersion();

    java.util.Map<String, Object> getUsageStatistics(Long userId, Integer days);
}
