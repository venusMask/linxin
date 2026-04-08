# 消息系统严重 BUG 分析与修复方案

> 日期：2026-04-07  
> 症状：消息发送后对方无法接收；消息在数据库中存在但客户端拉取不到；每条消息在数据库中存在两条记录。

---

## 一、现象还原

| 现象 | 描述 |
|------|------|
| 接收方无法收到消息 | WebSocket 推送到接收方后消息被静默丢弃，界面不显示 |
| 客户端拉取不到消息 | 调用 `/chat/sync` 接口时消息被处理流程丢弃，但 `lastSequenceId` 已推进，导致下次拉取也拿不到 |
| 数据库每条消息有两条记录 | 发送单条私信会插入两条 `messages` 记录，各属于不同会话 |

---

## 二、根因分析

### Bug 1：写扩散（Write Fan-out）导致每条消息产生两条 DB 记录

**位置**：`ChatServiceImpl.java:195-205`

```java
Conversation senderConversation = getOrCreateConversation(senderId, request.getReceiverId());
Conversation receiverConversation = getOrCreateConversation(request.getReceiverId(), senderId);

Message message = createMessageEntity(senderConversation.getId(), senderId, ...);
messageMapper.insert(message);   // 第一条：写入发送方会话

Message receiverMessage = createMessageEntity(receiverConversation.getId(), senderId, ...);
messageMapper.insert(receiverMessage);  // 第二条：写入接收方会话
```

`createMessageEntity`（`ChatServiceImpl.java:354`）每次调用都会调用 `messageService.getNextSequenceId()`，使用雪花算法生成新 ID，因此：

- **Message 1**：`conversation_id = alice_bob_conv.id`，`sequence_id = S1`（较小）
- **Message 2**：`conversation_id = bob_alice_conv.id`，`sequence_id = S2`（较大，S2 > S1）

两条记录内容相同，但 `conversation_id` 和 `sequence_id` 不同。这是写扩散架构的设计意图，但与后续同步逻辑存在严重不兼容（见 Bug 3、Bug 4）。

---

### Bug 2（核心）：`getFriendById` 使用错误的字段，导致消息被静默丢弃

**位置**：`data_service.dart:95`

**问题代码**：
```dart
// peerId 是发送方的「用户 ID」（Snowflake 大整数，如 "1234567890123456"）
final peerId = isMe ? messageData['receiverId']?.toString() : senderId;

// 第一次尝试：通过会话 ID 查找（正确）
var chat = getChatById(conversationId);

if (chat == null) {
    final peerId = isMe ? messageData['receiverId']?.toString() : senderId;
    
    // 第二次尝试：通过好友 userId 查找（正确）
    chat = getChatByFriendId(peerId);  // ✓ 用 friend.friendId（用户 ID）比对

    if (chat == null) {
        await refreshFriends();
        
        // 第三次尝试（回退创建）：BUG 在这里！
        final friend = getFriendById(peerId);  // ✗ 用 friend.id（关系记录 ID）比对！
        if (friend != null) {
            chat = createChat(friend, notify: notify);
        }
    }
}

if (chat != null) {
    // 保存消息...
}
// chat == null → 消息被静默丢弃，没有任何错误提示
```

**字段含义对比**：

| 字段 | 含义 | 示例值 |
|------|------|--------|
| `Friend.id` | 好友关系表记录 ID（自增小整数） | `"5"` |
| `Friend.friendId` | 好友的用户 ID（雪花 ID 大整数） | `"1234567890123456"` |

`getChatByFriendId(peerId)` 正确地用 `chat.friend.friendId`（用户 ID）查找。  
但 `getFriendById(peerId)` 错误地用 `friend.id`（关系记录 ID）查找，而 `peerId` 是用户 ID，两者永远不匹配。

**后果**：
- `refreshFriends()` 执行后，好友列表已正确加载（含 `friendId` = 用户 ID）
- 但 `getFriendById(peerId)` 始终返回 `null`（字段类型不匹配）
- `chat` 永远为 `null` → 进入 `if (chat != null)` 块被跳过
- **消息被静默丢弃，不写入本地数据库，不更新 UI**

**影响范围**：接收方在内存中没有与发送方对应的会话记录时（如冷启动、第一次收到该用户消息），所有消息均被丢弃。

---

### Bug 3：`lastSequenceId` 在消息丢弃后仍然前进，导致消息永久不可达

**位置**：`data_service.dart:293-309`

```dart
Future<void> syncMessages() async {
    final response = await HttpService().get('/chat/sync', ...);
    final List<dynamic> data = response.data ?? [];

    int maxSeqId = _lastSequenceId;
    for (var json in data) {
        final int? seqId = int.tryParse(json['sequenceId']?.toString() ?? '');
        if (seqId != null && seqId > maxSeqId) {
            maxSeqId = seqId;   // ← 无论消息是否成功处理，都先记录 seq
        }

        await _handleIncomingMessage(json, notify: false);  // ← 可能因 Bug 2 静默丢弃
    }

    await _updateLastSequenceId(maxSeqId);  // ← seq 指针已推进，消息却未保存
}
```

**后果**：
1. 服务端返回消息 M（seq = S）
2. `maxSeqId` 更新为 S
3. `_handleIncomingMessage` 因 Bug 2 静默丢弃 M
4. `_updateLastSequenceId(S)` 将 `_lastSequenceId` 设为 S
5. 下次 sync：服务端查询 `seq > S`，M 被永久跳过

这是 Bug 2 的放大器：**一次丢弃 = 永久丢失**。

---

### Bug 4：`selectMaxSequenceId` 查询逻辑与同步查询不一致

**位置**：`MessageMapper.java:11`

```java
@Select("SELECT MAX(sequence_id) FROM messages WHERE sender_id = #{userId} OR receiver_id = #{userId}")
Long selectMaxSequenceId(@Param("userId") Long userId);
```

写扩散下，发送方和接收方的副本都有相同的 `sender_id`/`receiver_id`：

| 记录 | conversation_id | sender_id | receiver_id | sequence_id |
|------|----------------|-----------|-------------|-------------|
| 发送方副本 | alice_bob_conv | alice | bob | S1 |
| 接收方副本 | bob_alice_conv | alice | bob | S2 (S2>S1) |

`selectMaxSequenceId(alice)` 因 `sender_id = alice` 会返回 `max(S1, S2) = S2`。  
但 `syncMessages` 里 Alice 的同步查询只查 `alice_bob_conv`（只含 S1），永远拿不到 S2。

结果：

```
Alice._lastSequenceId = S1（只看到发送方副本）
selectMaxSequenceId(Alice) = S2（包含接收方副本）
```

每次同步自愈检查：`S1 < S2` → 不触发重置 → 永久存在 gap。  
虽然这个 gap 本身不导致消息丢失（接收方副本在 Bob 的会话中，不在 Alice 的会话中），但导致自愈逻辑实际上从未能正确识别真正的数据异常。

---

## 三、完整调用链（失败路径）

```
Alice 发送消息给 Bob
    ↓
ChatServiceImpl.sendMessage()
    ├── 插入 Message1（alice_bob_conv, seq=S1）
    ├── 插入 Message2（bob_alice_conv, seq=S2）
    └── webSocketHandler.sendMessageToUser(bob, Message2VO)
            ↓
        Bob 的 WebSocket 收到推送
            ↓
        DataService._handleIncomingMessage(Message2VO)
            ├── getChatById("bob_alice_conv") → null（冷启动无缓存）
            ├── getChatByFriendId(alice.userId) → null（无聊天缓存）
            ├── refreshFriends() → 加载好友，Alice 在列表中
            ├── getFriendById(alice.userId)
            │   └── 查找 friend.id == alice.userId → 永远不匹配！返回 null
            └── chat == null → 消息静默丢弃 ✗

Bob 重新连接，WebSocket connect 触发 syncMessages()
    ↓
DataService.syncMessages(lastSeqId=0)
    ↓
服务端返回 [Message2（seq=S2）]
    ↓
maxSeqId = S2
_handleIncomingMessage(Message2) → 同上，静默丢弃
_updateLastSequenceId(S2) → lastSeqId = S2
    ↓
下次 syncMessages(lastSeqId=S2) → 服务端返回空 → 消息永久消失
```

---

## 四、修复方案

### Fix 1（必须）：修复 `getFriendById` 的字段匹配逻辑

**文件**：`linxin-client/lib/core/state/data_service.dart`

**改动**：新增按用户 ID 查找好友的方法，并在消息处理回退路径中使用。

```dart
// 新增方法：按好友用户 ID 查找（friendId 字段）
Friend? getFriendByUserId(String userId) {
    try {
        return _friends.firstWhere((friend) => friend.friendId == userId);
    } catch (e) {
        return null;
    }
}
```

在 `_handleIncomingMessage`（约第 93 行）中：

```dart
// 修改前
final friend = getFriendById(peerId);

// 修改后
final friend = getFriendByUserId(peerId);
```

同样，`_handleIncomingGroupMessage` 中如有类似模式，一并修复。

---

### Fix 2（必须）：修复 `lastSequenceId` 推进逻辑，与消息处理结果绑定

**文件**：`linxin-client/lib/core/state/data_service.dart`

消息处理成功后才更新 seq，而不是预先收集最大值再统一更新。

```dart
Future<void> syncMessages() async {
    try {
        final response = await HttpService().get('/chat/sync', queryParameters: {
            'lastSequenceId': _lastSequenceId,
        });

        final List<dynamic> data = response.data ?? [];
        if (data.isEmpty) return;

        int maxSuccessSeqId = _lastSequenceId;  // 只记录成功处理的最大 seq

        for (var json in data) {
            final int? conversationType = json['conversationType'] as int?;
            bool success;
            if (conversationType == 1) {
                success = await _handleIncomingGroupMessage(json, notify: false);
            } else {
                success = await _handleIncomingMessage(json, notify: false);
            }

            // 只有消息成功处理（或已存在，幂等），才推进 seq
            if (success) {
                final int? seqId = int.tryParse(json['sequenceId']?.toString() ?? '');
                if (seqId != null && seqId > maxSuccessSeqId) {
                    maxSuccessSeqId = seqId;
                }
            }
        }

        await _updateLastSequenceId(maxSuccessSeqId);
        notifyListeners();
    } catch (e) {
        debugPrint('Sync messages failed: $e');
    }
}
```

对应地，`_handleIncomingMessage` 和 `_handleIncomingGroupMessage` 需要返回 `bool`（`true` = 成功处理或已存在，`false` = 处理失败/丢弃）。

---

### Fix 3（建议）：统一私聊消息架构为读扩散（Read Fan-out）

当前私聊使用写扩散（两条记录），群聊使用读扩散（一条记录）。建议将私聊统一改为读扩散，消除根本矛盾。

**方案**：

```java
// ChatServiceImpl.java - sendMessage 改为单条写入
@Transactional(rollbackFor = Exception.class)
@Override
public Message sendMessage(Long senderId, SendMessageRequest request) {
    // ... 校验逻辑不变 ...

    Conversation senderConversation = getOrCreateConversation(senderId, request.getReceiverId());
    // 接收方会话仍需创建（用于 unread 计数等），但不再插入消息副本
    Conversation receiverConversation = getOrCreateConversation(request.getReceiverId(), senderId);

    LocalDateTime sendTime = LocalDateTime.now();
    // 只写一条记录，conversation_id 设为 0（类似群聊）或保留发送方会话
    Message message = new Message();
    message.setSenderId(senderId);
    message.setReceiverId(request.getReceiverId());
    message.setContent(request.getContent());
    message.setMessageType(request.getMessageType());
    message.setSendTime(sendTime);
    message.setSendStatus(SendStatus.SENT);
    message.setIsAi(false);
    message.setSequenceId(messageService.getNextSequenceId());
    messageMapper.insert(message);

    // 更新双方会话状态
    updateSenderConversation(senderConversation, message, senderId);
    updateReceiverConversation(receiverConversation, message, senderId);

    // 推送
    MessageVO messageVO = chatConverter.toVO(message);
    // ... 填充发送者信息 ...
    webSocketHandler.sendMessageToUser(request.getReceiverId(), new WebSocketMessage("new_message", messageVO));
    return message;
}
```

对应地，`syncMessages` 的查询条件需要调整：不再仅查 `conversationId IN (...)` ，改为同时支持通过 `senderId/receiverId` 查找：

```java
// 私聊读扩散时的 sync 查询
wrapper.gt(Message::getSequenceId, lastSequenceId)
    .and(w -> w
        .and(pw -> pw
            .eq(Message::getSenderId, userId)
            .or()
            .eq(Message::getReceiverId, userId)
        )
        .isNull(Message::getGroupId)
    )
    .or(w -> w.in(Message::getGroupId, groupIds));
```

---

### Fix 4（辅助）：修复 `selectMaxSequenceId` 与同步查询的语义对齐

如果维持写扩散架构，`selectMaxSequenceId` 应只返回用户自身会话中的最大 seq，以正确支持自愈逻辑：

```java
// MessageMapper.java
// 写扩散架构下：只统计属于该用户会话的消息（即 userId 的视角）
@Select("SELECT MAX(m.sequence_id) FROM messages m " +
        "INNER JOIN conversations c ON m.conversation_id = c.id " +
        "WHERE c.user_id = #{userId}")
Long selectMaxSequenceId(@Param("userId") Long userId);
```

---

### Fix 5（辅助）：发送消息后更新客户端 `_lastSequenceId`

**文件**：`linxin-client/lib/modules/chat/message_service.dart`

```dart
Future<Message?> sendMessage({...}) async {
    final response = await _httpService.sendMessage(...);

    final message = Message(
        id: response['id']?.toString() ?? '',
        conversationId: conversationId,
        senderId: response['senderId']?.toString() ?? '',
        content: content,
        messageType: messageType,
        status: 1,
        createdAt: DateTime.now(),
        isRead: false,
        sequenceId: int.tryParse(response['sequenceId']?.toString() ?? ''),  // 新增
    );

    await _localService.saveMessage(message);

    // 通知 DataService 更新 lastSequenceId
    if (message.sequenceId != null) {
        // 通过回调或 EventBus 通知 DataService 更新 seq
    }

    // ...
}
```

---

## 五、优先级与修复顺序

| 优先级 | Fix | 影响 | 工作量 |
|--------|-----|------|--------|
| P0 | Fix 1：修复 `getFriendById` 字段 | 解决消息丢弃根因 | 极低（1行代码） |
| P0 | Fix 2：seq 推进与处理结果绑定 | 防止已丢弃消息永久不可达 | 低（修改返回值逻辑） |
| P1 | Fix 3：私聊改为读扩散 | 根治双记录问题，简化架构 | 中（后端 + 前端 sync 逻辑） |
| P2 | Fix 4：修正 `selectMaxSequenceId` | 正确支持自愈逻辑 | 低 |
| P3 | Fix 5：发送后更新客户端 seq | 减少冗余同步 | 低 |

**最小修复集（立即可上线）**：Fix 1 + Fix 2，可解决"消息不可接收/不可拉取"的核心问题，无需改动后端或数据库 schema。

---

## 六、涉及文件清单

| 文件 | 位置 | 问题 |
|------|------|------|
| `ChatServiceImpl.java` | `linxin-server/.../service/impl/` | Bug 1：写扩散双写；Fix 3 改写 |
| `MessageMapper.java` | `linxin-server/.../mapper/` | Bug 4：selectMaxSequenceId 语义错误；Fix 4 修正 |
| `data_service.dart` | `linxin-client/lib/core/state/` | Bug 2（主因）、Bug 3（放大器）；Fix 1、Fix 2 修正 |
| `message_service.dart` | `linxin-client/lib/modules/chat/` | Bug 5：发送后不更新 seq；Fix 5 修正 |
| `V1.0.0.1__init.sql` | `linxin-server/.../db/migration/` | `sequence_id BIGINT UNIQUE`（写扩散下 OK，读扩散后仍 OK） |
