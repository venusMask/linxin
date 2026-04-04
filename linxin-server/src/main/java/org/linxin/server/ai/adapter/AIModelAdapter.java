package org.linxin.server.ai.adapter;

import java.util.List;
import org.linxin.server.ai.dto.ChatResponse;
import org.linxin.server.ai.tools.AITool;

public interface AIModelAdapter {
    ChatResponse chat(String userInput, List<AITool> tools);
    String getProviderName();
    String getModelName();
    void dispose();
}
