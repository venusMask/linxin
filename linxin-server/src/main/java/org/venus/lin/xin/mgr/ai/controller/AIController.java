package org.venus.lin.xin.mgr.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.venus.lin.xin.mgr.ai.dto.ChatRequest;
import org.venus.lin.xin.mgr.ai.dto.ChatResponse;
import org.venus.lin.xin.mgr.ai.dto.ModifyParamsRequest;
import org.venus.lin.xin.mgr.ai.service.AIService;
import org.venus.lin.xin.mgr.ai.tools.AITool;
import org.venus.lin.xin.mgr.common.result.Result;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = aiService.processUserInput(request);
        return Result.success(response);
    }

    @PostMapping("/modify")
    public Result<ChatResponse> modifyParams(@RequestBody ModifyParamsRequest request) {
        ChatResponse response = aiService.modifyParams(
                request.getOriginalResponse(),
                request.getModification()
        );
        return Result.success(response);
    }

    @GetMapping("/tools")
    public Result<List<AITool>> getTools() {
        List<AITool> tools = aiService.getAvailableTools();
        return Result.success(tools);
    }

    @GetMapping("/tools/version")
    public Result<String> getToolsVersion() {
        return Result.success(aiService.getToolsVersion());
    }
}