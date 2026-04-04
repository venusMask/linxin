# 灵信 (LinXin) Agent 接口规范 (MVP)

## 1. 能力发现 (Capability Discovery)

第三方 Agent 应首先通过该接口了解系统支持的工具。

- **Endpoint**: `GET /api/agent/manifest`
- **Authentication**: `Bearer <UserAgentToken>`

### 响应格式 (Manifest JSON)
```json
{
  "version": "1.0.0",
  "tools": [
    {
      "name": "send_message",
      "description": "发送文本消息。支持通过姓名、备注或模糊语义匹配联系人。",
      "parameters": {
        "recipient": "string (ID, 备注名, 或如 '我老婆' 这种模糊描述)",
        "content": "string (消息文本)"
      }
    },
    {
      "name": "search_history",
      "description": "搜索聊天记录。",
      "parameters": {
        "keyword": "string"
      }
    }
  ]
}
```

---

## 2. 意图执行 (Intent Execution)

核心接口，Agent 提交自然语言指令。

- **Endpoint**: `POST /api/agent/execute`
- **Payload**:
```json
{
  "command": "给爱人发个信息，问问今晚吃什么"
}
```

### 响应场景 A：成功执行
```json
{
  "status": "SUCCESS",
  "action": "send_message",
  "data": {
    "target": "爱人(小芳)",
    "messageId": "msg_887212",
    "timestamp": 1712210000
  }
}
```

### 响应场景 B：语义歧义 (Ambiguity)
系统发现多个匹配项，要求澄清。
```json
{
  "status": "REQUIRE_CLARIFICATION",
  "error_code": "AMBIGUOUS_RECIPIENT",
  "message": "发现了两个‘老李’，请确认是哪位？",
  "choices": [
    { "hint": "老李 (备注: 同事)", "id_placeholder": "ref_1" },
    { "hint": "老李 (备注: 邻居)", "id_placeholder": "ref_2" }
  ]
}
```
*注意：不返回真实的 UserID，仅返回占位符以保护隐私。*

---

## 3. 安全协议细节

### 3.1 Token 分发
- 用户在 App 内生成 `AgentToken`。
- Token 绑定特定的 Scope。目前 MVP 支持：
    - `msg:send` (发送消息)
    - `contact:resolve` (语义解析联系人)

### 3.2 隐私保护规则
- **模糊匹配**：系统内部解析“媳妇”-> ID 1001 的逻辑对 Agent 透明。
- **数据孤岛**：Agent 无法通过 API 批量导出好友列表或消息记录。

---

## 4. 最佳实践 (Best Practices)

1. **上下文感知的指令**：Agent 可以在 `command` 中包含上下文描述，提高解析率。
2. **频率限制**：MVP 阶段限制每分钟 60 次请求，超过将返回 `429 Too Many Requests`。
3. **回写校验**：Agent 可以定期调用结果查询接口，确认消息是否已成功推送到用户的移动端。
