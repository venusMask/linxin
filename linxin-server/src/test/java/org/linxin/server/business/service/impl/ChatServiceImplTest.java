package org.linxin.server.business.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linxin.server.business.converter.ChatConverter;
import org.linxin.server.business.entity.Conversation;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.mapper.*;
import org.linxin.server.business.model.request.SendMessageRequest;
import org.linxin.server.websocket.WebSocketHandler;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChatServiceImplTest {

    @Mock
    private ConversationMapper conversationMapper;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ChatConverter chatConverter;
    @Mock
    private WebSocketHandler webSocketHandler;
    @Mock
    private GroupMapper groupMapper;
    @Mock
    private GroupMemberMapper groupMemberMapper;
    @Mock
    private MessageStatusMapper messageStatusMapper;

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    public void setup() {
        // 初始化 MyBatis-Plus 的 TableInfo 缓存，防止 LambdaQueryWrapper 报错
        MybatisConfiguration config = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), Conversation.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), Message.class);
    }

    @Test
    public void testGetOrCreateConversation_Existing() {
        Long userId = 1L;
        Long peerId = 2L;
        Conversation mockConv = new Conversation();
        mockConv.setId(100L);

        when(conversationMapper.selectOne(any())).thenReturn(mockConv);

        Conversation result = chatService.getOrCreateConversation(userId, peerId);

        assertNotNull(result);
        assertEquals(100L, result.getId());
    }

    @Test
    public void testSendMessage_Success() {
        Long senderId = 1L;
        SendMessageRequest request = new SendMessageRequest();
        request.setReceiverId(2L);
        request.setContent("Hello");
        request.setMessageType(1);

        User receiver = new User();
        receiver.setId(2L);

        Conversation senderConv = new Conversation();
        senderConv.setId(10L);
        senderConv.setMuteStatus(0); // 初始化状态

        Conversation receiverConv = new Conversation();
        receiverConv.setId(11L);
        receiverConv.setMuteStatus(0); // 初始化状态

        when(userMapper.selectById(2L)).thenReturn(receiver);
        when(conversationMapper.selectOne(any()))
                .thenReturn(senderConv)
                .thenReturn(receiverConv);

        chatService.sendMessage(senderId, request);

        verify(messageMapper, times(2)).insert(any(Message.class));
        verify(webSocketHandler, times(1)).sendMessageToUser(eq(2L), any());
    }
}
