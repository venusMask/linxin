package org.linxin.server.business.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.linxin.server.business.entity.AgentToken;
import org.linxin.server.business.service.IAgentService;
import org.linxin.server.business.service.IAgentTokenService;
import org.linxin.server.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Agent开放接口")
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final IAgentService agentService;
    private final IAgentTokenService agentTokenService;

    @GetMapping("/manifest")
    @Operation(summary = "能力发现接口")
    public Result<Map<String, Object>> getManifest() {
        return Result.success(agentService.getManifest());
    }

    @PostMapping("/call")
    @Operation(summary = "结构化工具调用接口")
    public Result<Map<String, Object>> callTool(@RequestBody Map<String, Object> request,
                                               @RequestAttribute("userId") Long userId) {
        String toolName = (String) request.get("tool");
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
        String agentName = (String) request.getOrDefault("agentName", "未知Agent");
        return Result.success(agentService.callTool(userId, toolName, arguments, agentName));
    }

    // --- 令牌管理接口 ---

    @GetMapping("/tokens")
    @Operation(summary = "获取当前用户的Agent令牌列表")
    public Result<List<AgentToken>> getTokens(@RequestAttribute("userId") Long userId) {
        return Result.success(agentTokenService.getUserTokens(userId));
    }

    @PostMapping("/tokens/generate")
    @Operation(summary = "生成新的Agent令牌")
    public Result<AgentToken> generateToken(@RequestAttribute("userId") Long userId,
                                           @RequestBody Map<String, Object> request) {
        String agentName = (String) request.get("agentName");
        String scopes = (String) request.get("scopes");
        Integer expireDays = (Integer) request.get("expireDays");
        
        java.time.LocalDateTime expireTime = null;
        if (expireDays != null && expireDays > 0) {
            expireTime = java.time.LocalDateTime.now().plusDays(expireDays);
        }
        
        return Result.success(agentTokenService.generateToken(userId, agentName, scopes, expireTime));
    }

    @DeleteMapping("/tokens/{tokenId}")
    @Operation(summary = "撤销Agent令牌")
    public Result<String> revokeToken(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long tokenId) {
        agentTokenService.revokeToken(userId, tokenId);
        return Result.success("令牌已撤销");
    }

    @PostMapping("/execute")
    @Operation(summary = "意图执行接口 (Legacy)")
    public Result<Map<String, Object>> execute(@RequestBody Map<String, String> request,
                                               @RequestAttribute("userId") Long userId) {
        String command = request.get("command");
        String agentName = request.getOrDefault("agentName", "未知Agent");
        return Result.success(agentService.executeCommand(userId, command, agentName));
    }
}
