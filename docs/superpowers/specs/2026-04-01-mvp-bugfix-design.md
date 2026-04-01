# LinXin MVP Bug Fix Design

**Date**: 2026-04-01
**Scope**: P0 + P1 fixes across backend (SpringBoot) and frontend (Flutter)
**Order**: Backend first, then frontend
**Strategy**: Group related fixes by module; each group leaves the system in a runnable state

---

## Fix Groups

### Group 1 — Authentication Module (Backend P0 + P1)

**Files**: `DPUserDetailLoginService.java`, `JwtAuthenticationFilter.java`, `SecurityConfig.java`, `AuthController.java`, `ChatController.java`, `FriendController.java`, `GroupController.java`, `AuthController.java`

**Problems addressed**:
- `userDetails` always null in `JwtAuthenticationFilter` → all users have no permissions
- API responses include `password` field (sensitive data leak)
- All controllers rely on `@RequestHeader("X-User-Id")` which can be forged

**Design**:

1. **`DPUserDetailLoginService.loadUserByUsername()`**: Query user from DB by username, return `DPUserDetail` with userId + authorities.

2. **`JwtAuthenticationFilter`**:
   - After JWT validation, call `userDetailsService.loadUserByUsername(username)` to load real UserDetails
   - Set `authentication.setPrincipal(userDetails)` with real authorities
   - Write `request.setAttribute("userId", userId)` for downstream use by controllers

3. **All Controllers**: Replace `@RequestHeader("X-User-Id") Long userId` with `@RequestAttribute("userId") Long userId` on every endpoint.

4. **`AuthController`**: Login and register return `UserVO` (no password field) instead of raw `User` entity.

---

### Group 2 — WebSocket Module (Backend P0 + P1)

**Files**: `WebSocketConfig.java`, `WebSocketHandler.java`, `application.yml`, `application-dev.yml`, `application-pro.yml`

**Problems addressed**:
- `setAllowedOrigins("*")` allows any origin (security risk)
- Unbounded `ConcurrentLinkedQueue` for offline messages → potential OOM
- No message expiry → stale messages accumulate forever

**Design**:

1. **`WebSocketConfig`**: Read allowed origins from `websocket.allowed-origins` config property. Dev: `http://localhost:*`, Pro: actual domain.

2. **`WebSocketHandler` queue**:
   - Replace `ConcurrentLinkedQueue<TextMessage>` with a wrapper holding `TextMessage` + `timestamp`
   - Max queue size: 200 per user (drop oldest when full)
   - On `sendPendingMessages`: skip messages older than 72 hours
   - On session close: schedule queue cleanup after 72 hours (or clear immediately if no reconnect expected)

---

### Group 3 — Business Logic Module (Backend P1)

**Files**: `GroupServiceImpl.java`, `ChatServiceImpl.java`, `GroupController.java`, `AIServiceImpl.java`, `default_tools.json`

**Problems addressed**:
- `GroupServiceImpl` permission check logic is inverted (owners cannot manage)
- Group messages skip members with no conversation record
- Unread count update is non-atomic (race condition)
- `@RequestBody String` in `GroupController.updateAnnouncement` is invalid
- AI tool definitions duplicated between code and JSON

**Design**:

1. **`GroupServiceImpl`**: Fix permission check to `operator.getRole() == 0` (only regular members are unauthorized). Admin=1, Owner=2 both have permission.

2. **`ChatServiceImpl` group message**: When iterating group members for unread count update, if a member has no conversation record, auto-create one before updating.

3. **`ChatServiceImpl` unread count**: Replace in-memory increment with native SQL:
   `UPDATE conversations SET unread_count = unread_count + 1 WHERE id = ? AND mute_status = 0`

4. **`GroupController`**: Add `UpdateAnnouncementRequest` class with `announcement` field. Change endpoint to use `@RequestBody UpdateAnnouncementRequest`.

5. **AI Tools**: Remove hardcoded tool list from `AIServiceImpl.getDefaultTools()`. Load exclusively from `default_tools.json`. Add `"implemented": false` flag to `searchMessages` and `setReminder` tools in JSON.

---

### Group 4 — Frontend Fixes (Flutter P0 + P1)

**Files**: `chat_detail_page.dart`, `websocket_service.dart`, `auth_service.dart`, `chat_list_page.dart`, `ai_chat_page.dart`

**Problems addressed**:
- `currentUserId` is null → all messages show wrong direction (isMe always false)
- Loading message ID uses `_messages.length - 1` → race condition in AI chat
- `_reconnectAttempts` never resets on successful reconnect
- 4 `StreamController`s never closed → memory leak
- `logout()` doesn't await `_clearAuthData()` → stale token risk
- `logout()` doesn't clear SQLite → multi-account data leakage
- `_loadChats()` full reload on every new message → performance issue
- `TextEditingController` leak in `_showModifyDialog()`

**Design**:

1. **`chat_detail_page.dart` — currentUserId**: Read from `AuthService.instance.currentUser` in `initState()`. Use `id.toString()` with null safety guard.

2. **`chat_detail_page.dart` — loading message ID**: Replace `'${_messages.length - 1}'` with `DateTime.now().microsecondsSinceEpoch.toString()` as a stable, unique key. Store as local variable before the async gap.

3. **`websocket_service.dart` — reconnect counter**: Add `_reconnectAttempts = 0` in the success callback of `_doConnect()`.

4. **`websocket_service.dart` — StreamController lifecycle**: In `dispose()`, call `.close()` on all 4 StreamControllers before setting `_instance` state.

5. **`auth_service.dart` — logout**:
   - Add `await` before `_clearAuthData()`
   - After clearing auth data, call `DbService.instance.clearUserData()` to wipe messages and conversations tables

6. **`db_service.dart`**: Add `clearUserData()` method that deletes all rows from `messages` and `conversations` tables.

7. **`chat_list_page.dart` — incremental update**: On WebSocket `new_message` event, find the matching chat in `_chats` by `conversationId` and update only that item's `lastMessage` + `unreadCount`, instead of calling `_loadChats()`.

8. **`ai_chat_page.dart` — TextEditingController leak**: Wrap `showDialog` in try/finally, call `controller.dispose()` in the `finally` block.

---

## Constraints

- No new dependencies introduced (backend or frontend)
- No database schema changes required
- Each group can be applied and tested independently
- P2 items (state management framework, data model refactor, Redis) are out of scope

---

## Testing Checklist

### Group 1
- [ ] Login returns user info without password field
- [ ] Register returns user info without password field
- [ ] Authenticated endpoints reject requests without JWT
- [ ] Group admin operations work for both admin and owner roles

### Group 2
- [ ] WebSocket rejects connections from non-whitelisted origins
- [ ] Offline messages are delivered on reconnect
- [ ] Queue does not exceed 200 messages per user
- [ ] Messages older than 72h are discarded

### Group 3
- [ ] Group owner can add/remove members
- [ ] Group admin can add/remove members
- [ ] Regular member cannot add/remove members
- [ ] Group message creates conversation for members who lack one
- [ ] Unread count increments correctly under concurrent sends
- [ ] Update announcement endpoint accepts JSON body

### Group 4
- [ ] Sent messages appear on the right side (isMe = true)
- [ ] Received messages appear on the left side (isMe = false)
- [ ] AI chat loading indicator updates correctly
- [ ] WebSocket reconnects with delay reset after success
- [ ] Logout clears all local message data
- [ ] Chat list updates without full reload on new message
- [ ] Modify dialog does not leak TextEditingController
