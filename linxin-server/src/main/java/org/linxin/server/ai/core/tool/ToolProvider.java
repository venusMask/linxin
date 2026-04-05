package org.linxin.server.ai.core.tool;

import java.util.List;

public interface ToolProvider {
    List<AITool> getTools();
    AITool getTool(String id);
}
