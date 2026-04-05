# 灵信 (LinXin) 功能结构文档

> 版本：v2.0 | 更新时间：2026-04-05

---

## 1. 产品概述

**灵信** 是一款以 AI 为核心驱动力的即时通讯应用，采用 **AI-Native** 设计理念。区别于传统 IM 产品，灵信的差异化能力体现在：

- **语义化操作**：用户可以通过自然语言（如"帮我问问老李明天几点开会"）来驱动 App 执行通讯操作，无需手动查找联系人。
- **Agentic API**：开放标准化的 Agent 接口，允许第三方智能体（如 AutoGPT、OpenClaw）通过授权 Token 远程调用灵信的通讯能力。
- **隐私优先**：所有语义实体解析（自然语言 → 用户ID）均在服务端密闭环境中完成，不向外部 Agent 暴露用户通讯录。

---

## 2. 功能模块总览

```
灵信 (LinXin)
├── 2.1 用户账号体系
├── 2.2 即时消息
├── 2.3 联系人管理
├── 2.4 群组功能
├── 2.5 AI 助手（本地 AI）
├── 2.6 Agent 开放平台（远程 Agent）
└── 2.7 数据同步与离线支持
```

---

## 2.1 用户账号体系

### 功能列表

| 功能 | 状态 | 说明 |
|------|------|------|
| 用户注册 | ✅ 已实现 | 支持用户名 + 密码注册，邮箱验证码二次校验 |
| 用户登录 | ✅ 已实现 | JWT Token 认证，支持 Token 刷新 |
| 个人资料编辑 | ✅ 已实现 | 支持修改昵称、头像、性别、个性签名 |
| 账号安全设置 | ✅ 已实现 | 修改密码、退出登录 |
| 邮箱验证 | ✅ 已实现 | 注册时发送验证码邮件进行邮箱鉴权 |
| 密码版本控制 | ✅ 已实现 | `password_version` 字段，密码变更后强制旧 Token 失效 |

### 关键页面

- `login_page.dart` — 登录页
- `register_page.dart` — 注册页（含邮箱验证码步骤）
- `profile_page.dart` — 个人资料展示
- `edit_profile_page.dart` — 个人资料编辑
- `account_security_page.dart` — 账号安全

---

## 2.2 即时消息

### 功能列表

| 功能 | 状态 | 说明 |
|------|------|------|
| 单聊发送文字消息 | ✅ 已实现 | WebSocket 实时推送 |
| 群聊发送消息 | ✅ 已实现 | 支持群组消息广播 |
| 查看历史消息 | ✅ 已实现 | 从本地 SQLite 读取，分页加载 |
| 消息状态追踪 | ✅ 已实现 | 发送中 / 已发送 / 已读状态（`message_status` 表） |
| 会话列表 | ✅ 已实现 | 显示最新消息预览、未读数气泡 |
| 消息置顶/免打扰 | ✅ 已实现 | 会话级别 `top_status` / `mute_status` 字段 |
| AI 消息标识 | ✅ 已实现 | AI 发送的消息在 UI 上有特殊标记（`is_ai` / `sender_type` 字段） |
| 离线消息补全 | ✅ 已实现 | 基于 `sequence_id` 的增量同步 |

### 关键页面

- `chat_list_page.dart` — 会话列表
- `chat_detail_page.dart` — 聊天详情页（含消息输入框）

### 消息模型

```
messages 表关键字段：
  id, conversation_id, sender_id, receiver_id, group_id
  message_type, content, extra (JSON)
  send_status, send_time, sequence_id
  is_ai (0=普通用户, 1=AI), sender_type ('USER'/'AGENT'/'AI_ASSISTANT')
```

---

## 2.3 联系人管理

### 功能列表

| 功能 | 状态 | 说明 |
|------|------|------|
| 好友列表 | ✅ 已实现 | 按名称排序展示 |
| 搜索用户 | ✅ 已实现 | 通过 username 搜索，跳转用户详情页 |
| 发送好友申请 | ✅ 已实现 | 申请附带打招呼文字 |
| 新朋友通知 | ✅ 已实现 | 实时接收好友申请 WebSocket 推送 |
| 通过/拒绝申请 | ✅ 已实现 | `friend_apply` 表状态流转 |
| 好友备注 | ✅ 已实现 | `friends.friend_nickname` — AI 语义实体解析的关键数据源 |
| 删除好友 | ✅ 已实现 | 软删除，WebSocket 推送通知对方 |
| 查看对方资料 | ✅ 已实现 | `user_details_page.dart`，支持发消息、申请好友等操作 |

### 关键页面

- `friend_list_page.dart` — 联系人列表
- `friend_apply_list_page.dart` — 新朋友列表
- `user_search_result_page.dart` — 用户搜索结果
- `user_details_page.dart` — 用户详情

---

## 2.4 群组功能

### 功能列表

| 功能 | 状态 | 说明 |
|------|------|------|
| 创建群组 | ✅ 已实现 | 选择联系人批量创建 |
| 群消息收发 | ✅ 已实现 | 支持群消息广播与接收 |
| 查看群成员 | ✅ 已实现 | 群设置页展示成员列表 |
| 修改群名/公告 | ✅ 已实现 | 仅群主可操作 |
| 邀请成员 | ✅ 已实现 | 从好友列表中选择 |
| 移除成员/解散群 | ✅ 已实现 | 仅群主权限 |
| 退出群组 | ✅ 已实现 | 普通群员可退出 |
| 群内免打扰 | ✅ 已实现 | `group_members.mute_status` |

### 关键页面

- `create_group_page.dart` — 创建群组
- `group_settings_page.dart` — 群设置（成员管理、群名、公告）

---

## 2.5 AI 助手（本地 AI Chat）

灵信内置了 AI 助手聊天功能，用户可在 `ai_chat_page` 中通过自然语言与 AI 交互，AI 会分析意图并调用工具执行操作。

### 交互流程

```
用户输入自然语言
    → 前端 AIChatPage 调用 AIService.chat()
    → 服务端 AIAgent.run() 进入 ReAct 推理循环
        → LLM 分析意图，返回 ToolCall 列表
        → 服务端执行 Tool（sendMessage / addFriend / createGroup）
        → 工具结果回注 LLM，完成下一轮推理
    → 返回最终文字回复 + 结构化 ToolCall 信息
    → 前端展示 AI 回复，用户可二次确认执行
```

### 功能列表

| 功能 | 状态 | 说明 |
|------|------|------|
| AI 对话 | ✅ 已实现 | 与内置 AI 助手自由聊天 |
| 发消息 Tool | ✅ 已实现 | AI 可代替用户发送消息给联系人 |
| 加好友 Tool | ✅ 已实现 | AI 可代替用户发起好友申请 |
| 创建群 Tool | ✅ 已实现 | AI 可代替用户创建群组并邀请成员 |
| 参数修改确认 | ✅ 已实现 | 用户可对 AI 识别到的参数进行修改后确认执行 |
| 多步推理（ReAct 循环） | ✅ 已实现 | 最多 5 轮迭代，支持先加好友再建群等复合任务 |

### 关键页面

- `ai_chat_page.dart` — AI 对话聊天界面

---

## 2.6 Agent 开放平台（远程 Agent）

灵信提供开放 API，允许第三方 Agent 以授权 Token 的方式远程调用灵信的通讯能力。

### 功能列表

| 功能 | 状态 | 说明 |
|------|------|------|
| Agent Token 管理 | ✅ 已实现 | 用户在 App 内生成、查看、吊销 Agent Token |
| Token Scope 控制 | ✅ 已实现 | `msg:send`、`contact:resolve` 等精细化权限 |
| 能力清单接口 | 📋 规划中 | `GET /api/agent/manifest` — 返回可用工具 JSON Schema |
| 意图执行接口 | ✅ 已实现 | `POST /api/agent/execute` — 接收自然语言指令并执行 |
| 语义歧义处理 | 📋 规划中 | 多结果匹配时返回 `REQUIRE_CLARIFICATION` |
| 操作溯源标记 | ✅ 已实现 | Agent 操作在消息中打标 `sender_type='AGENT'` |
| Token 使用记录 | ✅ 已实现 | `agent_tokens` 表追踪 Token 使用情况 |

### 关键页面

- `agent_token_page.dart` — Agent Token 管理
- `token_usage_page.dart` — Token 使用记录

---

## 2.7 数据同步与离线支持

### 实时同步（热路径）

- **协议**：原生 WebSocket（`ws://host:9099/lxa/ws`）
- **消息类型**：`message`、`group_message`、`friend_apply`、`friend_handle`、`friend_delete`
- **重连策略**：指数退避（最大 30 秒），App 生命周期内自动维持连接

### 离线补课（冷路径）

- **机制**：`sequence_id` 全局单调递增（Snowflake 算法）
- **触发时机**：App 启动、WebSocket 断线重连、手动刷新
- **接口**：`GET /chat/sync?lastSequenceId={id}`，返回缺失消息列表
- **客户端去重**：基于 `message.id` 的幂等写入 SQLite

### 本地存储

- **数据库**：SQLite（通过 `sqflite` 包）
- **存储内容**：消息记录、会话列表、用户信息（缓存）
- **刷新机制**：`DataService.notifyListeners()` — 驱动 UI 响应式更新

---

## 3. 用户场景地图

### 场景 A：移动端本地 AI 辅助

```
用户输入："帮我问问老李明天几点开会"
  → App 调用服务端 AI 解析意图
  → 服务端识别实体"老李" → 匹配 friend_nickname
  → 服务端执行 sendMessage
  → WebSocket 推送 → UI 实时更新
```

### 场景 B：第三方 Agent 远程代发

```
用户在 OpenClaw 输入："帮我告诉爱人我今晚加班"
  → OpenClaw 携带 AgentToken 调用 POST /api/agent/execute
  → 服务端解析"爱人" → 匹配 friend_nickname → 得到目标 userId
  → 服务端调用 MessageService 发送消息
  → 服务端 WebSocket 推送给用户移动端
  → 移动端自动写入本地 SQLite，UI 刷新
```

### 场景 C：语义歧义处理

```
用户输入："给张三发信息"
  → 系统匹配到"张三-同事"和"张三-同学"两个记录
  → 返回 REQUIRE_CLARIFICATION 状态
  → 返回脱敏后的选项（仅提示备注，不返回真实 ID）
  → Agent / AI 向用户询问确认
```
