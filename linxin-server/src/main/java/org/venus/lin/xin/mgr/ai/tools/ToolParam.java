package org.venus.lin.xin.mgr.ai.tools;

import lombok.Data;
import java.util.List;

@Data
public class ToolParam {
    private String name;
    private String type;
    private String description;
    private boolean required;
    private Integer maxLength;
    private List<String> enumValues;
    private String itemsType;
}