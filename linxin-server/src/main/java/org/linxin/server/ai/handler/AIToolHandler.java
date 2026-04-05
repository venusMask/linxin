package org.linxin.server.ai.handler;

import java.util.Map;

public interface AIToolHandler {

    /**
     * 工具名称，对应 default_tools.json 中的 id 或 name
     */
    String getToolName();

    /**
     * 执行具体的业务逻辑
     * 
     * @param userId
     *            操作发起人
     * @param arguments
     *            大模型解析出来的参数
     * @return 返回统一格式的结果 Map
     */
    Map<String, Object> execute(Long userId, Map<String, Object> arguments);
}
