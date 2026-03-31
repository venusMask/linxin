package org.linxin.server.ai.tools;

import lombok.Data;
import java.util.List;

@Data
public class ToolsConfig {
    private String version;
    private List<AITool> tools;
}