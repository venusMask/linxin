package org.venus.lin.xin.mgr.ai.adapter;

import org.venus.lin.xin.mgr.ai.dto.ChatResponse;
import org.venus.lin.xin.mgr.ai.tools.AITool;
import java.util.List;

public interface AIModelAdapter {
    ChatResponse chat(String userInput, List<AITool> tools);
    void dispose();
}