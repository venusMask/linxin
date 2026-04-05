package org.linxin.server.ai.core.model;

import java.util.List;
import java.util.Map;
import org.linxin.server.ai.core.dto.ChatMessage;
import org.linxin.server.ai.core.dto.ModelResponse;

/**
 * 核心模型契约：不再关心具体的业务，只负责将消息序列转换为模型响应
 */
public interface LLMModel {
    /**
     * 调用模型
     * 
     * @param messages
     *            历史消息列表
     * @param tools
     *            可用工具描述（OpenAI 格式）
     * @return 模型响应
     */
    ModelResponse chat(List<ChatMessage> messages, List<Map<String, Object>> tools);

    String getProviderName();
    String getModelName();
}
