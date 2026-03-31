package org.venus.lin.xin.mgr.ai.tools;

import lombok.Data;
import java.util.List;

@Data
public class AITool {
    private String id;
    private String name;
    private String description;
    private String icon;
    private boolean requireConfirm;
    private List<ToolParam> params;
    private String group;
}