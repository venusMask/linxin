# 灵信 (LinXin) 数据同步与一致性策略 (MVP)

## 1. 核心挑战
Agent 从云端发起的“代发”操作，本质上是一种**异地写入**。为了保证用户在移动端 App 上能实时看到这些变动，必须解决：
1. **实时通知 (Real-time Notification)**
2. **状态一致性 (State Consistency)**
3. **离线补课 (Offline Synchronization)**

---

## 2. 实时同步流程 (WebSocket)

### 2.1 链路设计
1. **Agent 调用** -> 服务端接口执行成功。
2. **服务端入库** -> 存储消息，生成全局唯一的 `sequenceId` (由 Redis 或 Snowflake 生成)。
3. **WebSocket 推送** -> 服务端立即向该用户的所有在线端推送消息。

### 2.2 推送 Payload (示例)
```json
{
  "type": "new_message",
  "data": {
    "id": "msg_101",
    "conversationId": "chat_123",
    "content": "今晚吃啥？",
    "senderId": "agent_openclaw",
    "senderType": "AGENT",
    "createdAt": "2024-04-04T12:00:00Z",
    "sequenceId": 5001
  }
}
```

---

## 3. 离线同步策略 (Sequence ID)

如果 App 处于离线状态，它会错过 WebSocket 推送。因此需要引入 **Sequence ID (序列号)**。

### 3.1 客户端同步触发点
1. **App 启动**。
2. **WebSocket 断开后重新连接**。
3. **手动下拉刷新**。

### 3.2 同步算法
1. **客户端请求**：`GET /chat/sync?lastSequenceId=4990`。
2. **服务端查询**：查找 `sequenceId > 4990` 的所有消息记录。
3. **分批下发**：服务端返回缺失的消息列表，并按 `sequenceId` 排序。
4. **客户端合并**：App 遍历返回列表，逐条存入本地 SQLite，并更新本地 `lastSequenceId`。

---

## 4. 冲突处理与去重

### 4.1 消息幂等性
由于同步机制可能会导致重复接收（例如 WebSocket 收到一次，Sync 接口又收到一次），客户端必须基于 **MessageID** 进行幂等处理。
- **策略**：在插入 SQLite 前，检查是否存在相同的 `id`，若存在则更新状态而非插入新行。

### 4.2 顺序保证
UI 渲染应严格基于 `sequenceId` 或 `createdAt` 排序，确保 Agent 代发的消息出现在正确的时间线位置。

---

## 5. UI 回写表现

为了避免用户困惑，Agent 发出的消息应具有特殊的视觉反馈：
- **发送者标识**：显示为“Agent 辅助发送”或“来自 [Agent名称]”。
- **本地化缓存**：同步后的消息应立即触发 `DataService.notifyListeners()`，确保会话列表和聊天详情页实时刷新。
