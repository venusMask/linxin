package org.linxin.server.business.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linxin.server.business.entity.Conversation;
import org.linxin.server.business.entity.Group;
import org.linxin.server.business.entity.GroupMember;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.mapper.ConversationMapper;
import org.linxin.server.business.mapper.GroupMapper;
import org.linxin.server.business.mapper.GroupMemberMapper;
import org.linxin.server.business.mapper.UserMapper;
import org.linxin.server.business.model.request.CreateGroupRequest;
import org.linxin.server.business.vo.GroupVO;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupServiceImplTest {

    @Mock private GroupMapper groupMapper;
    @Mock private GroupMemberMapper groupMemberMapper;
    @Mock private ConversationMapper conversationMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private GroupServiceImpl groupService;

    @BeforeEach
    public void setup() {
        MybatisConfiguration config = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), Group.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), GroupMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), Conversation.class);
    }

    @Test
    public void testCreateGroup() {
        Long userId = 1L;
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("Test Group");

        User owner = new User();
        owner.setId(userId);
        owner.setNickname("Owner");

        when(userMapper.selectById(userId)).thenReturn(owner);
        
        // 模拟 selectById 返回刚才插入的群组
        Group mockGroup = new Group();
        mockGroup.setId(100L);
        mockGroup.setName("Test Group");
        mockGroup.setOwnerId(userId);
        mockGroup.setDeleted(0);
        
        when(groupMapper.selectById(any())).thenReturn(mockGroup);

        GroupVO result = groupService.createGroup(userId, request);

        assertNotNull(result);
        assertEquals("Test Group", result.getName());
        verify(groupMapper, times(1)).insert(any(Group.class));
        verify(groupMemberMapper, atLeastOnce()).insert(any(GroupMember.class));
    }
}
