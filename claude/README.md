# 灵信 (LinXin) - AI原生即时通讯应用

## 项目概述

**灵信**是一款新一代AI原生即时通讯软件，用户通过自然语言与AI交互即可完成消息发送、信息总结、定时提醒等操作。

## MVP功能范围

- [x] 用户认证（注册/登录/JWT Token）
- [x] 好友管理（添加/删除/申请处理）
- [x] 一对一聊天
- [x] 群聊（500人以下）
- [x] 消息已读未读状态
- [x] 消息搜索
- [x] AI服务集成（服务端解析 + 客户端执行）

## 项目结构

```
im/
├── lin_xin/              # Flutter客户端
├── lin_xin_mgr/          # SpringBoot服务端
├── doc/                  # 项目文档
│   └── mvp.md           # MVP设计文档
└── .claude/              # 架构文档
    ├── README.md         # 项目概述（本文件）
    ├── backend/          # 后端文档
    │   ├── architecture.md  # 后端架构
    │   └── features.md      # 后端功能
    └── frontend/         # 前端文档
        ├── architecture.md  # 前端架构
        └── features.md      # 前端功能
```

## 技术栈

### 客户端 (lin_xin)
- **框架**: Flutter 3.10.8+ (Dart)
- **状态管理**: ChangeNotifier + AuthService
- **网络层**: Dio 5.9.2（HTTP）+ WebSocket（实时通信）
- **本地存储**: SQLite (sqflite 2.4.1)
- **AI服务**: 通过HTTP调用服务端AI接口 + 本地执行操作

**详细文档**: [前端架构](./frontend/architecture.md) | [前端功能](./frontend/features.md)

### 服务端 (lin_xin_mgr)
- **框架**: SpringBoot 3.4.2 (Java 17)
- **持久层**: MyBatis-Plus 3.5.10.1
- **数据库**: MySQL 8.x + Druid 1.2.24 连接池
- **数据库迁移**: Flyway 11.10.5
- **安全认证**: Spring Security + JWT (jjwt 0.12.7)
- **API文档**: Knife4j 4.5.0 + SpringDoc OpenAPI 2.3.0
- **即时通讯**: WebSocket
- **AI服务**: OpenAI兼容接口调用（仅解析，不执行）

**详细文档**: [后端架构](./backend/architecture.md) | [后端功能](./backend/features.md)

## 核心特性

### AI服务集成

**设计理念**：
- **服务端负责AI解析**：接收用户自然语言输入，调用AI模型解析意图和参数
- **客户端负责执行操作**：根据解析结果，调用公共业务服务执行实际操作

**优势**：
- 操作与现有消息系统复用（发送消息走同一套流程）
- 本地SQLite正确存储
- 消息状态管理统一
- API密钥仅在服务端存储
- 新增Tool只需实现ToolExecutor接口即可自动注册

**核心流程**：
```
用户输入 → 服务端AI解析 → 返回tool_calls → 客户端执行 → EventBus通知
```

### 消息发送链路

```
Flutter客户端
  ↓ 生成临时消息(状态=0)
  ↓ HTTP POST /chat/messages
  ↓ 更新消息状态为1
  ↓ SQLite本地存储
  ↓ WebSocket通知对方
  ↓
SpringBoot服务端
  ↓ ChatService.sendMessage()
  ↓ MessageMapper.insert()
  ↓ WebSocket推送消息
```

## 快速开始

### 前端启动
```bash
cd lin_xin
flutter pub get
flutter run
```

### 后端启动
```bash
cd lin_xin_mgr
mvn clean install
mvn spring-boot:run
```

### 数据库初始化
- 创建MySQL数据库
- 配置application-dev.yml中的数据库连接
- Flyway自动执行迁移脚本

## API文档

启动后端后访问：`http://localhost:9099/lxa/doc.html`

## 部署架构

**MVP阶段（单服务器 2核2G）**
- SpringBoot: 9099端口
- MySQL: 3306端口
- Context Path: /lxa

## 后续规划

- [ ] 消息搜索功能优化
- [ ] 文件/图片/语音消息支持
- [ ] 消息撤回功能
- [ ] 群组管理功能增强
- [ ] 服务端Go迁移（产品表现良好后）

## 相关文档

- [MVP设计文档](../doc/mvp.md)
- [前端架构文档](./frontend/architecture.md)
- [前端功能文档](./frontend/features.md)
- [后端架构文档](./backend/architecture.md)
- [后端功能文档](./backend/features.md)
- [代码分析报告](./code_analysis_report.md)

---

## 版本历史

### v1.0.1 (2026-03-31)

#### 🐛 BUG修复

**后端修复（6个）**:
- ✅ WebSocket消息队列未消费（严重）- 用户上线时自动发送离线消息
- ✅ 群消息接收者ID未设置（严重）- 保证群消息数据完整性
- ✅ 好友关系物理删除（严重）- 改为逻辑删除，保持数据一致性
- ✅ 会话未读数未考虑静音状态（中等）- 静音会话不增加未读数
- ✅ AI服务历史记录内存泄漏（轻微）- 添加定时清理任务
- ✅ JWT Token过期时间单位不明确（轻微）- 添加明确的单位转换方法

**前端修复（2个）**:
- ✅ MessageService单例模式问题（中等）- 使用标准Dart单例模式
- ✅ WebSocket重连延迟计算错误（中等）- 优化重连延迟算法

#### 📝 改进说明

1. **系统稳定性**: WebSocket离线消息不会丢失，重连策略更合理
2. **数据完整性**: 群消息有完整的接收者信息，好友删除使用逻辑删除
3. **用户体验**: 静音会话不再增加未读数，重连延迟更合理
4. **内存管理**: AI服务自动清理过期历史记录，防止内存泄漏
5. **代码质量**: 单例模式规范化，JWT过期时间单位更明确

#### 📊 修复统计

- **修复文件数**: 8个
- **修复BUG数**: 8个
- **高优先级**: 3个 ✅
- **中等优先级**: 3个 ✅
- **轻微优先级**: 2个 ✅

**详细修复内容**:
- [后端功能文档 - 版本历史](./backend/features.md#版本历史)
- [前端功能文档 - 版本历史](./frontend/features.md#版本历史)

---

### v1.0.2 (2026-03-31)

#### 🐛 BUG修复

**后端修复（2个）**:
- ✅ WebSocket离线消息丢失（严重）- 修复sendMessageToUser方法，确保离线消息正确入队
- ✅ broadcastMessage双重序列化问题（中等）- 移除重复序列化，直接传递原始对象

**前端修复（1个）**:
- ✅ WebSocketService单例模式不规范（轻微）- 使用标准Dart单例模式，与MessageService保持一致

#### 📝 改进说明

1. **消息可靠性**: WebSocket离线消息现在会正确加入队列，不再丢失
2. **代码质量**: 避免了双重序列化问题，代码更清晰
3. **代码一致性**: 前后端单例模式都使用标准方式

#### 📊 修复统计

- **修复文件数**: 3个
- **修复BUG数**: 3个
- **高优先级**: 1个 ✅
- **中等优先级**: 1个 ✅
- **轻微优先级**: 1个 ✅

**详细修复内容**:
- [后端功能文档 - 版本历史](./backend/features.md#v102-2026-03-31)
- [前端功能文档 - 版本历史](./frontend/features.md#v102-2026-03-31)
