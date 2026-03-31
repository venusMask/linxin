package org.venus.lin.xin.mgr.ai.service;

import org.venus.lin.xin.mgr.ai.dto.ChatRequest;
import org.venus.lin.xin.mgr.ai.dto.ChatResponse;
import org.venus.lin.xin.mgr.ai.tools.AITool;

import java.util.List;

public interface AIService {

    ChatResponse processUserInput(ChatRequest request);

    ChatResponse modifyParams(ChatResponse originalResponse, String modification);

    List<AITool> getAvailableTools();

    String getToolsVersion();
}