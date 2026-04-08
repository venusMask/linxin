# 代码质量分析与优化方案

> 日期：2026-04-07  
> 定位：全新项目，仅有 MySQL 可用，无需兼容遗留包袱  
> 原则：按正确的方式设计，删除"为了兼容"的冗余代码

---

## 一、客户端（Flutter）

---

### C1 【严重】`DataService.initialize()` 从未被调用

**文件**：`main.dart:19`、`data_service.dart:255`

```dart
// main.dart 中只创建，从未 await initialize()
ChangeNotifierProvider(create: (_) => DataService()),
```

`initialize()` 负责从 SharedPreferences 恢复 `_lastSequenceId`、从本地 SQLite 加载 `_friends`。由于从未被调用：

- 每次 App 启动 `_lastSequenceId = 0`，导致 WebSocket 连接后 sync 拉取全量消息
- `_friends` 从内存而非本地恢复，冷启动后好友列表为空，必须等网络请求才能显示

**修复**：

```dart
// main.dart
void main() async {
    WidgetsFlutterBinding.ensureInitialized();
    await LogService().init();
    final authService = AuthService();
    await authService.initialize();

    final dataService = DataService();
    await dataService.initialize();   // 在 UI 之前完成本地数据恢复

    runApp(
        MultiProvider(
            providers: [
                ChangeNotifierProvider.value(value: authService),
                ChangeNotifierProvider.value(value: dataService),
            ],
            child: const MyApp(),
        ),
    );
}
```

---

### C2 【严重】SharedPreferences 的 `last_sequence_id` 未区分用户

**文件**：`data_service.dart:249,258`

```dart
await prefs.setInt('last_sequence_id', _lastSequenceId);      // 无用户隔离
_lastSequenceId = prefs.getInt('last_sequence_id') ?? 0;
```

对比 `auth_service.dart:197` 中已正确使用 `TestConfig.storagePrefix`：

```dart
await prefs.setString('${prefix}auth_token', token);
```

在同一设备上切换账号时，用户 B 会继承用户 A 的 `last_sequence_id`，导致消息拉取完全错乱。

**修复**：

```dart
// data_service.dart
Future<void> _updateLastSequenceId(int newId) async {
    if (newId > _lastSequenceId) {
        _lastSequenceId = newId;
        final prefs = await SharedPreferences.getInstance();
        final userId = AuthService().currentUser?.id?.toString() ?? 'default';
        await prefs.setInt('${TestConfig.storagePrefix}last_seq_$userId', _lastSequenceId);
    }
}
```

初始化时同样使用用户 ID 作为 key 后缀读取。

---

### C3 【中】`Message.fromJson` 与 `Message.fromServerJson` 几乎完全相同

**文件**：`message.dart:104-157`

两个工厂方法的差异仅有一处：`fromJson` 多了 `time` 字段解析。全部逻辑重复，是死代码。

**修复**：删除 `fromServerJson`，统一使用 `fromJson`。

---

### C4 【中】`Message.fromJson` 中大量 snake_case 回退是死代码

**文件**：`message.dart:107-128`

```dart
conversationId: json['conversationId']?.toString() ?? json['conversation_id']?.toString() ?? '',
senderId: json['senderId']?.toString() ?? json['sender_id']?.toString() ?? '',
senderNickname: json['senderNickname'] as String? ?? json['sender_nickname'] as String?,
// ...以此类推
```

服务端 `JacksonConfig.java` 已统一 camelCase 输出，本地 SQLite 的 `_messageFromDb` 方法直接用 `json['sender_id']` 读数据库列名，两条路径不相交。`json['conversation_id']` 等 snake_case 回退在运行时永远不会命中，可以全部删除。

**修复**：`fromJson` 只保留 camelCase 字段，移除所有 `?? json['xxx_xxx']` 回退：

```dart
factory Message.fromJson(Map<String, dynamic> json) {
    return Message(
        id: json['id']?.toString() ?? '',
        conversationId: json['conversationId']?.toString() ?? '',
        senderId: json['senderId']?.toString() ?? '',
        senderNickname: json['senderNickname'] as String?,
        content: json['content'] as String? ?? '',
        messageType: json['messageType'] as int? ?? 1,
        createdAt: json['sendTime'] != null
            ? DateTime.parse(json['sendTime'] as String)
            : DateTime.now(),
        isRead: json['isRead'] as bool? ?? false,
        isMe: json['isMe'] as bool? ?? false,
        groupId: json['groupId']?.toString(),
        conversationType: json['conversationType'] as int? ?? 0,
        sequenceId: int.tryParse(json['sequenceId']?.toString() ?? ''),
        senderType: json['senderType'] as String?,
        isAi: json['isAi'] as bool? ?? false,
    );
}
```

---

### C5 【中】`Message.sequenceId` 的类型应为 `String`，而非 `int?`

**文件**：`message.dart:16`

服务端通过 `JacksonConfig` 将所有 `Long` 序列化为字符串。客户端当前用 `int.tryParse` 将字符串转回 `int`，再存入 SQLite 的 `INTEGER` 列。

问题：
- 这套转换链本身就是在填 JSON number 精度的坑，虽然 Dart native 的 `int` 是 64 位不会溢出，但逻辑上绕了一圈
- 本地 SQLite 里存的是 `INTEGER`（64-bit），从 DB 读回时用 `json['sequence_id'] as int?`，与网络数据路径不对称
- `sequenceId` 的语义是"用于比大小的有序 token"，不需要做任何数学运算，`String` 反而更合适

**修复方案 A（推荐）**：`sequenceId` 统一用 `String`。网络层直接拿字符串，SQLite 也存 TEXT。比较时用 `BigInt.parse` 或服务端统一转数字返回。

**修复方案 B（最小改动）**：保持 `int?`，但删除 `is String ? tryParse : as int?` 的分支判断，因为服务端始终是 String，直接 `int.tryParse(json['sequenceId']?.toString() ?? '')`，去掉 `json['sequence_id']` 回退。

---

### C6 【中】`chat_` 临时 ID 前缀检测是脆弱的

**文件**：`data_service.dart:87`

```dart
if (chat != null && chat.id.startsWith('chat_')) {
    // 临时 ID，替换为真实 conversationId
}
```

**问题**：
- 依赖魔法字符串前缀做类型判断
- `createChat` 中的临时 ID 生成：`'chat_${DateTime.now().millisecondsSinceEpoch}'`，同一毫秒内多次调用可能重复

**修复**：增加独立的 `bool isTemporary` 字段，或通过分离"已确认的会话 ID"和"本地临时 ID"两种类型来表达，不依赖字符串前缀。

---

### C7 【中】WebSocket 监听器的 StreamSubscription 从未取消

**文件**：`data_service.dart:29-56`

```dart
void _initWebSocketListener() {
    WebSocketService.instance.connectStream.listen((_) { ... });
    WebSocketService.instance.messageStream.listen((event) { ... });
    // 返回的 StreamSubscription 被丢弃，无法取消
}
```

`DataService` 继承 `ChangeNotifier`，但没有实现 `dispose()`，导致 StreamSubscription 持续占用内存且 GC 无法回收。

**修复**：

```dart
class DataService extends ChangeNotifier {
    StreamSubscription? _msgSub;
    StreamSubscription? _connectSub;
    StreamSubscription? _friendSub;
    StreamSubscription? _groupMsgSub;

    void _initWebSocketListener() {
        _connectSub = WebSocketService.instance.connectStream.listen((_) {
            syncMessages();
            syncFriends();
        });
        _msgSub = WebSocketService.instance.messageStream.listen((event) {
            if (event['type'] == 'new_message') _handleIncomingMessage(event['data']);
        });
        // ...
    }

    @override
    void dispose() {
        _msgSub?.cancel();
        _connectSub?.cancel();
        _friendSub?.cancel();
        _groupMsgSub?.cancel();
        super.dispose();
    }
}
```

---

### C8 【低】本地 SQLite `conversations` 表实际上没有使用

**文件**：`db_service.dart:66-78`

建了 `conversations` 表，有完整 schema，但 `DataService._chats` 完全在内存中管理，从不读写这张表。每次冷启动 `_chats` 为空，会话列表靠消息同步时动态重建。

**建议**：要么真正使用这张表（冷启动从本地恢复会话列表），要么删掉表定义减少误导。保留一张永远不读写的表只会让后续维护者困惑。

---

### C9 【低】软删除方案本地与服务端不一致

- 服务端：`deleted TINYINT DEFAULT 0`（MyBatis @TableLogic，0/1）
- 本地 SQLite：`deleted_at TEXT`（时间戳字符串软删除）

全新项目应统一选一种。如果客户端不需要软删除（删了直接不展示即可），本地直接硬删除最简单。

---

## 二、服务端（SpringBoot）

---

### S1 【严重】`LocalMessageBroker` 日志信息具有误导性

**文件**：`LocalMessageBroker.java:27`

```java
log.debug("User {} not online locally, message marked as offline (stored in MySQL)", userId);
```

实际上消息**没有**存入任何离线队列。`MysqlOfflineMessageService.saveOfflineMessage` 方法体为空（只打 debug 日志），且从未被调用。这条日志让人以为离线消息有保障，实际上离线用户会静默丢失推送（依赖客户端主动 sync 找回）。

**修复**：改为如实描述的日志：

```java
log.debug("User {} is offline, message will be retrieved via /chat/sync on next login", userId);
```

---

### S2 【高】`IMessageService.sendMessage` 与 `IMessageService.sendAgentMessage` 是死代码

**文件**：`MessageServiceImpl.java:33-111`

`IMessageService.sendMessage()` 从未被任何 Controller 或其他 Service 调用，实际发消息路径全部走 `ChatServiceImpl.sendMessage()`。`sendAgentMessage` 同理。

`IMessageService` 的唯一有效职能是 `getNextSequenceId()`。

**修复**：

```java
// 精简接口，只保留真正使用的方法
public interface IMessageService {
    Long getNextSequenceId();
    List<Message> getMessages(Long conversationId, int pageNum, int pageSize);
    void markRead(Long userId, Long conversationId);
}
```

删除 `sendMessage`、`sendAgentMessage` 方法及其实现。

---

### S3 【高】`IOfflineMessageService` 接口及其实现是空壳

**文件**：`MysqlOfflineMessageService.java`、`IOfflineMessageService.java`

`saveOfflineMessage` 方法体为空，且全局没有任何地方调用它。`fetchAndClearMessages` 也只返回空 List。这两个接口方法都是哑火的。

离线消息的实际机制是：客户端重连时调用 `/chat/sync` REST 接口。这个机制是正确的，不需要 `IOfflineMessageService`。

**修复**：删除 `IOfflineMessageService` 接口、`MysqlOfflineMessageService` 实现类，以及 `WebSocketHandler` 中对应的注入和调用：

```java
// WebSocketHandler.java - 删除以下内容
private final IOfflineMessageService offlineMessageService;

// afterConnectionEstablished 中删除
List<Object> pendingMessages = offlineMessageService.fetchAndClearMessages(userId);
for (Object msg : pendingMessages) {
    pushMessageToLocalUser(userId, msg);
}
```

---

### S4 【高】`IMessageBroker` 单实现抽象，MySQL-Only 场景下过度设计

**文件**：`IMessageBroker.java`、`LocalMessageBroker.java`

`IMessageBroker` 的设计意图是"将来可能接入 Redis Pub/Sub 或 MQ 做跨节点路由"。但约束是**只有 MySQL**，未来也不会有多节点。这层抽象带来了循环依赖（`@Lazy`）和无谓的间接层。

**修复**：删除 `IMessageBroker` 接口，`WebSocketHandler` 直接持有推送逻辑：

```java
// WebSocketHandler.java
public void sendMessageToUser(Long userId, Object message) {
    boolean success = pushMessageToLocalUser(userId, message);
    if (!success) {
        log.debug("User {} is offline, message will be retrieved via /chat/sync", userId);
    }
}
```

---

### S5 【高】`selectMaxSequenceId` 的 OR 查询无法利用任何现有索引

**文件**：`MessageMapper.java:11`

```java
@Select("SELECT MAX(sequence_id) FROM messages WHERE sender_id = #{userId} OR receiver_id = #{userId}")
Long selectMaxSequenceId(@Param("userId") Long userId);
```

`V1.0.0.2__performance_tuning.sql` 添加的索引：
- `idx_receiver_seq (receiver_id, sequence_id)`
- `idx_sender_seq (sender_id, sequence_id)`

MySQL 在面对 `WHERE A = ? OR B = ?` 时无法同时使用两个索引，会退化为全表扫描（或 index merge，效率也低）。随着消息表增大，这个查询会成为性能瓶颈。

**修复方案（MySQL-Only）**：用 UNION 改写，使每个分支都能走索引：

```java
@Select("SELECT MAX(seq) FROM (" +
        "  SELECT MAX(sequence_id) AS seq FROM messages WHERE sender_id = #{userId} " +
        "  UNION ALL " +
        "  SELECT MAX(sequence_id) AS seq FROM messages WHERE receiver_id = #{userId}" +
        ") t")
Long selectMaxSequenceId(@Param("userId") Long userId);
```

但注意，这个查询本身在写扩散架构下存在语义问题（见消息 Bug 文档 Bug 4），建议配合 Fix 4 一起改。

---

### S6 【中】`JacksonConfig` 注释说明了错误的原因

**文件**：`JacksonConfig.java:21`

```java
// 解决前端 JS 无法精确处理 Long 的问题 (2^53-1 限制)
simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
```

这是 Flutter native（iOS/Android），不是 Web 应用，不存在 JavaScript 的 53 位精度问题。

但 Long→String 的序列化本身是正确的：统一用字符串传递 ID 使客户端类型处理更简单。注释应改为：

```java
// 将 Long 类型 ID 序列化为字符串，统一 API 中 ID 的类型为 String
```

---

### S7 【中】`SnowflakeIdWorker.workerId` 硬编码

**文件**：`SnowflakeIdWorker.java:30`

```java
private long workerId = 1L;
```

单节点部署没有问题，但没有任何配置入口。若以后需要调试或区分环境，只能改代码重部署。

**修复**：

```java
@Value("${snowflake.worker-id:1}")
private long workerId;
```

对应 `application.yml` 增加：

```yaml
snowflake:
  worker-id: 1
```

---

### S8 【中】三个 `getOrCreateConversation` 变体逻辑分散

**文件**：`ChatServiceImpl.java:101`、`140`、`574`

- `getOrCreateConversation(userId, peerId)` — 私聊
- `getOrCreateAIConversationInternal(userId)` — AI 会话（实际也是私聊，只是 peer 是 AI 用户）
- `getOrCreateGroupConversation(userId, groupId)` — 群聊

私聊与 AI 会话逻辑几乎相同，AI 版本只是 hardcode 了 `peerId = aiUserId`。可合并：

```java
// AI 会话本质就是私聊，无需单独方法
Conversation aiConv = getOrCreateConversation(userId, aiUserId);
```

---

### S9 【低】`sequence_id BIGINT UNIQUE` 约束语义模糊

**文件**：`V1.0.0.1__init.sql:98`

`UNIQUE` 约束保证雪花 ID 不重复，这是对的（雪花 ID 本身是全局唯一的），但约束本身产生了一个全局唯一索引，所有消息写入都会竞争这个索引的锁。在 MySQL 的 InnoDB 引擎下，UNIQUE 索引的插入会加间隙锁（gap lock），高并发时可能成为热点。

由于雪花算法已经保证了唯一性，这个 `UNIQUE` 约束是冗余的安全网。

**建议**：如果并发压力不大，保留原样。如需优化，改为普通 INDEX：

```sql
-- 去掉 UNIQUE，改为普通索引（雪花 ID 本身保证唯一）
INDEX idx_sequence_id (sequence_id)
```

---

### S10 【低】AI 异步任务无超时控制

**文件**：`ChatServiceImpl.java:280`

```java
taskExecutor.execute(() -> {
    ChatResponse response = aiService.processUserInput(aiRequest, context);
    // 如果 AI 服务无响应，此线程永久阻塞
});
```

无超时设置，AI 服务挂起会导致线程池耗尽。

**修复**：

```java
CompletableFuture.supplyAsync(() -> aiService.processUserInput(aiRequest, context), taskExecutor)
    .orTimeout(30, TimeUnit.SECONDS)
    .whenComplete((response, ex) -> {
        if (ex != null) {
            log.error("AI processing timeout or failed for user {}", userId, ex);
            // 发送错误提示消息给用户
        } else {
            // 正常处理 response
        }
    });
```

---

## 三、优先级汇总

| 优先级 | 编号 | 问题 | 工作量 |
|--------|------|------|--------|
| P0 | C1 | `DataService.initialize()` 未调用 | 极低（3行） |
| P0 | C2 | SharedPreferences key 未区分用户 | 低 |
| P0 | S1 | 日志误导用户离线消息"已存储" | 极低（1行） |
| P1 | S2 | 删除 `IMessageService` 死代码 | 低 |
| P1 | S3 | 删除 `IOfflineMessageService` 空壳 | 低 |
| P1 | S4 | 删除 `IMessageBroker` 过度抽象 | 低 |
| P1 | S5 | 修复 `selectMaxSequenceId` OR 查询 | 低 |
| P2 | C3 | 删除 `fromServerJson` 重复代码 | 极低 |
| P2 | C4 | 删除 snake_case 回退死代码 | 低 |
| P2 | C7 | 修复 StreamSubscription 内存泄漏 | 低 |
| P3 | C5 | 统一 `sequenceId` 类型为 `String` | 中 |
| P3 | C8 | 本地 conversations 表实际使用或删除 | 中 |
| P3 | S6 | 更正 JacksonConfig 注释 | 极低 |
| P3 | S7 | `workerId` 配置化 | 极低 |
| P3 | S8 | 合并 AI 会话与私聊的 getOrCreate | 低 |
| P4 | C6 | 替换临时 ID 字符串前缀机制 | 中 |
| P4 | C9 | 统一本地/服务端软删除方案 | 中 |
| P4 | S9 | sequence_id UNIQUE → INDEX | 极低 |
| P4 | S10 | AI 任务增加超时 | 低 |

**最小有效改动集（P0+P1，预计半天）**：C1、C2、S1、S2、S3、S4 合并处理，可显著降低代码复杂度和潜在问题，且无任何功能风险。
