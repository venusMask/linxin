package org.linxin.server.ai.adapter;

import org.linxin.server.ai.dto.ChatResponse;
import org.linxin.server.ai.tools.AITool;
import java.util.List;

public interface AIModelAdapter {
    ChatResponse chat(String userInput, List<AITool> tools);
    String getProviderName();
    String getModelName();
    void dispose();
}