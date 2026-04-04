package org.linxin.server.ai.tools;

import lombok.Data;
import java.util.List;

@Data
public class AITool {
    private String id;
    private boolean implemented;
    private String name;
    private String description;
    private String icon;
    private boolean requireConfirm;
    private List<ToolParam> params;
    private String group;
}