package org.linxin.server.module.auth.service;

import java.util.Map;

public interface IAgentService {
    Map<String, Object> getManifest();
    Map<String, Object> callTool(Long userId, String toolName, Map<String, Object> arguments, String agentName);
    Map<String, Object> executeCommand(Long userId, String command, String agentName);
}
