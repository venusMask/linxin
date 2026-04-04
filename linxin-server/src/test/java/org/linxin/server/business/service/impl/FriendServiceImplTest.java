package org.linxin.server.business.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linxin.server.business.entity.Friend;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.mapper.FriendMapper;
import org.linxin.server.business.mapper.UserMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FriendServiceImplTest {

    @Mock
    private FriendMapper friendMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private FriendServiceImpl friendService;

    @Test
    public void testResolveRecipient_ByNickname() {
        Long userId = 1L;
        String keyword = "小王";

        Friend mockFriend = new Friend();
        mockFriend.setFriendNickname("小王");
        mockFriend.setFriendId(2L);

        when(userMapper.selectOne(any())).thenReturn(null); // 假设 username 没搜到
        when(friendMapper.selectList(any())).thenReturn(List.of(mockFriend));

        List<Friend> results = friendService.resolveRecipient(userId, keyword);

        assertFalse(results.isEmpty());
        assertEquals("小王", results.get(0).getFriendNickname());
    }

    @Test
    public void testResolveRecipient_ByTags() {
        Long userId = 1L;
        String keyword = "媳妇";

        Friend mockFriend = new Friend();
        mockFriend.setTags("媳妇,爱人");
        mockFriend.setFriendId(2L);

        when(userMapper.selectOne(any())).thenReturn(null);
        when(friendMapper.selectList(any())).thenReturn(List.of(mockFriend));

        List<Friend> results = friendService.resolveRecipient(userId, keyword);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getTags().contains("媳妇"));
    }

    @Test
    public void testResolveRecipient_ByUsername() {
        Long userId = 1L;
        String username = "lx_user_888";

        User mockUser = new User();
        mockUser.setId(888L);
        mockUser.setUsername(username);

        Friend mockFriend = new Friend();
        mockFriend.setFriendId(888L);

        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(friendMapper.selectList(any())).thenReturn(List.of(mockFriend));

        List<Friend> results = friendService.resolveRecipient(userId, username);

        assertFalse(results.isEmpty());
        assertEquals(888L, results.get(0).getFriendId());
    }
}
