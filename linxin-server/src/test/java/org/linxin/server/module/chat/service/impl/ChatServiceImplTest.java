package org.linxin.server.module.chat.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linxin.server.module.chat.converter.ChatConverter;
import org.linxin.server.module.chat.entity.Conversation;
import org.linxin.server.module.chat.entity.Message;
import org.linxin.server.module.chat.mapper.*;
import org.linxin.server.module.chat.model.request.SendMessageRequest;
import org.linxin.server.module.group.mapper.*;
import org.linxin.server.module.user.entity.User;
import org.linxin.server.module.user.mapper.*;
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
    private org.linxin.server.module.chat.service.IMessageService messageService;
    @Mock
    private org.linxin.server.module.contact.service.IFriendService friendService;
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
        when(friendService.isFriend(senderId, 2L)).thenReturn(true);
        when(conversationMapper.selectOne(any()))
                .thenReturn(senderConv)
                .thenReturn(receiverConv);

        when(messageService.getNextSequenceId()).thenReturn(1L);
        chatService.sendMessage(senderId, request);

        verify(messageMapper, times(2)).insert(any(Message.class));
        verify(webSocketHandler, times(1)).sendMessageToUser(eq(2L), any());
    }
}
