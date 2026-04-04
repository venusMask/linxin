package org.linxin.server.business.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.mapper.MessageMapper;
import org.linxin.server.websocket.WebSocketHandler;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceImplTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private WebSocketHandler webSocketHandler;

    @InjectMocks
    private MessageServiceImpl messageService;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(messageService, "baseMapper", messageMapper);
    }

    @Test
    public void testSendAgentMessage() {
        Long senderId = 1001L;
        Long receiverId = 2002L;
        String content = "Hello from Agent";
        String agentName = "OpenClaw";

        Message result = messageService.sendAgentMessage(senderId, receiverId, content, agentName);

        assertNotNull(result);
        assertEquals(content, result.getContent());
        assertTrue(result.getIsAi());
        assertEquals(agentName, result.getSenderType());
        assertNotNull(result.getSequenceId());

        verify(messageMapper, times(1)).insert(any(Message.class));
        verify(webSocketHandler, times(2)).sendMessageToUser(anyLong(), any());
    }
}
