package org.linxin.server.ai.service;

import org.linxin.server.ai.dto.ChatRequest;
import org.linxin.server.ai.dto.ChatResponse;
import org.linxin.server.ai.tools.AITool;

import java.util.List;

public interface AIService {

    ChatResponse processUserInput(ChatRequest request);

    ChatResponse modifyParams(ChatResponse originalResponse, String modification);

    List<AITool> getAvailableTools();

    String getToolsVersion();
}