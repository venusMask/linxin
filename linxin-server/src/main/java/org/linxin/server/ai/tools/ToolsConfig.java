package org.linxin.server.ai.tools;

import java.util.List;
import lombok.Data;

@Data
public class ToolsConfig {
    private String version;
    private List<AITool> tools;
}
