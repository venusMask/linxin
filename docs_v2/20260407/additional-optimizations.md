# 补充优化建议

> 日期：2026-04-07  
> 本文为 `code-quality-optimization.md` 的补充，记录在代码细节走查中发现的额外问题

---

## A1 【严重 BUG】消息发送后又通过 WebSocket 再发一次

**文件**：`chat_detail_page.dart:230-242`

```dart
// HTTP POST 发消息成功后，已完成持久化并推送给接收方
response = await _httpService.sendMessage(...);

// ...处理 UI...

// 问题：又通过 WebSocket 再发送一次！
_webSocketService.sendMessage(
    content: text,
    conversationId: _chatId,
    messageType: 1,
);
```

HTTP 发送成功后，服务端已持久化消息并通过 WebSocket 推送给接收方。随后再调用 `_webSocketService.sendMessage` 会让接收方收到两条内容相同的消息。

**修复**：删除 `_sendMessage()` 中末尾的两段 WebSocket 调用（私聊和群聊各一处）：

```dart
// 删除以下内容（chat_detail_page.dart:230-242）
if (_isGroupChat) {
    _webSocketService.sendGroupMessage(...);
} else {
    _webSocketService.sendMessage(...);
}
```

---

## A2 【安全】HTTP 拦截器将完整请求体与响应体写入日志

**文件**：`http_service.dart:31,35`

```dart
LogService.info('HTTP Request: ... Data: ${options.data} ...');
LogService.info('HTTP Response: ... Data: ${response.data}');
```

所有消息内容、token、好友列表等敏感数据都以明文形式记录到日志。生产环境会导致：
- 敏感数据泄露（消息内容、用户信息）
- 大量 IO 拖慢性能
- 日志文件膨胀

**修复**：仅记录请求元信息，body 不记录（或仅 debug 模式下记录）：

```dart
onRequest: (options, handler) {
    if (_token != null) {
        options.headers['Authorization'] = 'Bearer $_token';
    }
    LogService.debug('[HTTP] ${options.method} ${options.path}');
    return handler.next(options);
},
onResponse: (response, handler) {
    LogService.debug('[HTTP] ${response.statusCode} ${response.requestOptions.path}');
    // 解包 Result 结构...
},
```

---

## A3 【性能】`requestRaw` 每次创建新 Dio 实例

**文件**：`http_service.dart:106-118`

```dart
Future<dynamic> requestRaw(...) async {
    final options = BaseOptions(...);
    final dio = Dio(options);        // 每次调用都创建新连接池
    if (_token != null) {
        dio.options.headers['Authorization'] = 'Bearer $_token';  // 手动加 token，绕过拦截器
    }
    final response = await dio.post(path, data: data);
}
```

此方法每次调用都创建独立的 Dio 实例（新的连接池），既浪费资源，又绕过了统一的 auth/error 拦截器，需要手动重复注入 token。

**修复**：在现有 `_dio` 实例上单次覆盖超时参数即可：

```dart
Future<dynamic> requestRaw(String path, {
    dynamic data,
    Duration connectTimeout = const Duration(seconds: 10),
    Duration receiveTimeout = const Duration(seconds: 60),
}) async {
    final response = await _dio.post(
        path,
        data: data,
        options: Options(
            sendTimeout: connectTimeout,
            receiveTimeout: receiveTimeout,
        ),
    );
    return response.data;
}
```

---

## A4 【稳定性】Token 过期后客户端无任何处理

**文件**：`http_service.dart:56-74`

`onError` 拦截器处理了超时和 badResponse，但没有针对 HTTP 401 的逻辑。生产环境 Token 有效期为 60 分钟（`application-pro.yml`），过期后所有请求静默失败，用户不知道需要重新登录。

**修复**：在 `onError` 中增加 401 处理：

```dart
onError: (e, handler) {
    if (e.response?.statusCode == 401) {
        LogService.warn('Token expired, redirecting to login');
        AuthService().logout();           // 清除本地 token 和状态
        EventBus.instance.emit(SessionExpiredEvent());  // 通知 UI 跳转登录页
        return handler.reject(e);
    }
    // 原有逻辑...
}
```

---

## A5 【性能】历史消息翻页用 offset，深翻性能差

**文件**：`ChatServiceImpl.java:359`、`chat_detail_page.dart:65`

```java
// 第 N 页 = 跳过 (N-1)*pageSize 条记录再读取
Page<Message> page = new Page<>(pageNum, pageSize);
wrapper.eq(Message::getConversationId, conversationId)
       .orderByDesc(Message::getSendTime);
messageMapper.selectPage(page, wrapper);
```

MySQL offset 分页在跳过大量行时会扫描并丢弃前面所有行，随着历史消息增多性能线性下降。IM 场景的历史消息加载天然适合游标分页。

**修复**：改为基于 `sequence_id` 的游标分页：

```java
// 接口参数由 pageNum 改为 beforeSequenceId（游标）
@GetMapping("/messages/{conversationId}")
public Result<List<MessageVO>> getMessages(
    @PathVariable Long conversationId,
    @RequestParam(required = false) Long beforeSequenceId,
    @RequestParam(defaultValue = "20") Integer pageSize
) {
    LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Message::getConversationId, conversationId);
    if (beforeSequenceId != null) {
        wrapper.lt(Message::getSequenceId, beforeSequenceId);  // 只取比 cursor 小的
    }
    wrapper.orderByDesc(Message::getSequenceId)
           .last("LIMIT " + Math.min(pageSize, 50));
    // ...
}
```

客户端上拉加载更多时，传入当前最早一条消息的 `sequenceId` 作为 cursor，无论翻多深，每次都是索引范围查询，性能恒定。

---

## A6 【体验】会话列表冷启动为空，`conversations.version` 字段从未使用

**文件**：`data_service.dart:236`、`V1.0.0.1__init.sql:75`

App 冷启动时 `_chats = []`，会话列表完全为空，必须等 WebSocket 连接并触发 `syncMessages` 后才能渐进显示。而 `conversations` 表已经建了 `version` 字段（`BIGINT DEFAULT 0`）和对应索引（`idx_user_conversation_version`），明显是预留给会话列表增量同步用的，但一直未实现。

**两步完善**：

**第一步**：服务端实现会话增量同步接口

```java
// ChatController.java
@GetMapping("/conversations/sync")
public Result<List<ConversationVO>> syncConversations(
    @RequestAttribute("userId") Long userId,
    @RequestParam(defaultValue = "0") Long lastVersion
) {
    // 取 version > lastVersion 的所有会话
    List<Conversation> updated = conversationMapper.selectList(
        new LambdaQueryWrapper<Conversation>()
            .eq(Conversation::getUserId, userId)
            .gt(Conversation::getVersion, lastVersion)
            .orderByAsc(Conversation::getVersion)
    );
    return Result.success(updated.stream().map(chatConverter::toVO).collect(toList()));
}
```

**第二步**：每次更新会话时递增 version

```java
// ChatServiceImpl - updateReceiverConversation 中加一行
updateWrapper.setSql("version = version + 1");
```

**第三步**：客户端在 `initialize()` 或 WebSocket 连接时同步会话列表，存入本地 SQLite 的 `conversations` 表，冷启动时直接从本地恢复。

---

## A7 【可靠性】消息发送无幂等键，网络重试会产生重复消息

**文件**：`http_service.dart:144`、`ChatServiceImpl.java:177`

当前发消息请求没有携带客户端生成的消息 ID。网络超时时，客户端无法判断服务端是否已处理，如果重试则会在 DB 中插入两条内容相同的消息。

**修复**：

客户端（已有 `tempId`，直接传出去）：

```dart
// chat_detail_page.dart
final tempId = 'msg_${DateTime.now().millisecondsSinceEpoch}';

await _httpService.sendMessage(
    clientMsgId: tempId,      // 新增参数
    receiverId: ...,
    content: text,
);
```

服务端 `messages` 表增加字段：

```sql
ALTER TABLE messages ADD COLUMN client_msg_id VARCHAR(64) DEFAULT NULL;
CREATE UNIQUE INDEX uk_sender_client_msg ON messages(sender_id, client_msg_id);
```

`ChatServiceImpl.sendMessage` 开头加幂等检查：

```java
if (request.getClientMsgId() != null) {
    Message existing = messageMapper.selectOne(
        new LambdaQueryWrapper<Message>()
            .eq(Message::getSenderId, senderId)
            .eq(Message::getClientMsgId, request.getClientMsgId())
    );
    if (existing != null) return existing;   // 幂等返回已有消息
}
```

---

## A8 【体验】发送失败的消息有状态但无重试入口

**文件**：`chat_detail_page.dart:250-253`

```dart
_messages[index] = pendingMessage.copyWith(status: -1);  // 标记失败
```

UI 层已把失败消息的 `status` 置为 -1，但 `message_bubble.dart` 没有对应的失败状态展示和重试交互。用户唯一办法是重新手打内容。

**修复**：在 `MessageBubble` 中对 `status == -1` 的消息显示感叹号图标，点击后触发重试回调：

```dart
// message_bubble.dart
if (message.status == -1)
    GestureDetector(
        onTap: onRetry,
        child: Icon(Icons.error_outline, color: Colors.red, size: 16),
    ),
```

`ChatDetailPage` 中传入 `onRetry`，重新调用 `_sendMessage` 并将临时 ID 复用（配合 A7 的幂等键，重试不会产生重复消息）。

---

## 优先级汇总

| 优先级 | 编号 | 问题 | 工作量 |
|--------|------|------|--------|
| P0 | A1 | 消息双重发送 BUG | 极低（删几行） |
| P0 | A2 | HTTP 日志打印敏感数据 | 低 |
| P1 | A4 | Token 过期无处理 | 低 |
| P1 | A3 | requestRaw 新建 Dio 实例 | 极低 |
| P2 | A7 | 消息发送幂等键 | 中（前后端各改） |
| P2 | A6 | 会话列表增量同步（利用 version 字段） | 中 |
| P3 | A5 | 历史消息翻页改为 cursor | 中 |
| P3 | A8 | 失败消息重试 UI | 低 |
