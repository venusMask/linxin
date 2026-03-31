# 灵信 (LinXin)

一个基于 Flutter 和 Spring Boot 开发的 AI 原生即时通讯应用。

## 项目概述

灵信是一款新一代 AI 原生即时通讯软件，用户通过自然语言与 AI 交互即可完成消息发送、信息总结、定时提醒等操作。

## 项目结构

```
linxin/
├── linxin-client/          # Flutter 客户端（支持多平台）
├── linxin-server/          # Spring Boot 服务端
└── claude/                 # 项目文档
    ├── README.md           # 项目详细文档
    ├── backend/            # 后端架构和功能文档
    └── frontend/           # 前端架构和功能文档
```

## 核心功能

- 用户认证（注册/登录/JWT Token）
- 好友管理（添加/删除/申请处理）
- 一对一聊天
- 群聊（500人以下）
- 消息已读未读状态
- 消息搜索
- AI 服务集成（服务端解析 + 客户端执行）

## 技术栈

### 客户端
- Flutter 3.10.8+ (Dart)
- Dio 5.9.2（HTTP）
- WebSocket（实时通信）
- SQLite (sqflite 2.4.1)

### 服务端
- Spring Boot 3.4.2 (Java 17)
- MyBatis-Plus 3.5.10.1
- MySQL 8.x + Druid 1.2.24
- Flyway 11.10.5（数据库迁移）
- Spring Security + JWT
- Knife4j 4.5.0（API 文档）

## 快速开始

### 客户端启动
```bash
cd linxin-client
flutter pub get
flutter run
```

### 服务端启动
```bash
cd linxin-server
mvn clean install
mvn spring-boot:run
```

## 文档

- [项目详细文档](claude/README.md)
- [前端架构](claude/frontend/architecture.md)
- [前端功能](claude/frontend/features.md)
- [后端架构](claude/backend/architecture.md)
- [后端功能](claude/backend/features.md)

## 许可证

本项目采用非商业许可证，详见 [LICENSE](LICENSE) 文件。

## 作者

venusMask
