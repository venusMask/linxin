# 后端架构文档

## 技术栈

### 核心框架
- **SpringBoot**: 3.4.2
- **Java**: 17
- **Maven**: 项目构建工具

### 持久层
- **MyBatis-Plus**: 3.5.10.1 - ORM框架
- **MySQL**: 8.x - 关系型数据库
- **Druid**: 1.2.24 - 数据库连接池
- **Flyway**: 11.10.5 - 数据库版本管理

### 安全认证
- **Spring Security**: 安全框架
- **JWT (jjwt)**: 0.12.7 - Token认证

### API文档
- **Knife4j**: 4.5.0 - API文档增强
- **SpringDoc OpenAPI**: 2.3.0 - OpenAPI规范

### 其他组件
- **Jackson**: 2.18.4 - JSON处理
- **MapStruct**: 1.6.3 - 对象映射
- **WebSocket**: 即时通讯

## 项目结构

```
lin_xin_mgr/
├── src/main/java/org/venus/lin/xin/mgr/
│   ├── LinXinMgrApplication.java        # 主应用入口
│   │
│   ├── ai/                              # AI服务模块
│   │   ├── adapter/                     # AI模型适配器
│   │   │   ├── AIModelAdapter.java     # AI模型适配器接口
│   │   │   └── OpenAIAdapter.java      # OpenAI兼容适配器
│   │   ├── config/                      # AI配置
│   │   │   └── AIConfig.java
│   │   ├── controller/                  # AI接口（仅解析）
│   │   │   └── AIController.java
│   │   ├── dto/                         # 数据传输对象
│   │   │   ├── ChatRequest.java        # 聊天请求
│   │   │   ├── ChatResponse.java       # 聊天响应
│   │   │   ├── ExecuteRequest.java     # 执行请求
│   │   │   └── ModifyParamsRequest.java # 修改参数请求
│   │   ├── service/                     # AI业务逻辑（解析）
│   │   │   ├── AIService.java          # AI服务接口
│   │   │   └── impl/
│   │   │       └── AIServiceImpl.java  # AI解析实现
│   │   └── tools/                       # Tools配置模型
│   │       ├── AITool.java             # Tool定义模型
│   │       ├── ToolParam.java          # 参数定义模型
│   │       └── ToolsConfig.java        # Tools配置模型
│   │
│   ├── business/                        # 业务模块
│   │   ├── controller/                  # RESTful接口
│   │   │   ├── AuthController.java     # 认证控制器
│   │   │   ├── ChatController.java     # 聊天控制器
│   │   │   ├── FriendController.java   # 好友控制器
│   │   │   └── GroupController.java    # 群组控制器
│   │   │
│   │   ├── service/                     # 业务逻辑
│   │   │   ├── IUserService.java
│   │   │   ├── IFriendService.java
│   │   │   ├── IChatService.java
│   │   │   ├── IGroupService.java
│   │   │   └── impl/
│   │   │       ├── UserServiceImpl.java
│   │   │       ├── FriendServiceImpl.java
│   │   │       ├── ChatServiceImpl.java
│   │   │       └── GroupServiceImpl.java
│   │   │
│   │   ├── mapper/                      # 数据访问
│   │   │   ├── UserMapper.java
│   │   │   ├── FriendMapper.java
│   │   │   ├── FriendApplyMapper.java
│   │   │   ├── ConversationMapper.java
│   │   │   ├── MessageMapper.java
│   │   │   ├── MessageStatusMapper.java
│   │   │   ├── GroupMapper.java
│   │   │   └── GroupMemberMapper.java
│   │   │
│   │   ├── entity/                      # 实体类
│   │   │   ├── User.java
│   │   │   ├── Friend.java
│   │   │   ├── FriendApply.java
│   │   │   ├── Conversation.java
│   │   │   ├── Message.java
│   │   │   ├── MessageStatus.java
│   │   │   ├── Group.java
│   │   │   └── GroupMember.java
│   │   │
│   │   ├── vo/                          # 视图对象
│   │   │   ├── UserVO.java
│   │   │   ├── FriendVO.java
│   │   │   ├── FriendApplyVO.java
│   │   │   ├── ConversationVO.java
│   │   │   ├── MessageVO.java
│   │   │   ├── GroupVO.java
│   │   │   └── GroupMemberVO.java
│   │   │
│   │   ├── converter/                   # 对象转换器
│   │   │   ├── UserConverter.java
│   │   │   ├── FriendConverter.java
│   │   │   └── ChatConverter.java
│   │   │
│   │   └── model.request/               # 请求DTO
│   │       ├── UserLoginRequest.java
│   │       ├── UserRegisterRequest.java
│   │       ├── FriendApplyRequest.java
│   │       ├── FriendHandleRequest.java
│   │       ├── FriendListRequest.java
│   │       ├── FriendUpdateRequest.java
│   │       ├── SendMessageRequest.java
│   │       ├── CreateGroupRequest.java
│   │       └── AddGroupMembersRequest.java
│   │
│   ├── auth/                            # JWT认证
│   │   ├── AuthConfig.java
│   │   ├── AuthService.java
│   │   ├── DPUserDetail.java
│   │   ├── DPUserDetailLoginService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtService.java
│   │   └── SecurityConfig.java
│   │
│   ├── websocket/                       # WebSocket处理
│   │   ├── WebSocketConfig.java
│   │   ├── WebSocketHandler.java
│   │   ├── WebSocketInterceptor.java
│   │   └── WebSocketMessage.java
│   │
│   ├── config/                          # 配置类
│   │   ├── AsyncConfig.java            # 异步配置
│   │   ├── JacksonConfig.java          # JSON配置
│   │   ├── MybatisConfig.java          # MyBatis配置
│   │   ├── MybatisMetaObjectHandler.java # 字段自动填充
│   │   ├── OpenApiConfig.java          # API文档配置
│   │   └── PasswordConfig.java         # 密码配置
│   │
│   ├── common/                          # 通用工具
│   │   ├── constant/                   # 常量定义
│   │   │   ├── ApplyStatus.java       # 申请状态
│   │   │   ├── FriendStatus.java      # 好友状态
│   │   │   ├── MessageType.java       # 消息类型
│   │   │   └── SendStatus.java        # 发送状态
│   │   │
│   │   ├── exception/                  # 异常处理
│   │   │   ├── BusinessException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── result/                     # 统一响应
│   │       ├── Result.java
│   │       └── ResultCode.java
│   │
│   └── util/                            # 工具类
│       └── PasswordGenerator.java      # 密码生成器
│
├── src/main/resources/
│   ├── application.yml                  # 主配置文件
│   ├── application-dev.yml              # 开发环境配置
│   ├── application-pro.yml              # 生产环境配置
│   ├── logback-spring.xml               # 日志配置
│   │
│   ├── db/migration/mysql/              # 数据库迁移脚本
│   │   └── V1.0.0.1__init.sql          # 初始化脚本
│   │
│   └── tools/                           # AI工具配置
│       └── default_tools.json          # 默认工具配置
│
└── pom.xml                              # Maven配置
```

## 分层架构

### Controller层
- 接收HTTP请求
- 参数校验
- 调用Service层
- 返回统一响应格式

### Service层
- 业务逻辑处理
- 事务管理
- 调用Mapper层

### Mapper层
- 数据库访问
- SQL执行
- MyBatis-Plus增强

### Entity层
- 数据库表映射
- 字段定义

### VO层
- 视图对象
- 数据脱敏
- 格式转换

## 核心配置

### 服务端口
- 端口: 9099
- Context Path: /lxa

### 数据库配置
- MySQL 8.x
- Druid连接池
- Flyway版本管理

### 安全配置
- JWT Token认证
- Spring Security权限控制
- 密码BCrypt加密

### WebSocket配置
- 端点: /ws
- 消息推送
- 心跳检测

## 部署架构

### MVP阶段（单服务器）
```
┌─────────────────────────────────────┐
│      服务器 (2核2G)                  │
│                                     │
│  ┌──────────────┐  ┌─────────────┐ │
│  │ SpringBoot   │  │   MySQL     │ │
│  │   :9099      │  │   :3306     │ │
│  └──────────────┘  └─────────────┘ │
│                                     │
│  Context Path: /lxa                 │
└─────────────────────────────────────┘
```

### 配置文件说明
- `application.yml`: 主配置，公共配置
- `application-dev.yml`: 开发环境配置
- `application-pro.yml`: 生产环境配置

## AI服务架构

### 设计理念
- **服务端仅负责AI解析**
- 接收用户自然语言输入
- 调用AI模型解析意图和参数
- 返回解析结果，不执行操作

### 核心流程
```
用户输入 → AIController → AIService → OpenAIAdapter → AI模型
    ↓
返回解析结果（tool_calls）
    ↓
客户端执行实际操作
```

### 模块组成
1. **Adapter层**: AI模型适配器（支持OpenAI兼容接口）
2. **Service层**: AI解析逻辑
3. **Controller层**: AI接口暴露
4. **Tools配置**: 工具定义和参数配置

## 数据库设计

### 核心表
1. **users** - 用户表
2. **friends** - 好友关系表
3. **friend_apply** - 好友申请记录表
4. **conversations** - 会话表
5. **messages** - 消息表
6. **message_status** - 消息状态表
7. **groups** - 群组信息表
8. **group_members** - 群成员关系表

### 数据库迁移
- 使用Flyway管理数据库版本
- 迁移脚本位于 `db/migration/mysql/`
- 命名规范: `V{版本号}__{描述}.sql`

## 安全机制

### 认证流程
1. 用户登录 → 验证用户名密码
2. 生成JWT Token
3. 返回Token给客户端
4. 后续请求携带Token
5. JwtAuthenticationFilter验证Token

### 权限控制
- Spring Security配置
- 基于角色的访问控制
- 接口权限注解

### 数据安全
- 密码BCrypt加密
- 敏感信息脱敏
- SQL注入防护
- XSS防护
