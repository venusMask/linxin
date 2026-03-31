# 灵信 (Lin Xin)

一个基于 Flutter 开发的即时通讯应用，参考微信的简洁设计风格。

## 项目介绍

灵信是一个跨平台的即时通讯应用，支持 Android、iOS、Web、Windows、macOS 和 Linux 平台。

### 核心功能

- 💬 **聊天列表**: 显示所有聊天会话，包含未读消息数
- 👥 **好友列表**: 显示所有好友，支持点击发起聊天
- 💭 **聊天详情**: 支持发送消息和自动回复功能
- 🔄 **消息状态**: 实时显示消息时间和未读状态

## 项目结构

```
lib/
├── main.dart                          # 应用入口
├── models/                            # 数据模型层
│   ├── friend.dart                    # 好友模型
│   ├── message.dart                   # 消息模型
│   └── chat.dart                      # 聊天会话模型
├── services/                          # 服务层
│   └── data_service.dart              # 数据管理服务
├── widgets/                           # 可复用组件
│   ├── avatar_widget.dart             # 头像组件
│   └── message_bubble.dart            # 消息气泡组件
└── pages/                             # 页面层
    ├── main_page.dart                 # 主页面（底部导航）
    ├── chat_list_page.dart            # 聊天列表页面
    ├── friend_list_page.dart          # 好友列表页面
    └── chat_detail_page.dart          # 聊天详情页面
```

## 技术栈

- **框架**: Flutter 3.10.8+
- **语言**: Dart
- **状态管理**: 原生 StatefulWidget
- **UI组件**: Material Design
- **数据管理**: 单例模式服务

## 如何运行

### 环境要求

- Flutter SDK 3.10.8 或更高版本
- Dart SDK 3.1.3 或更高版本
- 各平台开发工具（Android Studio、Xcode等）

### 运行步骤

1. **克隆项目**
   ```bash
   git clone <项目地址>
   cd lin_xin
   ```

2. **安装依赖**
   ```bash
   flutter pub get
   ```

3. **运行应用**
   - Android: `flutter run`
   - iOS: `flutter run`
   - Web: `flutter run -d chrome`
   - Windows: `flutter run -d windows`
   - macOS: `flutter run -d macos`
   - Linux: `flutter run -d linux`

## 功能演示

1. **聊天列表**
   - 显示所有聊天会话
   - 包含最后一条消息预览
   - 显示未读消息数量
   - 显示消息时间

2. **好友列表**
   - 显示所有好友
   - 点击好友进入聊天页面

3. **聊天详情**
   - 支持发送文本消息
   - 自动回复功能
   - 消息时间显示
   - 消息气泡样式

## 项目特点

- **跨平台**: 一套代码支持六大平台
- **模块化**: 清晰的代码结构和职责分离
- **可扩展性**: 易于添加新功能
- **用户友好**: 简洁的界面设计
- **代码质量**: 完整的注释和规范的代码风格

## 后续计划

- [ ] 消息持久化存储
- [ ] 语音消息功能
- [ ] 表情和图片发送
- [ ] 群组聊天
- [ ] 消息撤回
- [ ] 主题切换

## 许可证

MIT License

## 作者

灵信团队
