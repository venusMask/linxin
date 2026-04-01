# MVP Bug Fix 进度

**日期:** 2026-04-01
**计划文件:** `docs/superpowers/plans/2026-04-01-mvp-bugfix.md`
**基础 commit:** `1417aad`

---

## 已完成 (14/14)

| Task | 内容 | Commit |
|------|------|--------|
| Task 1 | DPUserDetail 实现 UserDetails 接口 | d036818 |
| Task 2 | DPUserDetailLoginService 从数据库加载用户 | d0f1e39 |
| Task 3 | JwtAuthenticationFilter 注入真实 UserDetails + 写入 userId 请求属性 | cf629e1 |
| Task 4 | 所有 Controller 替换 @RequestHeader → @RequestAttribute；register 返回 UserVO；新增 UpdateAnnouncementRequest | 8cd30aa |
| Task 5 | WebSocketConfig 改为从配置读取 allowed-origins，dev/pro 环境分别配置；修复空值和空格边界问题 | 68e78cf, 08b87a6 |
| Task 6 | WebSocketHandler 有界队列 + 过期机制实现 | current |
| Task 7 | GroupServiceImpl 权限检查逻辑修复 | current |
| Task 8 | ChatServiceImpl 群聊会话自动创建 + 原子更新 unreadCount | current |
| Task 9 | AIServiceImpl 移除重复工具定义，支持 implemented 标志 | current |
| Task 10 | DatabaseService 添加 clearUserData() 清理本地数据 | current |
| Task 11 | AuthService 异步 logout + 清除本地 SQLite 数据 | current |
| Task 12 | ChatDetailPage 修正 currentUserId 来源 | current |
| Task 13 | AIChatPage 修复执行过程中的竞态与内存泄漏 | current |
| Task 14 | ChatListPage 实现 WebSocket 消息的增量更新 | current |

---

## 待完成 (0/14)

---

## 优化与架构增强 (Optimization Phase)

| Task | 内容 | Status |
|------|------|--------|
| Task 15 | 引入 Provider 状态管理，重构 main.dart 与 AuthWrapper | Done |
| Task 16 | DataService 改造为 ChangeNotifier，支持响应式数据流 | Done |
| Task 17 | ChatListPage 重构，使用 context.watch 监听会话列表 | Done |
| Task 18 | AIChatPage 引入消息 ID 机制，彻底消除执行状态竞态 | Done |
| Task 19 | 统一核心页面的错误反馈 (SnackBar) 与 mounted 安全检查 | Done |

---

## 验证命令

```bash
# 后端编译
cd linxin-server && mvn compile -q

# 前端静态分析
cd linxin-client && flutter analyze
```
