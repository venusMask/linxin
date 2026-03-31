# 后端功能文档

## 核心功能模块

### 1. 认证模块 (/auth)

#### 1.1 用户注册
- **接口**: `POST /auth/register`
- **功能**: 用户注册新账号
- **参数**:
  - username: 用户名
  - password: 密码
  - nickname: 昵称
  - phone: 手机号（可选）
  - email: 邮箱（可选）
- **流程**:
  1. 校验用户名是否已存在
  2. 密码BCrypt加密
  3. 创建用户记录
  4. 返回用户信息

#### 1.2 用户登录
- **接口**: `POST /auth/login`
- **功能**: 用户登录获取Token
- **参数**:
  - username: 用户名
  - password: 密码
- **流程**:
  1. 验证用户名密码
  2. 生成JWT Token
  3. 更新最后登录时间和IP
  4. 返回Token和用户信息

#### 1.3 获取用户信息
- **接口**: `GET /auth/userinfo`
- **功能**: 获取当前登录用户信息
- **认证**: 需要JWT Token
- **返回**: 用户详细信息

#### 1.4 搜索用户
- **接口**: `GET /auth/search`
- **功能**: 根据关键词搜索用户
- **参数**: keyword - 搜索关键词
- **返回**: 匹配的用户列表

#### 1.5 获取指定用户信息
- **接口**: `GET /auth/users/{userId}`
- **功能**: 获取指定用户的基本信息
- **参数**: userId - 用户ID
- **返回**: 用户基本信息

---

### 2. 好友模块 (/friends)

#### 2.1 获取好友列表
- **接口**: `POST /friends/list`
- **功能**: 分页获取好友列表
- **参数**:
  - username: 用户名（可选，用于搜索）
  - pageNum: 页码
  - pageSize: 每页数量
- **返回**: 好友列表（分页）

#### 2.2 申请添加好友
- **接口**: `POST /friends/apply`
- **功能**: 发送好友申请
- **参数**:
  - toUserId: 被申请人ID
  - remark: 申请备注
- **流程**:
  1. 检查是否已经是好友
  2. 检查是否已有待处理的申请
  3. 创建好友申请记录
  4. 通过WebSocket通知被申请人

#### 2.3 获取收到的好友申请
- **接口**: `GET /friends/apply/received`
- **功能**: 获取当前用户收到的好友申请列表
- **返回**: 好友申请列表

#### 2.4 获取发出的好友申请
- **接口**: `GET /friends/apply/sent`
- **功能**: 获取当前用户发出的好友申请列表
- **返回**: 好友申请列表

#### 2.5 处理好友申请
- **接口**: `POST /friends/apply/handle`
- **功能**: 同意或拒绝好友申请
- **参数**:
  - applyId: 申请ID
  - status: 处理结果（1-同意，2-拒绝）
- **流程**:
  1. 更新申请状态
  2. 如果同意，创建双向好友关系
  3. 通过WebSocket通知申请人

#### 2.6 更新好友信息
- **接口**: `PUT /friends/update`
- **功能**: 更新好友备注或分组
- **参数**:
  - friendId: 好友ID
  - friendNickname: 备注昵称
  - friendGroup: 分组名称

#### 2.7 删除好友
- **接口**: `DELETE /friends/{friendId}`
- **功能**: 删除好友关系
- **参数**: friendId - 好友ID
- **流程**:
  1. 删除双向好友关系
  2. 逻辑删除记录

#### 2.8 检查好友关系
- **接口**: `GET /friends/check/{friendId}`
- **功能**: 检查是否是好友关系
- **参数**: friendId - 好友ID
- **返回**: true/false

---

### 3. 聊天模块 (/chat)

#### 3.1 获取会话列表
- **接口**: `GET /chat/conversations`
- **功能**: 分页获取用户的会话列表
- **参数**:
  - pageNum: 页码
  - pageSize: 每页数量
- **返回**: 会话列表（按最后消息时间排序）

#### 3.2 获取或创建会话
- **接口**: `GET /chat/conversations/{peerId}`
- **功能**: 获取或创建与指定用户的会话
- **参数**: peerId - 对方用户ID
- **流程**:
  1. 查找是否已有会话
  2. 如果没有，创建新会话
  3. 返回会话信息

#### 3.3 发送消息
- **接口**: `POST /chat/messages`
- **功能**: 发送消息（私聊或群聊）
- **参数**:
  - conversationId: 会话ID
  - receiverId: 接收者ID
  - content: 消息内容
  - messageType: 消息类型（1-文本，2-图片，3-语音，4-视频，5-文件）
  - conversationType: 会话类型（0-私聊，1-群聊）
- **流程**:
  1. 创建消息记录
  2. 更新会话最后消息
  3. 通过WebSocket推送给接收者
  4. 返回消息信息

#### 3.4 获取消息列表
- **接口**: `GET /chat/messages/{conversationId}`
- **功能**: 分页获取会话的消息列表
- **参数**:
  - conversationId: 会话ID
  - pageNum: 页码
  - pageSize: 每页数量
- **返回**: 消息列表（按发送时间排序）

#### 3.5 标记消息已读
- **接口**: `POST /chat/messages/{conversationId}/read`
- **功能**: 标记会话中的消息为已读
- **参数**: conversationId - 会话ID
- **流程**:
  1. 更新消息状态表
  2. 清零会话未读数
  3. 通知发送者消息已读

#### 3.6 切换置顶状态
- **接口**: `POST /chat/conversations/{conversationId}/top`
- **功能**: 切换会话的置顶状态
- **参数**: conversationId - 会话ID

#### 3.7 切换静音状态
- **接口**: `POST /chat/conversations/{conversationId}/mute`
- **功能**: 切换会话的免打扰状态
- **参数**: conversationId - 会话ID

#### 3.8 获取群消息列表
- **接口**: `GET /chat/group/messages`
- **功能**: 分页获取群聊消息
- **参数**:
  - groupId: 群组ID
  - pageNum: 页码
  - pageSize: 每页数量
- **返回**: 群消息列表

---

### 4. 群组模块 (/group)

#### 4.1 创建群组
- **接口**: `POST /group/create`
- **功能**: 创建新群组
- **参数**:
  - name: 群名称
  - memberIds: 初始成员ID列表
- **流程**:
  1. 创建群组记录
  2. 创建群主成员记录（角色=2）
  3. 创建其他成员记录（角色=0）
  4. 更新群成员数
  5. 返回群组信息

#### 4.2 获取群组信息
- **接口**: `GET /group/{groupId}`
- **功能**: 获取群组基本信息
- **参数**: groupId - 群组ID
- **返回**: 群组详细信息

#### 4.3 获取群成员列表
- **接口**: `GET /group/{groupId}/members`
- **功能**: 获取群组的所有成员
- **参数**: groupId - 群组ID
- **返回**: 成员列表（包含角色信息）

#### 4.4 添加群成员
- **接口**: `POST /group/{groupId}/members/add`
- **功能**: 添加新成员到群组
- **参数**:
  - groupId: 群组ID
  - memberIds: 新成员ID列表
- **流程**:
  1. 检查操作者权限（群主或管理员）
  2. 检查群成员数是否达到上限
  3. 创建成员记录
  4. 更新群成员数
  5. 通过WebSocket通知所有成员

#### 4.5 移除群成员
- **接口**: `DELETE /group/{groupId}/members/{memberId}`
- **功能**: 移除群成员
- **参数**:
  - groupId: 群组ID
  - memberId: 成员ID
- **流程**:
  1. 检查操作者权限
  2. 逻辑删除成员记录
  3. 更新群成员数
  4. 通过WebSocket通知所有成员

#### 4.6 退出群组
- **接口**: `POST /group/{groupId}/leave`
- **功能**: 用户主动退出群组
- **参数**: groupId - 群组ID
- **流程**:
  1. 检查是否是群主（群主不能退出，只能解散）
  2. 逻辑删除成员记录
  3. 更新群成员数
  4. 通过WebSocket通知所有成员

#### 4.7 解散群组
- **接口**: `DELETE /group/{groupId}`
- **功能**: 群主解散群组
- **参数**: groupId - 群组ID
- **流程**:
  1. 检查是否是群主
  2. 逻辑删除群组记录
  3. 逻辑删除所有成员记录
  4. 通过WebSocket通知所有成员

#### 4.8 更新群公告
- **接口**: `PUT /group/{groupId}/announcement`
- **功能**: 更新群公告
- **参数**:
  - groupId: 群组ID
  - announcement: 群公告内容
- **权限**: 群主或管理员

#### 4.9 获取我的群组列表
- **接口**: `GET /group/my`
- **功能**: 获取当前用户加入的所有群组
- **返回**: 群组列表

---

### 5. AI模块 (/ai)

#### 5.1 解析用户输入
- **接口**: `POST /ai/chat`
- **功能**: 解析用户自然语言输入，返回意图和参数
- **参数**:
  - userInput: 用户输入
  - conversationId: 会话ID（可选）
  - context: 上下文信息（可选）
- **流程**:
  1. 调用OpenAI适配器
  2. 传入tools定义和用户输入
  3. AI解析返回tool_calls
  4. 返回解析结果（不执行操作）
- **返回**:
  - intent: 操作意图
  - toolCalls: 待执行的工具列表
  - aiText: AI回复文本
  - needConfirm: 是否需要用户确认

#### 5.2 修改操作参数
- **接口**: `POST /ai/modify`
- **功能**: 根据用户反馈修改操作参数
- **参数**:
  - originalResponse: 原始AI响应
  - modification: 修改指令
- **流程**:
  1. 调用AI服务重新解析
  2. 返回修改后的参数

#### 5.3 获取可用工具列表
- **接口**: `GET /ai/tools`
- **功能**: 获取所有可用的AI工具定义
- **返回**: 工具列表

#### 5.4 获取工具版本
- **接口**: `GET /ai/tools/version`
- **功能**: 获取当前工具配置的版本号
- **返回**: 版本号字符串

---

## WebSocket功能

### 连接流程
1. 客户端连接 `/ws` 端点
2. 携带JWT Token进行认证
3. 认证成功建立WebSocket连接
4. 绑定用户ID和Session

### 消息类型
1. **聊天消息推送**: 实时推送新消息
2. **好友申请通知**: 实时推送好友申请
3. **群组变动通知**: 实时推送群成员变动
4. **消息已读通知**: 实时推送已读状态

### 心跳机制
- 客户端每30秒发送ping
- 服务端60秒无心跳断开连接

---

## 消息状态管理

### 消息状态值
| 状态值 | 含义 |
|--------|------|
| 0 | 发送中 |
| 1 | 已发送 |
| 2 | 已送达 |
| 3 | 已读 |
| -1 | 发送失败 |

### 状态流转
```
发送中(0) → 已发送(1) → 已送达(2) → 已读(3)
    ↓
发送失败(-1)
```

---

## 数据校验

### 参数校验
- 使用Jakarta Validation
- 注解校验（@NotNull, @NotBlank, @Size等）
- 自定义校验器

### 业务校验
- 用户名唯一性
- 好友关系检查
- 群成员数限制
- 权限检查

---

## 异常处理

### 异常类型
1. **BusinessException**: 业务异常
2. **参数校验异常**: ValidationException
3. **认证异常**: AuthenticationException
4. **权限异常**: AccessDeniedException

### 统一响应格式
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 错误码定义
- 200: 成功
- 400: 参数错误
- 401: 未认证
- 403: 无权限
- 404: 资源不存在
- 500: 服务器错误

---

## 性能优化

### 数据库优化
- 索引优化
- 分页查询
- 连接池配置

### 缓存策略
- 会话信息缓存
- 用户信息缓存
- 群组信息缓存

### 异步处理
- 消息推送异步化
- 通知发送异步化
- 使用@Async注解

---

## 安全措施

### 认证安全
- JWT Token认证
- Token过期机制
- 密码BCrypt加密

### 接口安全
- 所有接口需要认证（除登录注册）
- SQL注入防护
- XSS防护
- CSRF防护

### 数据安全
- 敏感信息脱敏
- 逻辑删除
- 操作日志记录

---

## 版本历史

### v1.0.1 (2026-03-31)

#### 🐛 BUG修复

##### 1. WebSocket消息队列未消费（严重）
**问题描述**:
- 用户离线时消息被放入队列，但用户上线后队列中的消息不会被发送
- 导致消息丢失

**修复方案**:
- 在`WebSocketHandler.afterConnectionEstablished()`中添加`sendPendingMessages()`方法
- 用户上线时自动发送队列中的待发送消息
- 连接关闭时不再清空pendingMessages，保留离线消息

**影响文件**: `WebSocketHandler.java`

**代码示例**:
```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    Long userId = (Long) session.getAttributes().get("userId");
    if (userId != null) {
        sessions.put(userId, session);
        sessionLocks.put(userId, new ReentrantLock());
        pendingMessages.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        
        // 发送离线消息
        sendPendingMessages(userId, session);
        
        log.info("WebSocket connection established for user: {}", userId);
    }
}
```

---

##### 2. 群消息接收者ID未设置（严重）
**问题描述**:
- 群消息记录中`receiver_id`为NULL
- 可能导致消息查询逻辑异常
- 数据不完整

**修复方案**:
- 在创建群消息时设置`message.setReceiverId(0L)`
- 0L表示这是一个群消息，保证数据完整性

**影响文件**: `ChatServiceImpl.java`

**代码示例**:
```java
Message message = new Message();
message.setSenderId(senderId);
message.setReceiverId(0L);  // 群消息设置特殊值
message.setGroupId(groupId);
```

---

##### 3. 好友关系物理删除（严重）
**问题描述**:
- 删除好友使用物理删除`friendMapper.delete()`
- 与数据库设计中的逻辑删除不一致
- 可能导致数据恢复困难

**修复方案**:
- 改为逻辑删除，设置`deleted=1`
- 保持与其他表的逻辑删除策略一致

**影响文件**: `FriendServiceImpl.java`

**代码示例**:
```java
public void deleteFriend(Long userId, Long friendId) {
    LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId);
    Friend friend1 = friendMapper.selectOne(wrapper);
    if (friend1 != null) {
        friend1.setDeleted(1);
        friendMapper.updateById(friend1);
    }
    // ... 双向删除
}
```

---

##### 4. 会话未读数未考虑静音状态（中等）
**问题描述**:
- 静音会话仍会增加未读数
- 与用户预期不符

**修复方案**:
- 在更新未读数前检查会话的静音状态
- 只有非静音会话才增加未读数

**影响文件**: `ChatServiceImpl.java`

**代码示例**:
```java
// 私聊消息
if (conversation.getMuteStatus() == 0) {
    conversation.setUnreadCount(conversation.getUnreadCount() + 1);
}

// 群聊消息
if (!member.getUserId().equals(senderId) && conversation.getMuteStatus() == 0) {
    conversation.setUnreadCount(conversation.getUnreadCount() + 1);
}
```

---

##### 5. AI服务历史记录内存泄漏（轻微）
**问题描述**:
- 用户历史记录永久保存在内存中
- 长时间运行会导致内存溢出

**修复方案**:
- 添加`userLastActiveTime`跟踪用户最后活动时间
- 添加`@Scheduled`定时任务，每小时清理过期历史记录（24小时）
- 添加`@PreDestroy`销毁方法，应用关闭时清空所有数据

**影响文件**: `AIServiceImpl.java`

**代码示例**:
```java
@Scheduled(fixedRate = 3600000)
public void cleanupExpiredHistory() {
    LocalDateTime expireTime = LocalDateTime.now().minus(HISTORY_EXPIRE_HOURS, ChronoUnit.HOURS);
    
    Iterator<Map.Entry<Long, LocalDateTime>> iterator = userLastActiveTime.entrySet().iterator();
    while (iterator.hasNext()) {
        Map.Entry<Long, LocalDateTime> entry = iterator.next();
        if (entry.getValue().isBefore(expireTime)) {
            Long userId = entry.getKey();
            iterator.remove();
            userConversationHistory.remove(userId);
            log.debug("Cleaned up expired history for user: {}", userId);
        }
    }
}
```

---

##### 6. JWT Token过期时间单位不明确（轻微）
**问题描述**:
- `getExpire()`返回值单位不明确
- 配置文件中可能误解

**修复方案**:
- 在`AuthConfig`中添加`getExpireInMillis()`方法
- 在`JwtService`中使用新方法，代码更清晰

**影响文件**: `AuthConfig.java`, `JwtService.java`

**代码示例**:
```java
// AuthConfig.java
public long getExpireInMillis() {
    return (long) expire * 60 * 1000;  // 明确单位：分钟
}

// JwtService.java
.expiration(new Date(System.currentTimeMillis() + authConfig.getExpireInMillis()))
```

---

#### 📝 改进说明

1. **数据完整性**: 群消息现在有完整的接收者信息
2. **数据一致性**: 好友删除使用逻辑删除，与其他表保持一致
3. **用户体验**: 静音会话不再增加未读数，符合用户预期
4. **系统稳定性**: WebSocket离线消息不会丢失
5. **内存管理**: AI服务自动清理过期历史记录，防止内存泄漏
6. **代码可读性**: JWT过期时间单位更明确

---

#### 🔧 技术细节

##### WebSocket消息队列机制
- **队列存储**: `ConcurrentLinkedQueue<TextMessage>`
- **线程安全**: 使用`ReentrantLock`保证并发安全
- **重试机制**: 发送失败时消息重新入队
- **连接恢复**: 用户上线时自动发送队列中的消息

##### 逻辑删除策略
- **字段**: `deleted` (0-未删除, 1-已删除)
- **查询条件**: 所有查询都添加`deleted=0`条件
- **删除操作**: 设置`deleted=1`而不是物理删除
- **数据恢复**: 可以通过修改`deleted`字段恢复数据

##### 静音状态判断
- **字段**: `muteStatus` (0-未静音, 1-已静音)
- **判断逻辑**: `if (conversation.getMuteStatus() == 0)`
- **应用场景**: 私聊消息、群聊消息

##### 定时清理任务
- **执行频率**: 每小时执行一次
- **过期时间**: 24小时未活动的用户
- **清理内容**: 对话历史记录、最后活动时间
- **日志记录**: 记录清理的详细信息

---

#### 📊 影响评估

| 修复项 | 影响范围 | 严重程度 | 修复状态 |
|--------|----------|----------|----------|
| WebSocket消息队列 | 所有用户 | 严重 | ✅ 已修复 |
| 群消息接收者ID | 群聊功能 | 严重 | ✅ 已修复 |
| 好友关系删除 | 好友功能 | 严重 | ✅ 已修复 |
| 会话未读数 | 所有会话 | 中等 | ✅ 已修复 |
| AI服务内存泄漏 | AI功能 | 轻微 | ✅ 已修复 |
| JWT过期时间 | 认证功能 | 轻微 | ✅ 已修复 |

---

#### 🎯 测试建议

1. **WebSocket测试**:
   - 测试用户离线后上线是否能收到离线消息
   - 测试并发发送消息的队列处理
   - 测试网络断开重连后的消息同步

2. **群消息测试**:
   - 验证群消息记录中`receiver_id`字段值
   - 测试群消息查询功能

3. **好友删除测试**:
   - 验证删除好友后记录是否还存在
   - 测试好友列表是否正确显示

4. **静音会话测试**:
   - 测试静音会话收到消息后未读数是否增加
   - 测试非静音会话未读数是否正常

5. **AI服务测试**:
   - 验证长时间运行后内存是否稳定
   - 测试定时清理任务是否正常执行

6. **JWT测试**:
   - 验证Token过期时间是否正确
   - 测试Token过期后的处理逻辑

---

### v1.0.2 (2026-03-31)

#### 🐛 BUG修复

##### 1. WebSocket离线消息丢失（严重）
**问题描述**:
- `sendMessageToUser()`方法在用户离线时直接返回，没有将消息加入待发送队列
- 导致离线消息永久丢失
- 即使有队列机制也无法正常工作

**修复方案**:
- 调整代码逻辑，先获取队列，再检查session状态
- 无论用户是否在线，消息都会先加入队列
- 优化日志记录，便于调试

**影响文件**: `WebSocketHandler.java`

**代码示例**:
```java
public void sendMessageToUser(Long userId, Object message) {
    ConcurrentLinkedQueue<TextMessage> messageQueue = pendingMessages.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
    WebSocketSession session = sessions.get(userId);

    try {
        String jsonMessage = objectMapper.writeValueAsString(message);
        TextMessage textMessage = new TextMessage(jsonMessage);

        ReentrantLock lock = getSessionLock(userId);
        lock.lock();
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(textMessage);
                log.debug("Sent message to user: {}", userId);
            } else {
                messageQueue.offer(textMessage);
                log.warn("Session not found or closed, message queued for user: {}", userId);
            }
        } finally {
            lock.unlock();
        }
    } catch (IOException e) {
        log.error("Error sending message to user {}", userId, e);
    }
}
```

---

##### 2. broadcastMessage双重序列化问题（中等）
**问题描述**:
- `broadcastMessage()`方法先将消息序列化为JSON，再把TextMessage对象传给sendMessageToUser
- 会导致双重序列化问题

**修复方案**:
- 直接调用sendMessageToUser传递原始对象
- 由sendMessageToUser统一处理序列化

**影响文件**: `WebSocketHandler.java`

**代码示例**:
```java
public void broadcastMessage(Object message) {
    for (Long userId : sessions.keySet()) {
        sendMessageToUser(userId, message);
    }
}
```

---

#### 📝 改进说明

1. **消息可靠性**: WebSocket离线消息现在会正确加入队列，不再丢失
2. **代码质量**: 避免了双重序列化问题，代码更清晰
3. **日志完善**: 添加了更详细的日志记录，便于问题排查

---

#### 🔧 技术细节

##### WebSocket消息发送流程
- **先获取队列**: 确保队列存在
- **再检查Session**: 根据session状态决定是发送还是入队
- **线程安全**: 使用ReentrantLock保证并发安全
- **消息重试**: 发送失败时消息重新入队

---

#### 📊 影响评估

| 修复项 | 影响范围 | 严重程度 | 修复状态 |
|--------|----------|----------|----------|
| WebSocket离线消息丢失 | 所有用户 | 严重 | ✅ 已修复 |
| broadcastMessage双重序列化 | 广播功能 | 中等 | ✅ 已修复 |

---

#### 🎯 测试建议

1. **WebSocket离线消息测试**:
   - 测试用户离线时发送消息，消息是否入队
   - 测试用户上线后是否能收到离线消息
   - 测试多个离线消息的顺序是否正确

2. **广播消息测试**:
   - 测试广播功能是否正常工作
   - 验证消息内容是否正确
