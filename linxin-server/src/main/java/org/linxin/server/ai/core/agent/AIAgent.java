package org.linxin.server.ai.core.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.ai.core.context.ConversationContext;
import org.linxin.server.ai.core.dto.ChatMessage;
import org.linxin.server.ai.core.dto.ModelResponse;
import org.linxin.server.ai.core.model.LLMModel;
import org.linxin.server.ai.core.tool.AITool;
import org.linxin.server.ai.core.tool.ToolProvider;
import org.linxin.server.module.auth.service.IAgentService;
import org.springframework.stereotype.Component;

/**
 * 核心智能体：负责控制推理循环 (Reasoning Loop)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIAgent {

    private final LLMModel model;
    private final ToolProvider toolProvider;
    private final IAgentService agentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_ITERATIONS = 5;

    public ModelResponse run(Long userId, String userInput, List<ChatMessage> previousMessages) {
        // 1. 初始化上下文
        String systemPrompt = "你是一个全能的即时通讯助手。请根据用户的需求，灵活调用工具来完成任务。\n" +
                "【重要指令】\n" +
                "1. 只要用户要求执行操作（如发送消息、加好友、建群），你必须调用相应的工具，绝不能仅仅通过回复文字来描述操作过程。\n" +
                "2. 如果一个任务需要多个步骤（例如：先加好友，再建群），请先执行第一步，拿到工具返回的结果后，再根据结果执行下一步。\n" +
                "3. 在工具调用期间，不要向用户解释你的步骤，直到所有工具执行完毕并获得最终结果后，再给用户一个总结性的答复。\n" +
                "4. 如果工具返回 AMBIGUOUS_RECIPIENT，说明存在多个匹配项，请你根据返回的 choices 列表，主动向用户确认他想要联系的是哪一个。";

        ConversationContext context = new ConversationContext(systemPrompt);
        if (previousMessages != null && !previousMessages.isEmpty()) {
            context.addPreviousMessages(previousMessages);
        }
        context.addUserMessage(userInput);

        List<Map<String, Object>> toolSpecs = toolProvider.getTools().stream()
                .map(AITool::toOpenAIFormat)
                .collect(Collectors.toList());

        ModelResponse.Usage totalUsage = ModelResponse.Usage.builder().build();

        // 2. 进入 ReAct 循环
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            log.debug("Agent Iteration {}: Calling LLM...", i + 1);

            ModelResponse response = model.chat(context.getMessages(), toolSpecs);
            accumulateUsage(totalUsage, response.getUsage());

            if (!response.hasToolCalls()) {
                // 模型直接给出了回答，结束循环
                response.setUsage(totalUsage);
                return response;
            }

            // 3. 执行工具调用
            context.addAssistantMessage(response.getContent(), response.getToolCalls());

            for (ChatMessage.ToolCall tc : response.getToolCalls()) {
                String toolName = tc.getFunction().getName();
                String argsJson = tc.getFunction().getArguments();

                log.info("Agent executing tool: {} with args: {}", toolName, argsJson);

                try {
                    Map<String, Object> arguments = objectMapper.readValue(argsJson, Map.class);
                    // 调用业务逻辑处理器
                    Map<String, Object> result = agentService.callTool(userId, toolName, arguments, "AgentEngine");

                    // 增强反馈：如果是歧义，返回更详细的上下文给 LLM
                    if ("AMBIGUOUS_RECIPIENT".equals(result.get("status"))) {
                        log.warn("Ambiguous recipient detected, feeding choices back to LLM");
                    }

                    // 将结果喂回给模型
                    String resultJson = objectMapper.writeValueAsString(result);
                    context.addToolMessage(tc.getId(), resultJson);
                } catch (Exception e) {
                    log.error("Tool execution failed", e);
                    context.addToolMessage(tc.getId(), "Error: " + e.getMessage());
                }
            }

            // 继续下一轮循环，让模型根据工具结果进行思考
        }

        return ModelResponse.builder()
                .content("抱歉，我尝试了多次但未能完成您的复杂请求。")
                .usage(totalUsage)
                .build();
    }

    private void accumulateUsage(ModelResponse.Usage total, ModelResponse.Usage current) {
        if (current == null)
            return;
        total.setPromptTokens(total.getPromptTokens() + current.getPromptTokens());
        total.setCompletionTokens(total.getCompletionTokens() + current.getCompletionTokens());
        total.setTotalTokens(total.getTotalTokens() + current.getTotalTokens());
    }
}
