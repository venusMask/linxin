# LinXin MVP Bug Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all P0+P1 bugs identified in the MVP code review across backend (SpringBoot) and frontend (Flutter).

**Architecture:** Four fix groups applied in order: (1) Auth module, (2) WebSocket module, (3) Business logic, (4) Frontend. Each group is independently testable and leaves the system in a runnable state.

**Tech Stack:** SpringBoot 3.4.2 / Java 17 / MyBatis-Plus / Spring Security / JWT (backend); Flutter 3.10.8 / Dart / sqflite (frontend)

---

## File Map

**Modified (backend):**
- `linxin-server/src/main/java/org/linxin/server/auth/DPUserDetail.java`
- `linxin-server/src/main/java/org/linxin/server/auth/DPUserDetailLoginService.java`
- `linxin-server/src/main/java/org/linxin/server/auth/JwtAuthenticationFilter.java`
- `linxin-server/src/main/java/org/linxin/server/business/controller/AuthController.java`
- `linxin-server/src/main/java/org/linxin/server/business/controller/ChatController.java`
- `linxin-server/src/main/java/org/linxin/server/business/controller/FriendController.java`
- `linxin-server/src/main/java/org/linxin/server/business/controller/GroupController.java`
- `linxin-server/src/main/java/org/linxin/server/websocket/WebSocketConfig.java`
- `linxin-server/src/main/java/org/linxin/server/websocket/WebSocketHandler.java`
- `linxin-server/src/main/java/org/linxin/server/business/service/impl/GroupServiceImpl.java`
- `linxin-server/src/main/java/org/linxin/server/business/service/impl/ChatServiceImpl.java`
- `linxin-server/src/main/java/org/linxin/server/ai/service/impl/AIServiceImpl.java`
- `linxin-server/src/main/resources/application.yml`
- `linxin-server/src/main/resources/application-dev.yml`
- `linxin-server/src/main/resources/application-pro.yml`
- `linxin-server/src/main/resources/tools/default_tools.json`

**Created (backend):**
- `linxin-server/src/main/java/org/linxin/server/business/model/request/UpdateAnnouncementRequest.java`

**Modified (frontend):**
- `linxin-client/lib/services/auth_service.dart`
- `linxin-client/lib/services/db_service.dart`
- `linxin-client/lib/pages/chat_detail_page.dart`
- `linxin-client/lib/pages/chat_list_page.dart`
- `linxin-client/lib/pages/ai_chat_page.dart`

---

## Group 1 — Authentication Module

### Task 1: Fix DPUserDetail — implement UserDetails interface

**Files:**
- Modify: `linxin-server/src/main/java/org/linxin/server/auth/DPUserDetail.java`

- [ ] **Step 1: Replace DPUserDetail content**

```java
package org.linxin.server.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class DPUserDetail implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final Integer status;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return status != null && status == 1; }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add linxin-server/src/main/java/org/linxin/server/auth/DPUserDetail.java
git commit -m "fix: make DPUserDetail implement UserDetails interface"
```

---

### Task 2: Fix DPUserDetailLoginService — load user from database

**Files:**
- Modify: `linxin-server/src/main/java/org/linxin/server/auth/DPUserDetailLoginService.java`

- [ ] **Step 1: Implement loadUserByUsername**

```java
package org.linxin.server.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class DPUserDetailLoginService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return new DPUserDetail(user.getId(), user.getUsername(), user.getPassword(), user.getStatus());
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add linxin-server/src/main/java/org/linxin/server/auth/DPUserDetailLoginService.java
git commit -m "fix: implement loadUserByUsername to load user from database"
```

---

### Task 3: Fix JwtAuthenticationFilter — use real UserDetails + inject userId

**Files:**
- Modify: `linxin-server/src/main/java/org/linxin/server/auth/JwtAuthenticationFilter.java`

- [ ] **Step 1: Rewrite filter to load UserDetails and set userId attribute**

```java
package org.linxin.server.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final DPUserDetailLoginService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(jwt);
            Long userId = jwtService.extractUserId(jwt);

            if (username != null && userId != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (userDetails.isEnabled()) {
                    request.setAttribute("userId", userId);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.error("JWT Authentication failed: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add linxin-server/src/main/java/org/linxin/server/auth/JwtAuthenticationFilter.java
git commit -m "fix: load real UserDetails in JWT filter and inject userId as request attribute"
```

---

### Task 4: Fix all Controllers — use @RequestAttribute instead of @RequestHeader

**Files:**
- Modify: `linxin-server/src/main/java/org/linxin/server/business/controller/AuthController.java`
- Modify: `linxin-server/src/main/java/org/linxin/server/business/controller/ChatController.java`
- Modify: `linxin-server/src/main/java/org/linxin/server/business/controller/FriendController.java`
- Modify: `linxin-server/src/main/java/org/linxin/server/business/controller/GroupController.java`

- [ ] **Step 1: Fix AuthController — replace @RequestHeader + fix register return type**

```java
package org.linxin.server.business.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.linxin.server.auth.JwtService;
import org.linxin.server.business.converter.UserConverter;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.model.request.UserLoginRequest;
import org.linxin.server.business.model.request.UserRegisterRequest;
import org.linxin.server.business.service.IUserService;
import org.linxin.server.business.vo.UserVO;
import org.linxin.server.common.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证模块", description = "用户注册、登录相关接口")
public class AuthController {

    private final IUserService userService;
    private final JwtService jwtService;
    private final UserConverter userConverter;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<UserVO> register(@Valid @RequestBody UserRegisterRequest request) {
        User user = userService.register(request);
        return Result.success(userConverter.toVO(user));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginRequest request) {
        User user = userService.login(request);
        String token = jwtService.generateToken(user.getId(), user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        return Result.success(data);
    }

    @GetMapping("/userinfo")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getUserInfo(@RequestAttribute("userId") Long userId) {
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索用户")
    public Result<List<UserVO>> searchUsers(@RequestParam String keyword) {
        List<UserVO> users = userService.searchUsers(keyword);
        return Result.success(users);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "获取指定用户信息")
    public Result<UserVO> getUserById(@PathVariable Long userId) {
        UserVO userVO = userService.getUserById(userId);
        if (userVO == null) {
            return Result.error("用户不存在");
        }
        return Result.success(userVO);
    }
}
```

- [ ] **Step 2: Fix ChatController — replace all @RequestHeader("X-User-Id")**

```java
package org.linxin.server.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.linxin.server.business.entity.Conversation;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.model.request.SendMessageRequest;
import org.linxin.server.business.service.IChatService;
import org.linxin.server.business.vo.ConversationVO;
import org.linxin.server.business.vo.MessageVO;
import org.linxin.server.common.result.Result;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "聊天模块", description = "聊天相关操作接口")
public class ChatController {

    private final IChatService chatService;

    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表")
    public Result<IPage<ConversationVO>> getConversationList(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<ConversationVO> conversations = chatService.getConversationList(userId, pageNum, pageSize);
        return Result.success(conversations);
    }

    @GetMapping("/conversations/{peerId}")
    @Operation(summary = "获取或创建与指定用户的会话")
    public Result<Conversation> getOrCreateConversation(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long peerId) {
        Conversation conversation = chatService.getOrCreateConversation(userId, peerId);
        return Result.success(conversation);
    }

    @PostMapping("/messages")
    @Operation(summary = "发送消息")
    public Result<Message> sendMessage(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody SendMessageRequest request) {
        Message message;
        if (request.getConversationType() != null && request.getConversationType() == 1) {
            message = chatService.sendGroupMessage(userId, request);
        } else {
            message = chatService.sendMessage(userId, request);
        }
        return Result.success(message);
    }

    @GetMapping("/group/messages")
    @Operation(summary = "获取群消息列表")
    public Result<IPage<MessageVO>> getGroupMessageList(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long groupId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<MessageVO> messages = chatService.getGroupMessageList(groupId, userId, pageNum, pageSize);
        return Result.success(messages);
    }

    @GetMapping("/messages/{conversationId}")
    @Operation(summary = "获取消息列表")
    public Result<IPage<MessageVO>> getMessageList(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<MessageVO> messages = chatService.getMessageList(conversationId, pageNum, pageSize);
        return Result.success(messages);
    }

    @PostMapping("/messages/{conversationId}/read")
    @Operation(summary = "标记消息为已读")
    public Result<String> markAsRead(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long conversationId) {
        chatService.markMessagesAsRead(userId, conversationId);
        return Result.success("已标记为已读");
    }

    @PostMapping("/conversations/{conversationId}/top")
    @Operation(summary = "切换置顶状态")
    public Result<String> toggleTop(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long conversationId) {
        chatService.toggleTop(userId, conversationId);
        return Result.success("操作成功");
    }

    @PostMapping("/conversations/{conversationId}/mute")
    @Operation(summary = "切换静音状态")
    public Result<String> toggleMute(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long conversationId) {
        chatService.toggleMute(userId, conversationId);
        return Result.success("操作成功");
    }
}
```

- [ ] **Step 3: Fix FriendController — replace all @RequestHeader("X-User-Id")**

```java
package org.linxin.server.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.linxin.server.business.model.request.FriendApplyRequest;
import org.linxin.server.business.model.request.FriendHandleRequest;
import org.linxin.server.business.model.request.FriendListRequest;
import org.linxin.server.business.model.request.FriendUpdateRequest;
import org.linxin.server.business.service.IFriendService;
import org.linxin.server.business.vo.FriendApplyVO;
import org.linxin.server.business.vo.FriendVO;
import org.linxin.server.common.result.Result;

import java.util.List;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
@Tag(name = "好友管理模块", description = "好友相关操作接口")
public class FriendController {

    private final IFriendService friendService;

    @PostMapping("/list")
    @Operation(summary = "获取好友列表")
    public Result<IPage<FriendVO>> getFriendList(@RequestBody FriendListRequest request) {
        IPage<FriendVO> friends = friendService.getFriendList(
                request.getUsername(),
                request.getPageNum(),
                request.getPageSize()
        );
        return Result.success(friends);
    }

    @PostMapping("/apply")
    @Operation(summary = "发送好友申请")
    public Result<String> applyAddFriend(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody FriendApplyRequest request) {
        friendService.applyAddFriend(userId, request);
        return Result.success("申请已发送");
    }

    @GetMapping("/apply/received")
    @Operation(summary = "获取收到的好友申请列表")
    public Result<List<FriendApplyVO>> getReceivedApplyList(@RequestAttribute("userId") Long userId) {
        List<FriendApplyVO> applies = friendService.getReceivedApplyList(userId);
        return Result.success(applies);
    }

    @GetMapping("/apply/sent")
    @Operation(summary = "获取发出的好友申请列表")
    public Result<List<FriendApplyVO>> getSentApplyList(@RequestAttribute("userId") Long userId) {
        List<FriendApplyVO> applies = friendService.getSentApplyList(userId);
        return Result.success(applies);
    }

    @PostMapping("/apply/handle")
    @Operation(summary = "处理好友申请")
    public Result<String> handleFriendApply(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody FriendHandleRequest request) {
        friendService.handleFriendApply(userId, request);
        return Result.success("处理成功");
    }

    @PutMapping("/update")
    @Operation(summary = "更新好友信息")
    public Result<String> updateFriend(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody FriendUpdateRequest request) {
        friendService.updateFriend(userId, request);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{friendId}")
    @Operation(summary = "删除好友")
    public Result<String> deleteFriend(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long friendId) {
        friendService.deleteFriend(userId, friendId);
        return Result.success("删除成功");
    }

    @GetMapping("/check/{friendId}")
    @Operation(summary = "检查是否是好友关系")
    public Result<Boolean> isFriend(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long friendId) {
        boolean result = friendService.isFriend(userId, friendId);
        return Result.success(result);
    }
}
```

- [ ] **Step 4: Fix GroupController — replace @RequestHeader and fix announcement endpoint**

```java
package org.linxin.server.business.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.linxin.server.business.model.request.AddGroupMembersRequest;
import org.linxin.server.business.model.request.CreateGroupRequest;
import org.linxin.server.business.model.request.UpdateAnnouncementRequest;
import org.linxin.server.business.service.IGroupService;
import org.linxin.server.business.vo.GroupMemberVO;
import org.linxin.server.business.vo.GroupVO;
import org.linxin.server.common.result.Result;

import java.util.List;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
@Tag(name = "群组模块", description = "群组相关操作接口")
public class GroupController {

    private final IGroupService groupService;

    @PostMapping("/create")
    @Operation(summary = "创建群组")
    public Result<GroupVO> createGroup(
            @RequestAttribute("userId") Long userId,
            @RequestBody CreateGroupRequest request) {
        GroupVO group = groupService.createGroup(userId, request);
        return Result.success(group);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "获取群组信息")
    public Result<GroupVO> getGroupInfo(@PathVariable Long groupId) {
        GroupVO group = groupService.getGroupInfo(groupId);
        return Result.success(group);
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "获取群组成员列表")
    public Result<List<GroupMemberVO>> getGroupMembers(@PathVariable Long groupId) {
        List<GroupMemberVO> members = groupService.getGroupMembers(groupId);
        return Result.success(members);
    }

    @PostMapping("/{groupId}/members/add")
    @Operation(summary = "添加群成员")
    public Result<GroupVO> addMembers(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long groupId,
            @RequestBody AddGroupMembersRequest request) {
        GroupVO group = groupService.addMembers(groupId, userId, request);
        return Result.success(group);
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "移除群成员")
    public Result<String> removeMember(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long groupId,
            @PathVariable Long memberId) {
        groupService.removeMember(groupId, memberId, userId);
        return Result.success("成员已移除");
    }

    @PostMapping("/{groupId}/leave")
    @Operation(summary = "退出群组")
    public Result<String> leaveGroup(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long groupId) {
        groupService.leaveGroup(groupId, userId);
        return Result.success("已退出群组");
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "解散群组")
    public Result<String> dissolveGroup(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long groupId) {
        groupService.dissolveGroup(groupId, userId);
        return Result.success("群组已解散");
    }

    @PutMapping("/{groupId}/announcement")
    @Operation(summary = "更新群公告")
    public Result<String> updateAnnouncement(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long groupId,
            @RequestBody UpdateAnnouncementRequest request) {
        groupService.updateGroupAnnouncement(groupId, userId, request.getAnnouncement());
        return Result.success("群公告已更新");
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的群组列表")
    public Result<List<GroupVO>> getMyGroups(@RequestAttribute("userId") Long userId) {
        List<GroupVO> groups = groupService.getUserGroups(userId);
        return Result.success(groups);
    }
}
```

- [ ] **Step 5: Create UpdateAnnouncementRequest**

```java
package org.linxin.server.business.model.request;

import lombok.Data;

@Data
public class UpdateAnnouncementRequest {
    private String announcement;
}
```

File path: `linxin-server/src/main/java/org/linxin/server/business/model/request/UpdateAnnouncementRequest.java`

- [ ] **Step 6: Verify compilation**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add linxin-server/src/main/java/org/linxin/server/business/controller/
git add linxin-server/src/main/java/org/linxin/server/business/model/request/UpdateAnnouncementRequest.java
git commit -m "fix: replace X-User-Id header with request attribute across all controllers; fix register response; add UpdateAnnouncementRequest"
```

---

## Group 2 — WebSocket Module

### Task 5: Fix WebSocketConfig — configurable allowed origins

**Files:**
- Modify: `linxin-server/src/main/java/org/linxin/server/websocket/WebSocketConfig.java`
- Modify: `linxin-server/src/main/resources/application.yml`
- Modify: `linxin-server/src/main/resources/application-dev.yml`
- Modify: `linxin-server/src/main/resources/application-pro.yml`

- [ ] **Step 1: Add websocket.allowed-origins to application.yml**

Add at the end of `application.yml`:
```yaml
websocket:
  allowed-origins: ""
```

- [ ] **Step 2: Add allowed-origins to application-dev.yml**

Add at the end of `application-dev.yml`:
```yaml
websocket:
  allowed-origins: "http://localhost:*,http://127.0.0.1:*"
```

- [ ] **Step 3: Add allowed-origins to application-pro.yml**

Current `application-pro.yml` content — add:
```yaml
websocket:
  allowed-origins: "https://yourdomain.com"
```

- [ ] **Step 4: Fix WebSocketConfig to read from config**

```java
package org.linxin.server.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.linxin.server.auth.JwtService;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final JwtService jwtService;

    @Value("${websocket.allowed-origins:http://localhost:*}")
    private String allowedOrigins;

    public WebSocketConfig(WebSocketHandler webSocketHandler, JwtService jwtService) {
        this.webSocketHandler = webSocketHandler;
        this.jwtService = jwtService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addHandler(webSocketHandler, "/ws")
                .setAllowedOrigins(origins)
                .addInterceptors(new WebSocketInterceptor(jwtService));
    }
}
```

- [ ] **Step 5: Verify compilation**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add linxin-server/src/main/java/org/linxin/server/websocket/WebSocketConfig.java
git add linxin-server/src/main/resources/application.yml
git add linxin-server/src/main/resources/application-dev.yml
git add linxin-server/src/main/resources/application-pro.yml
git commit -m "fix: replace WebSocket wildcard CORS with configurable allowed-origins per environment"
```

---

### Task 6: Fix WebSocketHandler — bounded queue with message expiry

**Files:**
- Modify: `linxin-server/src/main/java/org/linxin/server/websocket/WebSocketHandler.java`

- [ ] **Step 1: Rewrite WebSocketHandler with PendingMessage wrapper and 200-message/72h limits**

```java
package org.linxin.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_QUEUE_SIZE = 200;
    private static final long EXPIRE_HOURS = 72;

    private static final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<PendingMessage>> pendingMessages = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public WebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Wraps a TextMessage with the time it was enqueued. */
    private static class PendingMessage {
        final TextMessage message;
        final Instant enqueuedAt;

        PendingMessage(TextMessage message) {
            this.message = message;
            this.enqueuedAt = Instant.now();
        }

        boolean isExpired() {
            return Duration.between(enqueuedAt, Instant.now()).toHours() >= EXPIRE_HOURS;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.put(userId, session);
            sessionLocks.put(userId, new ReentrantLock());
            pendingMessages.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
            sendPendingMessages(userId, session);
            log.info("WebSocket connection established for user: {}", userId);
        }
    }

    private void sendPendingMessages(Long userId, WebSocketSession session) {
        ConcurrentLinkedQueue<PendingMessage> queue = pendingMessages.get(userId);
        if (queue == null || queue.isEmpty()) return;

        ReentrantLock lock = getSessionLock(userId);
        lock.lock();
        try {
            PendingMessage pending;
            while ((pending = queue.poll()) != null) {
                if (pending.isExpired()) {
                    log.debug("Discarding expired pending message for user: {}", userId);
                    continue;
                }
                if (!session.isOpen()) {
                    queue.offer(pending);
                    break;
                }
                try {
                    session.sendMessage(pending.message);
                    log.debug("Sent pending message to user: {}", userId);
                } catch (IOException e) {
                    log.error("Error sending pending message to user {}", userId, e);
                    queue.offer(pending);
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received message: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId);
            sessionLocks.remove(userId);
            // Clean up expired messages so memory doesn't accumulate for long-absent users
            ConcurrentLinkedQueue<PendingMessage> queue = pendingMessages.get(userId);
            if (queue != null) {
                queue.removeIf(PendingMessage::isExpired);
            }
            log.info("WebSocket connection closed for user: {}, status: {}", userId, status);
        }
    }

    private ReentrantLock getSessionLock(Long userId) {
        return sessionLocks.computeIfAbsent(userId, k -> new ReentrantLock());
    }

    public void sendMessageToUser(Long userId, Object message) {
        ConcurrentLinkedQueue<PendingMessage> messageQueue =
                pendingMessages.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        WebSocketSession session = sessions.get(userId);

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);
            PendingMessage pendingMessage = new PendingMessage(textMessage);

            ReentrantLock lock = getSessionLock(userId);
            lock.lock();
            try {
                if (session != null && session.isOpen()) {
                    session.sendMessage(textMessage);
                    log.debug("Sent message to user: {}", userId);
                } else {
                    // Enforce queue size limit: drop oldest if full
                    while (messageQueue.size() >= MAX_QUEUE_SIZE) {
                        messageQueue.poll();
                        log.warn("Queue full for user {}, dropped oldest message", userId);
                    }
                    messageQueue.offer(pendingMessage);
                    log.debug("Message queued for offline user: {}", userId);
                }
            } finally {
                lock.unlock();
            }
        } catch (IOException e) {
            log.error("Error sending message to user {}", userId, e);
        }
    }

    public void broadcastMessage(Object message) {
        for (Long userId : sessions.keySet()) {
            sendMessageToUser(userId, message);
        }
    }

    public int getOnlineUserCount() {
        return sessions.size();
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add linxin-server/src/main/java/org/linxin/server/websocket/WebSocketHandler.java
git commit -m "fix: add 200-message queue limit and 72h expiry to WebSocket offline message queue"
```

---

## Group 3 — Business Logic Module

### Task 7: Fix GroupServiceImpl — permission check logic

**Files:**
- Modify: `linxin-server/src/main/java/org/linxin/server/business/service/impl/GroupServiceImpl.java`

The bug: `operator.getRole() < ROLE_ADMIN` where `ROLE_ADMIN=1, ROLE_OWNER=2`.
- Admin (role=1): `1 < 1` → false → throws exception → admin cannot manage ❌
- Owner (role=2): `2 < 1` → false → throws exception → owner cannot manage ❌
- Member (role=0): `0 < 1` → true → throws exception → correct ✓

Fix: the condition should reject only regular members (role == 0), i.e. `operator.getRole() == ROLE_MEMBER`.

- [ ] **Step 1: Replace all three occurrences of the broken permission check**

There are three `if (operator == null || operator.getRole() < ROLE_ADMIN)` checks in `addMembers`, `removeMember`, and `updateGroupAnnouncement`. Replace all three with `operator.getRole() == ROLE_MEMBER`.

In `addMembers` (line 125):
```java
        if (operator == null || operator.getRole() == ROLE_MEMBER) {
            throw new BusinessException("无权限操作");
        }
```

In `removeMember` (line 145):
```java
        if (operator == null || operator.getRole() == ROLE_MEMBER) {
            throw new BusinessException("无权限操作");
        }
```

In `updateGroupAnnouncement` (line 250):
```java
        if (operator == null || operator.getRole() == ROLE_MEMBER) {
            throw new BusinessException("无权限操作");
        }
```

- [ ] **Step 2: Verify compilation**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add linxin-server/src/main/java/org/linxin/server/business/service/impl/GroupServiceImpl.java
git commit -m "fix: correct group permission check — allow admin(1) and owner(2), reject member(0) only"
```

---

### Task 8: Fix ChatServiceImpl — auto-create missing group conversations + atomic unread count

**Files:**
- Modify: `linxin-server/src/main/java/org/linxin/server/business/service/impl/ChatServiceImpl.java`

**Bug 1:** In `sendGroupMessage`, when a group member has no conversation record, the loop does `continue`, silently skipping the unread count update for that member.

**Bug 2:** In `updateReceiverConversation`, `conversation.getUnreadCount() + 1` is a read-modify-write in application memory, vulnerable to concurrent updates. Fix with a single `UPDATE ... SET unread_count = unread_count + 1`.

- [ ] **Step 1: Fix sendGroupMessage — auto-create missing conversation then update**

Replace the loop body in `sendGroupMessage` (the for loop starting at line 315):

```java
        for (GroupMember member : members) {
            Conversation conversation = convMap.get(member.getUserId());
            if (conversation == null) {
                // Auto-create missing conversation so no member is silently skipped
                createGroupConversation(member.getUserId(), groupId, group.getName());
                LambdaQueryWrapper<Conversation> newConvWrapper = new LambdaQueryWrapper<>();
                newConvWrapper.eq(Conversation::getUserId, member.getUserId())
                        .eq(Conversation::getGroupId, groupId)
                        .eq(Conversation::getDeleted, 0);
                conversation = conversationMapper.selectOne(newConvWrapper);
                if (conversation == null) continue;
            }

            conversation.setLastMessageId(message.getId());
            conversation.setLastMessageContent(truncateContent(request.getContent(), 50));
            conversation.setLastMessageType(request.getMessageType());
            conversation.setLastMessageTime(sendTime);
            conversationMapper.updateById(conversation);

            // Atomic unread count increment — skip for sender, skip if muted
            if (!member.getUserId().equals(senderId)) {
                conversationMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<org.linxin.server.business.entity.Conversation>()
                                .setSql("unread_count = unread_count + 1")
                                .eq(org.linxin.server.business.entity.Conversation::getId, conversation.getId())
                                .eq(org.linxin.server.business.entity.Conversation::getMuteStatus, 0));
            }
        }
```

- [ ] **Step 2: Fix updateReceiverConversation — use atomic SQL increment**

Replace the `updateReceiverConversation` method:

```java
    private void updateReceiverConversation(Conversation conversation, Message message, Long senderId) {
        User sender = userMapper.selectById(senderId);
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageContent(truncateContent(message.getContent(), 50));
        conversation.setLastMessageType(message.getMessageType());
        conversation.setLastMessageTime(message.getSendTime());
        if (sender != null) {
            conversation.setPeerNickname(sender.getNickname());
            conversation.setPeerAvatar(sender.getAvatar());
        }
        conversationMapper.updateById(conversation);

        // Atomic unread count increment to avoid race conditions
        conversationMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Conversation>()
                        .setSql("unread_count = unread_count + 1")
                        .eq(Conversation::getId, conversation.getId())
                        .eq(Conversation::getMuteStatus, 0));
    }
```

- [ ] **Step 3: Verify compilation**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add linxin-server/src/main/java/org/linxin/server/business/service/impl/ChatServiceImpl.java
git commit -m "fix: auto-create missing group conversations; use atomic SQL for unread count increment"
```

---

### Task 9: Fix AIServiceImpl — remove duplicate tool definitions

**Files:**
- Modify: `linxin-server/src/main/java/org/linxin/server/ai/service/impl/AIServiceImpl.java`
- Modify: `linxin-server/src/main/resources/tools/default_tools.json`

**Problem:** `loadTools()` falls back to `getDefaultTools()` (3 tools hardcoded in Java) on JSON parse failure, while `default_tools.json` has 5 tools. Two tools (`searchMessages`, `setReminder`) are defined in JSON but have no executor on the client side.

**Fix:** Remove the `getDefaultTools()` fallback — on JSON load failure, keep `tools` empty and log the error clearly. Add `"implemented"` flag to JSON so client-side code can skip unimplemented tools.

- [ ] **Step 1: Update default_tools.json — add implemented flag**

```json
{
  "version": "1.0.0",
  "tools": [
    {
      "id": "send_message",
      "name": "sendMessage",
      "description": "发送消息给指定用户",
      "icon": "message",
      "requireConfirm": true,
      "implemented": true,
      "params": [
        {
          "name": "receiverId",
          "type": "string",
          "description": "接收者用户ID",
          "required": true
        },
        {
          "name": "content",
          "type": "string",
          "description": "消息内容",
          "required": true,
          "maxLength": 2000
        }
      ]
    },
    {
      "id": "create_group",
      "name": "createGroup",
      "description": "创建群聊并添加成员",
      "icon": "group_add",
      "requireConfirm": true,
      "implemented": true,
      "params": [
        {
          "name": "groupName",
          "type": "string",
          "description": "群名称",
          "required": true,
          "maxLength": 50
        },
        {
          "name": "memberIds",
          "type": "array",
          "description": "成员ID列表",
          "required": true,
          "itemsType": "string"
        }
      ]
    },
    {
      "id": "add_friend",
      "name": "addFriend",
      "description": "申请添加指定用户为好友",
      "icon": "person_add",
      "requireConfirm": true,
      "implemented": true,
      "params": [
        {
          "name": "userId",
          "type": "string",
          "description": "用户ID",
          "required": true
        },
        {
          "name": "remark",
          "type": "string",
          "description": "申请备注",
          "required": false,
          "maxLength": 100
        }
      ]
    },
    {
      "id": "search_messages",
      "name": "searchMessages",
      "description": "搜索聊天记录",
      "icon": "search",
      "requireConfirm": false,
      "implemented": false,
      "params": [
        {
          "name": "keyword",
          "type": "string",
          "description": "搜索关键词",
          "required": true,
          "maxLength": 100
        },
        {
          "name": "conversationId",
          "type": "string",
          "description": "会话ID（不填则搜索所有）",
          "required": false
        }
      ]
    },
    {
      "id": "set_reminder",
      "name": "setReminder",
      "description": "设置定时提醒",
      "icon": "alarm",
      "requireConfirm": true,
      "implemented": false,
      "params": [
        {
          "name": "time",
          "type": "string",
          "description": "提醒时间，ISO8601格式",
          "required": true
        },
        {
          "name": "content",
          "type": "string",
          "description": "提醒内容",
          "required": true,
          "maxLength": 500
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Remove getDefaultTools() from AIServiceImpl and fix loadTools fallback**

Replace the `loadTools()` method and remove `getDefaultTools()`:

```java
    private void loadTools() {
        try {
            ClassPathResource resource = new ClassPathResource("tools/default_tools.json");
            String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ToolsConfig config = objectMapper.readValue(json, ToolsConfig.class);
            this.tools = config.getTools();
            this.toolsVersion = config.getVersion();
            log.info("Loaded {} AI tools, version: {}", tools.size(), toolsVersion);
        } catch (IOException e) {
            log.error("Failed to load AI tools from config file. AI tools will be unavailable.", e);
            this.tools = new ArrayList<>();
            this.toolsVersion = "error";
        }
    }
```

Also remove the entire `getDefaultTools()` private method (lines 175–200 in the original file).

- [ ] **Step 3: Verify compilation**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Full backend build check**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn package -DskipTests -q
```
Expected: BUILD SUCCESS, jar file created in `target/`

- [ ] **Step 5: Commit**

```bash
git add linxin-server/src/main/java/org/linxin/server/ai/service/impl/AIServiceImpl.java
git add linxin-server/src/main/resources/tools/default_tools.json
git commit -m "fix: remove duplicate hardcoded AI tools; add implemented flag to default_tools.json"
```

---

## Group 4 — Frontend Fixes

### Task 10: Fix DatabaseService — add clearUserData()

**Files:**
- Modify: `linxin-client/lib/services/db_service.dart`

- [ ] **Step 1: Add clearUserData() method to DatabaseService**

Add after the `rawQuery` method (before `close()`):

```dart
  Future<void> clearUserData() async {
    final db = await database;
    await db.delete('messages');
    await db.delete('conversations');
  }
```

- [ ] **Step 2: Verify the app compiles**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-client
flutter analyze lib/services/db_service.dart
```
Expected: No issues found

- [ ] **Step 3: Commit**

```bash
git add linxin-client/lib/services/db_service.dart
git commit -m "feat: add clearUserData() to DatabaseService for logout cleanup"
```

---

### Task 11: Fix auth_service.dart — async logout + SQLite clear

**Files:**
- Modify: `linxin-client/lib/services/auth_service.dart`

- [ ] **Step 1: Make logout async, await clearAuthData, and clear SQLite**

Replace the `logout()` method:

```dart
  Future<void> logout() async {
    _currentUser = null;
    _httpService.clearToken();
    WebSocketService().disconnect();
    await _clearAuthData();
    await DatabaseService().clearUserData();
    notifyListeners();
  }
```

Also add the `DatabaseService` import at the top of the file:

```dart
import 'db_service.dart';
```

- [ ] **Step 2: Verify no analysis errors**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-client
flutter analyze lib/services/auth_service.dart
```
Expected: No issues found

- [ ] **Step 3: Commit**

```bash
git add linxin-client/lib/services/auth_service.dart
git commit -m "fix: make logout async; await clearAuthData; clear SQLite on logout to prevent data leakage between accounts"
```

---

### Task 12: Fix chat_detail_page.dart — currentUserId from AuthService

**Files:**
- Modify: `linxin-client/lib/pages/chat_detail_page.dart`

**Problem:** `_currentUserId = widget.currentUser?.id.toString()` is always null because `ChatListPage` never passes `currentUser` when creating `ChatDetailPage`. The result is all messages show as received (isMe = false).

**Fix:** Read `currentUser` directly from `AuthService.instance` in `initState`.

- [ ] **Step 1: Add AuthService import and fix currentUserId initialization**

Add import at the top of `chat_detail_page.dart`:
```dart
import '../services/auth_service.dart';
```

In `initState()`, replace:
```dart
    _currentUserId = widget.currentUser?.id.toString();
```
with:
```dart
    _currentUserId = AuthService().currentUser?.id?.toString();
```

- [ ] **Step 2: Verify no analysis errors**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-client
flutter analyze lib/pages/chat_detail_page.dart
```
Expected: No issues found

- [ ] **Step 3: Commit**

```bash
git add linxin-client/lib/pages/chat_detail_page.dart
git commit -m "fix: read currentUserId from AuthService instead of null widget parameter"
```

---

### Task 13: Fix ai_chat_page.dart — loading index race + TextEditingController leak

**Files:**
- Modify: `linxin-client/lib/pages/ai_chat_page.dart`

**Bug 1:** `_loadingMessageId = '${_messages.length - 1}'` is set inside `setState`, then used after an `await`. If new messages arrive during the `await`, the index is wrong.

**Fix:** Capture the index as a local `int` variable before the `await` gap (outside setState).

**Bug 2:** `_showModifyDialog()` creates a `TextEditingController` that is never disposed.

**Fix:** Dispose in a `finally` block.

- [ ] **Step 1: Fix _executePendingActions loading index race condition**

Replace the `_executePendingActions` method:

```dart
  Future<void> _executePendingActions() async {
    if (_pendingResponse == null) return;

    setState(() {
      _showConfirmDialog = false;
    });

    for (final toolCall in _pendingResponse!.toolCalls) {
      // Add loading message and capture its index BEFORE the await gap
      setState(() {
        _messages.add(AIChatMessage(
          content: '正在执行: ${toolCall.description}...',
          isUser: false,
          isLoading: true,
        ));
      });
      final int loadingIndex = _messages.length - 1;

      final result = await AIIntentService.instance.executeToolCall(toolCall);

      setState(() {
        if (loadingIndex < _messages.length) {
          _messages[loadingIndex] = AIChatMessage(
            content: result.message,
            isUser: false,
            isError: !result.success,
          );
        }
      });
    }

    setState(() {
      _pendingResponse = null;
    });
  }
```

- [ ] **Step 2: Fix _showModifyDialog TextEditingController leak**

Replace the `_showModifyDialog` method:

```dart
  Future<String?> _showModifyDialog() async {
    final controller = TextEditingController();
    try {
      return await showDialog<String>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('修改操作'),
          content: TextField(
            controller: controller,
            decoration: const InputDecoration(
              hintText: '请输入您的修改意见...',
              border: OutlineInputBorder(),
            ),
            maxLines: 3,
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('取消'),
            ),
            TextButton(
              onPressed: () => Navigator.pop(context, controller.text),
              child: const Text('确认修改'),
            ),
          ],
        ),
      );
    } finally {
      controller.dispose();
    }
  }
```

- [ ] **Step 3: Verify no analysis errors**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-client
flutter analyze lib/pages/ai_chat_page.dart
```
Expected: No issues found

- [ ] **Step 4: Commit**

```bash
git add linxin-client/lib/pages/ai_chat_page.dart
git commit -m "fix: capture AI loading message index before await gap; dispose TextEditingController in finally block"
```

---

### Task 14: Fix chat_list_page.dart — incremental conversation update

**Files:**
- Modify: `linxin-client/lib/pages/chat_list_page.dart`

**Problem:** Every WebSocket message (new_message, group_message, read_status) triggers `_loadChats()` which replaces the entire `_chats` list. This causes full list rebuilds on every incoming message.

**Fix:** On `new_message` and `group_message`, find the matching chat by `conversationId` and update only its `lastMessage` + `unreadCount` + `lastTime`. On `read_status`, reset unread count for the affected conversation. Fall back to `_loadChats()` only when the conversation isn't found locally (new conversation).

- [ ] **Step 1: Replace _handleWebSocketMessage and _handleGroupWebSocketMessage**

Replace both handler methods in `_ChatListPageState`:

```dart
  void _handleWebSocketMessage(dynamic data) {
    if (data['type'] == 'new_message') {
      _updateChatFromMessage(data['data']);
    } else if (data['type'] == 'read_status') {
      final conversationId = data['data']?['conversationId']?.toString();
      if (conversationId != null) {
        setState(() {
          final index = _chats.indexWhere((c) => c.id == conversationId);
          if (index != -1) {
            _chats[index] = _chats[index].copyWith(unreadCount: 0);
          }
        });
      }
    }
  }

  void _handleGroupWebSocketMessage(dynamic data) {
    if (data['type'] == 'group_message') {
      _updateChatFromMessage(data['data']);
    }
  }

  void _updateChatFromMessage(dynamic messageData) {
    if (messageData == null) return;
    final conversationId = messageData['conversationId']?.toString();
    if (conversationId == null) return;

    final index = _chats.indexWhere((c) => c.id == conversationId);
    if (index == -1) {
      // Unknown conversation — do a full reload to pick it up
      _loadChats();
      return;
    }

    final content = messageData['content']?.toString() ?? '';
    setState(() {
      _chats[index] = _chats[index].copyWith(
        lastMessage: content,
        lastTime: DateTime.now(),
        unreadCount: _chats[index].unreadCount + 1,
      );
    });
  }
```

- [ ] **Step 2: Verify no analysis errors**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-client
flutter analyze lib/pages/chat_list_page.dart
```
Expected: No issues found

- [ ] **Step 3: Full project analysis**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-client
flutter analyze
```
Expected: No issues found (or only pre-existing warnings)

- [ ] **Step 4: Commit**

```bash
git add linxin-client/lib/pages/chat_list_page.dart
git commit -m "fix: incremental chat list update on WebSocket messages instead of full reload"
```

---

## Final Verification

- [ ] **Backend: full build**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-server
mvn package -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Frontend: full analysis**

```bash
cd /Users/duzhihua/software/im/linxin/linxin-client
flutter analyze
```
Expected: No new issues

- [ ] **Confirm all 14 tasks committed**

```bash
cd /Users/duzhihua/software/im/linxin
git log --oneline -20
```
Expected: 14 fix commits visible since the design doc commit
