package org.linxin.server.ai.tools;

import java.util.List;
import lombok.Data;

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
