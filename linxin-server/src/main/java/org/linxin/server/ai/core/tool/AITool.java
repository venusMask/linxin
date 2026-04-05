package org.linxin.server.ai.core.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AITool {
    private String id;
    private String name;
    private String description;
    private String icon;
    private Boolean implemented;
    private List<Param> params;
    private Boolean requireConfirm;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Param {
        private String name;
        private String type;
        private String description;
        private boolean required;
        private String itemsType; // 针对 array 类型
        private Integer maxLength; // 补充 JSON 中的字段
    }

    /**
     * 转换为 OpenAI 的 tools 格式
     */
    public Map<String, Object> toOpenAIFormat() {
        Map<String, Object> function = new HashMap<>();
        function.put("name", id); // 使用 ID 作为模型识别的函数名
        function.put("description", description);

        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        if (params != null) {
            for (Param p : params) {
                Map<String, Object> pMap = new HashMap<>();
                pMap.put("type", p.getType());
                pMap.put("description", p.getDescription());
                if ("array".equals(p.getType()) && p.getItemsType() != null) {
                    pMap.put("items", Map.of("type", p.getItemsType()));
                }
                properties.put(p.getName(), pMap);
                if (p.isRequired())
                    required.add(p.getName());
            }
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);

        function.put("parameters", parameters);

        return Map.of("type", "function", "function", function);
    }
}
