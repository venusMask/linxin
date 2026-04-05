package org.linxin.server.ai.controller;

import lombok.RequiredArgsConstructor;
import org.linxin.server.ai.dto.ChatRequest;
import org.linxin.server.ai.dto.ChatResponse;
import org.linxin.server.ai.dto.ModifyParamsRequest;
import org.linxin.server.ai.service.AIService;
import org.linxin.server.common.result.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = aiService.processUserInput(request, java.util.Collections.emptyList());
        return Result.success(response);
    }

    @PostMapping("/modify")
    public Result<ChatResponse> modifyParams(@RequestBody ModifyParamsRequest request) {
        ChatResponse response = aiService.modifyParams(
                request.getOriginalResponse(),
                request.getModification());
        return Result.success(response);
    }

    @GetMapping("/tools/version")
    public Result<String> getToolsVersion() {
        return Result.success(aiService.getToolsVersion());
    }

    @GetMapping("/usage")
    public Result<java.util.Map<String, Object>> getUsage(@RequestAttribute("userId") Long userId,
            @RequestParam(required = false, defaultValue = "3") Integer days) {
        return Result.success(aiService.getUsageStatistics(userId, days));
    }
}
