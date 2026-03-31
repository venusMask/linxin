# 前端架构文档

## 技术栈

### 核心框架
- **Flutter**: 3.10.8+
- **Dart**: 3.10.8+

### 状态管理
- **ChangeNotifier**: Flutter内置状态管理
- **AuthService**: 认证状态管理

### 网络通信
- **Dio**: 5.9.2 - HTTP客户端
- **WebSocket**: 实时通信

### 本地存储
- **SQLite (sqflite)**: 2.4.1 - 本地数据库
- **shared_preferences**: 2.3.5 - 轻量级存储
- **path_provider**: 2.1.5 - 路径管理

### 日志
- **logger**: 2.7.0 - 日志记录

### UI组件
- **Material Design**: Material 3设计规范
- **cupertino_icons**: 1.0.8 - iOS风格图标

## 项目结构

```
lin_xin/
├── lib/
│   ├── main.dart                       # 应用入口
│   │
│   ├── config/                         # 配置
│   │   └── api_config.dart            # API配置
│   │
│   ├── models/                         # 数据模型
│   │   ├── user.dart                  # 用户模型
│   │   ├── friend.dart                # 好友模型
│   │   ├── message.dart               # 消息模型
│   │   ├── chat.dart                  # 会话模型
│   │   ├── group.dart                 # 群组模型
│   │   └── group_member.dart          # 群成员模型
│   │
│   ├── services/                       # 服务层
│   │   ├── ai_service.dart            # AI客户端服务
│   │   ├── ai_intent_service.dart     # AI意图服务
│   │   ├── tool_executor.dart         # 工具执行器接口
│   │   │
│   │   ├── executors/                 # 工具执行器实现
│   │   │   ├── send_message_executor.dart
│   │   │   └── add_friend_executor.dart
│   │   │
│   │   ├── message_service.dart       # 公共消息服务
│   │   ├── friend_service.dart        # 公共好友服务
│   │   ├── group_service.dart         # 群组服务
│   │   ├── event_bus.dart             # 事件总线
│   │   ├── auth_service.dart          # 认证服务
│   │   ├── http_service.dart          # HTTP请求
│   │   ├── websocket_service.dart     # WebSocket服务
│   │   ├── db_service.dart            # SQLite数据库
│   │   ├── message_local_service.dart # 消息本地存储
│   │   ├── data_service.dart          # 数据服务
│   │   └── log_service.dart           # 日志服务
│   │
│   ├── pages/                          # 页面
│   │   ├── ai_chat_page.dart          # AI对话页面
│   │   ├── chat_detail_page.dart      # 聊天详情页
│   │   ├── chat_list_page.dart        # 会话列表页
│   │   ├── friend_list_page.dart      # 好友列表页
│   │   ├── friend_apply_list_page.dart # 好友申请列表页
│   │   ├── create_group_page.dart     # 创建群组页
│   │   ├── group_settings_page.dart   # 群设置页
│   │   ├── login_page.dart            # 登录页
│   │   ├── register_page.dart         # 注册页
│   │   ├── main_page.dart             # 主页
│   │   ├── profile_page.dart          # 个人资料页
│   │   ├── user_details_page.dart     # 用户详情页
│   │   └── user_search_result_page.dart # 用户搜索结果页
│   │
│   ├── widgets/                        # 组件
│   │   ├── message_bubble.dart        # 消息气泡
│   │   └── avatar_widget.dart         # 头像组件
│   │
│   └── examples/                       # 示例
│       └── log_example.dart           # 日志示例
│
├── test/                               # 测试
│   └── widget_test.dart
│
├── android/                            # Android平台
├── ios/                                # iOS平台
├── web/                                # Web平台
├── linux/                              # Linux平台
├── macos/                              # macOS平台
├── windows/                            # Windows平台
│
├── pubspec.yaml                        # 依赖配置
├── analysis_options.yaml               # 代码分析配置
└── README.md                           # 项目说明
```

## 架构设计

### 分层架构

```
┌─────────────────────────────────────────┐
│              UI Layer (Pages)            │
│         页面展示、用户交互               │
├─────────────────────────────────────────┤
│          Service Layer (Services)        │
│      业务逻辑、网络请求、本地存储         │
├─────────────────────────────────────────┤
│           Data Layer (Models)            │
│          数据模型、状态管理               │
└─────────────────────────────────────────┘
```

### 状态管理

#### AuthService - 认证状态管理
```dart
class AuthService extends ChangeNotifier {
  bool _isLoggedIn = false;
  bool _isInitialized = false;
  String? _token;
  User? _currentUser;
  
  // 登录状态
  bool get isLoggedIn => _isLoggedIn;
  
  // 初始化状态
  bool get isInitialized => _isInitialized;
  
  // 当前用户
  User? get currentUser => _currentUser;
  
  // 登录
  Future<void> login(String username, String password);
  
  // 注册
  Future<void> register(String username, String password, String nickname);
  
  // 登出
  Future<void> logout();
  
  // 初始化
  Future<void> initialize();
}
```

### 服务层设计

#### HTTP服务 (http_service.dart)
- 封装Dio HTTP客户端
- 统一请求/响应处理
- Token自动添加
- 错误统一处理

#### WebSocket服务 (websocket_service.dart)
- WebSocket连接管理
- 消息实时推送
- 心跳机制
- 断线重连

#### 数据库服务 (db_service.dart)
- SQLite数据库管理
- 表结构创建
- CRUD操作封装

#### 消息本地服务 (message_local_service.dart)
- 消息本地存储
- 会话本地管理
- 离线消息缓存

### 事件总线

#### EventBus - 事件通知机制
```dart
class EventBus {
  // 事件类型
  static const MessageSentEvent = 'message_sent';
  static const FriendAppliedEvent = 'friend_applied';
  static const GroupCreatedEvent = 'group_created';
  static const ConversationUpdatedEvent = 'conversation_updated';
  
  // 监听事件
  static void on<T>(Function(T) handler);
  
  // 发送事件
  static void emit<T>(T event);
  
  // 取消监听
  static void off<T>(Function(T) handler);
}
```

## AI服务架构

### 设计理念
- **客户端负责执行操作**
- 根据服务端解析结果执行实际操作
- 调用公共业务服务
- 本地SQLite正确存储
- 统一消息状态管理

### 核心流程

```
┌─────────────────────────────────────────┐
│          用户输入自然语言                 │
│      "给李四说晚上好，提醒明天开会"        │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        AIService.chat()                 │
│    调用服务端AI接口解析意图               │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        服务端返回解析结果                 │
│  - intent: 操作意图                      │
│  - toolCalls: 待执行工具列表              │
│  - aiText: AI回复文本                    │
│  - needConfirm: 是否需要确认              │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        展示待确认操作界面                 │
│    用户确认 / 修改 / 取消                 │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      AIIntentService.execute()          │
│    根据toolName查找执行器并执行           │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        ToolExecutor执行操作              │
│  - SendMessageExecutor                  │
│  - AddFriendExecutor                    │
│  - CreateGroupExecutor                  │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        EventBus通知页面刷新              │
│    MessageSentEvent → 更新消息列表        │
└─────────────────────────────────────────┘
```

### 注册机制

```dart
class AIIntentService {
  final Map<String, ToolExecutor> _executors = {};
  
  // 注册执行器
  void registerExecutor(ToolExecutor executor) {
    _executors[executor.toolName] = executor;
  }
  
  // 执行工具
  Future<void> execute(String toolName, Map<String, dynamic> params) async {
    final executor = _executors[toolName];
    if (executor != null) {
      await executor.execute(params);
    }
  }
  
  // 注册默认执行器
  void _registerDefaultExecutors() {
    registerExecutor(SendMessageExecutor());
    registerExecutor(AddFriendExecutor());
    // 新增执行器只需在此注册
  }
}
```

### 工具执行器接口

```dart
abstract class ToolExecutor {
  // 工具名称
  String get toolName;
  
  // 执行操作
  Future<void> execute(Map<String, dynamic> params);
  
  // 描述
  String get description;
}
```

### 已实现的执行器

#### 1. SendMessageExecutor
- 发送消息执行器
- 调用MessageService.sendMessage()
- 保存到SQLite
- 发送EventBus通知

#### 2. AddFriendExecutor
- 添加好友执行器
- 调用FriendService.applyFriend()
- 发送EventBus通知

## 数据模型

### User - 用户模型
```dart
class User {
  final String id;
  final String username;
  final String nickname;
  final String? avatar;
  final String? phone;
  final String? email;
  final int gender;
  final String? signature;
  final int status;
  final DateTime? lastLoginTime;
  final DateTime createTime;
}
```

### Message - 消息模型
```dart
class Message {
  final String id;
  final String conversationId;
  final String senderId;
  final String receiverId;
  final String? groupId;
  final int messageType;
  final String content;
  final Map<String, dynamic>? extra;
  final int sendStatus;
  final DateTime sendTime;
}
```

### Conversation - 会话模型
```dart
class Conversation {
  final String id;
  final String peerId;
  final String? peerNickname;
  final String? peerAvatar;
  final int type;
  final String? groupId;
  final String? lastMessageContent;
  final int? lastMessageType;
  final DateTime? lastMessageTime;
  final int unreadCount;
  final int topStatus;
  final int muteStatus;
}
```

### Friend - 好友模型
```dart
class Friend {
  final String id;
  final String userId;
  final String friendId;
  final String? friendNickname;
  final String friendGroup;
  final int status;
  final User? friendInfo;
}
```

### Group - 群组模型
```dart
class Group {
  final String id;
  final String name;
  final String? avatar;
  final String ownerId;
  final String? announcement;
  final int memberLimit;
  final int memberCount;
  final int status;
  final DateTime createTime;
}
```

## 本地存储策略

### SQLite数据库

#### 消息表 (messages)
- 存储所有聊天消息
- 支持离线消息缓存
- 消息状态本地管理

#### 会话表 (conversations)
- 存储会话列表
- 最后消息缓存
- 未读数统计

#### 用户表 (users)
- 缓存用户基本信息
- 减少网络请求

### SharedPreferences
- 存储JWT Token
- 存储用户ID
- 存储应用配置

## 网络通信

### HTTP请求
- 基础URL配置
- Token自动添加
- 请求/响应拦截器
- 错误统一处理
- 超时配置

### WebSocket通信
- 连接管理
- 消息推送
- 心跳检测
- 断线重连
- 消息队列

## 路由管理

### 页面路由
- 登录页 → 主页
- 主页（底部导航）:
  - 会话列表
  - 好友列表
  - 个人资料
- 会话列表 → 聊天详情
- 好友列表 → 好友申请列表
- 好友列表 → 创建群组
- 聊天详情 → 群设置

### 认证路由守卫
```dart
class AuthWrapper extends StatefulWidget {
  @override
  Widget build(BuildContext context) {
    if (_authService.isLoggedIn) {
      return const MainPage();
    } else {
      return const LoginPage();
    }
  }
}
```

## UI设计规范

### Material Design 3
- 使用Material 3设计规范
- 绿色主题色
- 圆角卡片设计
- 流畅动画过渡

### 组件复用
- AvatarWidget: 头像组件
- MessageBubble: 消息气泡
- 统一loading样式
- 统一错误提示

## 性能优化

### 列表优化
- ListView.builder懒加载
- 图片缓存
- 分页加载

### 内存优化
- 图片压缩
- 及时释放资源
- 避免内存泄漏

### 网络优化
- 请求缓存
- 批量请求
- 离线缓存

## 错误处理

### 网络错误
- 网络异常提示
- 重试机制
- 离线模式

### 业务错误
- 错误提示
- 异常捕获
- 日志记录

## 测试策略

### 单元测试
- 服务层测试
- 工具执行器测试
- 数据模型测试

### Widget测试
- UI组件测试
- 页面测试

### 集成测试
- 完整流程测试
- 端到端测试
