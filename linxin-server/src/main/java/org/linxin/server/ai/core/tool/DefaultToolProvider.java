package org.linxin.server.ai.core.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultToolProvider implements ToolProvider {

    private final Map<String, AITool> tools = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadTools() {
        try {
            ClassPathResource resource = new ClassPathResource("tools/default_tools.json");
            try (InputStream is = resource.getInputStream()) {
                Map<String, Object> data = objectMapper.readValue(is, Map.class);
                List<Map<String, Object>> toolsList = (List<Map<String, Object>>) data.get("tools");

                for (Map<String, Object> toolMap : toolsList) {
                    AITool tool = objectMapper.convertValue(toolMap, AITool.class);
                    tools.put(tool.getId(), tool);
                    log.info("Loaded AI Tool config: {}", tool.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to load AI tools configuration", e);
        }
    }

    @Override
    public List<AITool> getTools() {
        return new ArrayList<>(tools.values());
    }

    @Override
    public AITool getTool(String id) {
        return tools.get(id);
    }
}
