package org.linxin.server.ai.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linxin.server.ai.adapter.AIModelAdapter;
import org.linxin.server.ai.adapter.FailoverAIModelAdapter;
import org.linxin.server.ai.config.AIConfig;
import org.linxin.server.ai.dto.ChatRequest;
import org.linxin.server.ai.dto.ChatResponse;
import org.linxin.server.ai.mapper.AIUsageLogMapper;
import org.linxin.server.ai.tools.ToolsConfig;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AIServiceImplTest {

    @Mock private AIConfig aiConfig;
    @Mock private ToolsConfig toolsConfig;
    @Mock private AIUsageLogMapper usageLogMapper;
    @Mock private AIModelAdapter mockAdapter1;
    @Mock private AIModelAdapter mockAdapter2;

    @InjectMocks
    private AIServiceImpl aiService;

    @BeforeEach
    public void setup() {
        // 创建一个 Failover 适配器，包含两个 Mock 适配器
        FailoverAIModelAdapter failoverAdapter = new FailoverAIModelAdapter(List.of(mockAdapter1, mockAdapter2));
        ReflectionTestUtils.setField(aiService, "activeAdapter", failoverAdapter);
        
        when(toolsConfig.getTools()).thenReturn(Collections.emptyList());
    }

    @Test
    public void testProcessUserInput_Failover() {
        ChatRequest request = new ChatRequest();
        request.setContent("Hi");
        request.setUserId(1L);

        // 模拟第一个适配器报错 (额度不足)
        ChatResponse errorResponse = new ChatResponse();
        errorResponse.setIntent("error");
        errorResponse.setAiText("Quota exceeded");
        
        when(mockAdapter1.chat(any(), any())).thenReturn(errorResponse);
        when(mockAdapter1.getProviderName()).thenReturn("Provider1");

        // 模拟第二个适配器成功
        ChatResponse successResponse = new ChatResponse();
        successResponse.setIntent("chat");
        successResponse.setAiText("Success from Backup");
        ChatResponse.Usage usage = new ChatResponse.Usage();
        usage.setTotalTokens(100);
        successResponse.setUsage(usage);

        when(mockAdapter2.chat(any(), any())).thenReturn(successResponse);
        when(mockAdapter2.getProviderName()).thenReturn("Provider2");
        when(mockAdapter2.getModelName()).thenReturn("Model2");

        ChatResponse result = aiService.processUserInput(request);

        assertNotNull(result);
        assertEquals("chat", result.getIntent());
        assertEquals("Success from Backup", result.getAiText());
        
        // 验证是否调用了第二个适配器
        verify(mockAdapter2, times(1)).chat(any(), any());
    }
}
