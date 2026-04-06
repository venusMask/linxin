import 'package:flutter/foundation.dart';
import 'package:lin_xin/modules/chat/chat.dart';
import 'package:lin_xin/modules/contact/friend.dart';
import 'package:lin_xin/core/service/websocket_service.dart';
import 'package:lin_xin/core/service/http_service.dart';
import 'package:lin_xin/modules/auth/auth_service.dart';
import 'package:lin_xin/modules/chat/message.dart';
import 'package:lin_xin/core/service/db_service.dart';
import 'package:lin_xin/modules/chat/message_local_service.dart';
import 'package:lin_xin/core/service/event_bus.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:lin_xin/modules/contact/friend_local_service.dart';
import 'package:lin_xin/config/test_config.dart';

class DataService extends ChangeNotifier {
  static final DataService _instance = DataService._internal();
  factory DataService() => _instance;
  
  DataService._internal() {
    final db = DatabaseService();
    _messageLocalService = MessageLocalService(db);
    _friendLocalService = FriendLocalService(db);
    _initWebSocketListener();
  }

  late final MessageLocalService _messageLocalService;
  late final FriendLocalService _friendLocalService;

  void _initWebSocketListener() {
    // 监听 WebSocket 连接成功事件，自动触发增量同步
    WebSocketService.instance.connectStream.listen((_) {
      syncMessages();
      syncFriends();
    });

    WebSocketService.instance.friendEventStream.listen((event) {
      final type = event['type'];
      if (type == 'friend_apply') {
        refreshPendingApplyCount();
      } else if (type == 'friend_handle' || type == 'friend_delete') {
        syncFriends();
        refreshPendingApplyCount();
      }
    });

    WebSocketService.instance.messageStream.listen((event) {
      if (event['type'] == 'new_message') {
        _handleIncomingMessage(event['data']);
      }
    });

    WebSocketService.instance.groupMessageStream.listen((event) {
      if (event['type'] == 'group_message') {
        _handleIncomingGroupMessage(event['data']);
      }
    });
  }

  Future<void> _handleIncomingMessage(dynamic messageData, {bool notify = true}) async {
    final messageId = messageData['id']?.toString();
    if (messageId == null) return;

    // 幂等处理：检查消息是否已经存在
    final existingMessage = await _messageLocalService.getMessageById(messageId);
    if (existingMessage != null) return;

    final conversationId = messageData['conversationId']?.toString();
    if (conversationId == null) return;

    final senderId = messageData['senderId']?.toString();
    final senderType = messageData['senderType']?.toString();
    final isAi = messageData['isAi'] as bool? ?? messageData['is_ai'] as bool? ?? false;
    final currentUserId = AuthService().currentUser?.id.toString();

    // 如果是 AI 代发的，且发送者是自己，则标记为 isMe
    bool isMe = (senderId == currentUserId);

    var chat = getChatById(conversationId);
    if (chat == null) {
      // 尝试通过发送者或接收者 ID 匹配
      final peerId = isMe ? messageData['receiverId']?.toString() : senderId;
      
      if (peerId != null) {
        // 普通好友或 AI 助手（现在也是好友），尝试通过 friendId 找会话
        chat = getChatByFriendId(peerId);
        
        if (chat != null && chat.id.startsWith('chat_')) {
          // 如果本地会话 ID 是临时的，同步更新为后端真实的 conversationId
          final index = _chats.indexWhere((c) => c.id == chat!.id);
          _chats[index] = chat.copyWith(id: conversationId);
          chat = _chats[index];
        } else if (chat == null) {
          // 会话不存在，尝试刷新好友列表并自动创建
          await refreshFriends();
          final friend = getFriendById(peerId);
          if (friend != null) {
            chat = createChat(friend, notify: notify);
            final index = _chats.indexWhere((c) => c.friend?.friendId == peerId);
            if (index != -1) {
              _chats[index] = _chats[index].copyWith(id: conversationId);
              chat = _chats[index];
            }
          }
        }
      }
    }

    if (chat != null) {
      final now = DateTime.now();
      
      // 持久化到本地数据库
      final message = Message(
        id: messageId,
        conversationId: conversationId,
        senderId: senderId ?? '',
        content: messageData['content']?.toString() ?? '',
        messageType: messageData['messageType'] ?? 1,
        status: 1,
        createdAt: now,
        isRead: false,
        isMe: isMe,
        senderType: senderType,
        isAi: isAi,
        sequenceId: int.tryParse(messageData['sequenceId']?.toString() ?? ''),
      );
      
      await _messageLocalService.saveMessage(message);

      // 更新会话列表状态
      addMessage(
        conversationId,
        message.content,
        now,
        notify: notify,
      );

      // 通知当前打开的聊天页面
      EventBus.instance.emit(MessageReceivedEvent(
        conversationId: conversationId,
        messageId: message.id,
        content: message.content,
        senderId: message.senderId,
        createdAt: message.createdAt,
      ));

      // 更新 sequenceId
      final int? seqId = int.tryParse(messageData['sequenceId']?.toString() ?? '');
      if (seqId != null) {
        await _updateLastSequenceId(seqId);
      }
    }
  }

  Future<void> _handleIncomingGroupMessage(dynamic messageData, {bool notify = true}) async {
    final groupId = messageData['groupId']?.toString();
    final messageId = messageData['id']?.toString();
    if (groupId == null || messageId == null) return;

    // 幂等处理
    final existingMessage = await _messageLocalService.getMessageById(messageId);
    if (existingMessage != null) {
      // 如果已存在，仍尝试更新 sequenceId 以防同步遗漏
      final int? seqId = int.tryParse(messageData['sequenceId']?.toString() ?? '');
      if (seqId != null) {
        await _updateLastSequenceId(seqId);
      }
      return;
    }

    final conversationId = messageData['conversationId']?.toString() ?? 'group_$groupId';
    var chat = getChatById(conversationId);
    if (chat == null) {
      debugPrint('Receive group message but chat is missing: $groupId');
      // 创建临时会话
      chat = Chat(
        id: conversationId,
        type: ChatType.group,
        messages: [],
        lastTime: DateTime.now(),
        unreadCount: 0,
      );
      _chats.add(chat);
      if (notify) notifyListeners();
    }

    final senderId = messageData['senderId']?.toString();
    final currentUserId = AuthService().currentUser?.id.toString();
    bool isMe = (senderId == currentUserId);

    final now = DateTime.now();

    final message = Message(
      id: messageId,
      conversationId: conversationId,
      senderId: senderId ?? '',
      content: messageData['content']?.toString() ?? '',
      messageType: messageData['messageType'] ?? 1,
      status: 1,
      createdAt: now,
      isRead: false,
      isMe: isMe,
      sequenceId: int.tryParse(messageData['sequenceId']?.toString() ?? ''),
      senderType: messageData['senderType']?.toString(),
      isAi: messageData['isAi'] as bool? ?? messageData['is_ai'] as bool? ?? false,
      senderNickname: messageData['senderNickname']?.toString() ?? '用户',
      senderAvatar: messageData['senderAvatar']?.toString() ?? '',
    );

    // 持久化到本地数据库
    await _messageLocalService.saveMessage(message);

    addMessage(
      conversationId,
      message.content,
      now,
      notify: notify,
    );

    // 通知群聊页面
    EventBus.instance.emit(MessageReceivedEvent(
      conversationId: conversationId,
      messageId: message.id,
      content: message.content,
      senderId: message.senderId,
      createdAt: message.createdAt,
    ));

    // 更新 sequenceId
    final int? seqId = int.tryParse(messageData['sequenceId']?.toString() ?? '');
    if (seqId != null) {
      await _updateLastSequenceId(seqId);
    }
  }

  final List<Friend> _friends = [];
  final List<Chat> _chats = [];
  int _pendingApplyCount = 0;
  int _lastSequenceId = 0;

  List<Friend> get friends => List.unmodifiable(_friends);
  List<Chat> get chats => List.unmodifiable(_chats);
  int get pendingApplyCount => _pendingApplyCount;

  // 更新并持久化 lastSequenceId
  Future<void> _updateLastSequenceId(int newId) async {
    if (newId > _lastSequenceId) {
      _lastSequenceId = newId;
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt('last_sequence_id', _lastSequenceId);
      debugPrint('Updated lastSequenceId to: $_lastSequenceId');
    }
  }

  // 初始化从本地数据库加载
  Future<void> initialize() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      _lastSequenceId = prefs.getInt('last_sequence_id') ?? 0;
      
      // 加载本地联系人并去重
      final localFriends = await _friendLocalService.getAllFriends();
      _friends.clear();
      final Map<String, Friend> uniqueFriends = {};
      for (var f in localFriends) {
        uniqueFriends[f.friendId] = f;
      }
      _friends.addAll(uniqueFriends.values);
      
      notifyListeners();
      debugPrint('DataService initialized, loaded ${localFriends.length} friends, lastMsgSeqId: $_lastSequenceId');
      
      // 启动后立即拉取一次最新好友，不单纯依赖 WebSocket
      syncFriends();
    } catch (e) {
      debugPrint('Failed to initialize DataService: $e');
    }
  }

  // 增量同步消息
  Future<void> syncMessages() async {
    try {
      debugPrint('Starting syncMessages with lastSequenceId: $_lastSequenceId');
      final response = await HttpService().get('/chat/sync', queryParameters: {
        'lastSequenceId': _lastSequenceId,
      });

      final List<dynamic> data = response.data ?? [];
      if (data.isEmpty) {
        debugPrint('No new messages to sync');
        return;
      }

      int maxSeqId = _lastSequenceId;
      for (var json in data) {
        final int? seqId = int.tryParse(json['sequenceId']?.toString() ?? '');
        if (seqId != null && seqId > maxSeqId) {
          maxSeqId = seqId;
        }

        // 批量同步时，禁用单条消息的 notify 以提升性能
        final int? conversationType = json['conversationType'] as int?;
        if (conversationType == 1) { // 群聊
          await _handleIncomingGroupMessage(json, notify: false);
        } else {
          await _handleIncomingMessage(json, notify: false);
        }
      }

      await _updateLastSequenceId(maxSeqId);
      
      // 批量处理完成后，统一通知 UI 刷新
      notifyListeners();
      
      debugPrint('Sync ${data.length} messages, new lastSequenceId: $_lastSequenceId');
    } catch (e) {
      debugPrint('Sync messages failed: $e');
    }
  }

  // 增量同步好友列表
  Future<void> syncFriends() async {
    try {
      String? currentUserId = AuthService().currentUser?.id?.toString();
      
      // 兜底：如果内存中还没有，尝试从存储中获取
      if (currentUserId == null) {
        final prefs = await SharedPreferences.getInstance();
        currentUserId = prefs.getString('${TestConfig.storagePrefix}auth_user_id');
      }
      
      if (currentUserId == null) {
        debugPrint('Sync friends skipped: No current user ID available');
        return;
      }

      final lastSeqId = await _friendLocalService.getMaxSequenceId();
      debugPrint('Syncing friends for user $currentUserId from sequenceId: $lastSeqId');
      
      final response = await HttpService().get('/friends/sync', queryParameters: {
        'lastSequenceId': lastSeqId,
      });

      final List<dynamic> data = response.data ?? [];
      if (data.isEmpty) {
        debugPrint('No new friends to sync');
        return;
      }

      for (var json in data) {
        var friend = Friend.fromJson(json);
        // 强制确保 userId 是当前用户的，防止 Sqlite 归属错误
        if (friend.userId == null || friend.userId != currentUserId) {
          friend = friend.copyWith(userId: currentUserId);
        }

        final deleted = json['deleted'] as int? ?? 0;
        
        if (deleted == 1) {
          await _friendLocalService.deleteFriend(friend.id);
        } else {
          await _friendLocalService.saveFriend(friend);
        }
      }

      // 同步完成后重新加载内存列表并去重
      final updatedFriends = await _friendLocalService.getAllFriends();
      _friends.clear();
      
      // 使用 Map 进行去重，以 friendId 为键
      final Map<String, Friend> uniqueFriends = {};
      for (var f in updatedFriends) {
        uniqueFriends[f.friendId] = f;
      }
      _friends.addAll(uniqueFriends.values);
      notifyListeners();
      
      debugPrint('Sync ${data.length} friends success, total unique: ${_friends.length}');
    } catch (e) {
      debugPrint('Sync friends failed: $e');
    }
  }

  // 刷新所有好友并更新列表
  Future<void> refreshFriends() async {
    try {
      await syncFriends();
    } catch (e) {
      debugPrint('Refresh friends failed: $e');
    }
  }

  void initMockData() {
    _friends.clear();
    _chats.clear();
    notifyListeners();
  }

  void updateFriends(List<Friend> newFriends) {
    _friends.clear();
    _friends.addAll(newFriends);
    notifyListeners();
  }

  Future<void> refreshPendingApplyCount() async {
    try {
      final applies = await HttpService().getReceivedApplies();
      // 过滤出未读或未处理的申请 (此处简单处理，统计所有待处理状态的)
      // 假设后端返回的列表中，status 为 0 (PENDING) 的需要统计
      int count = 0;
      for (var apply in applies) {
        if (apply['status'] == 0) { // ApplyStatus.PENDING
          count++;
        }
      }
      _pendingApplyCount = count;
      notifyListeners();
    } catch (e) {
      debugPrint('Refresh pending apply count failed: $e');
    }
  }

  void updateChats(List<Chat> newChats) {
    _chats.clear();
    _chats.addAll(newChats);
    notifyListeners();
  }

  Friend? getAiAssistant() {
    // 1. 优先从好友列表中找
    try {
      return _friends.firstWhere((f) => f.userType == 1);
    } catch (e) {
      // 2. 好友列表没同步到，尝试从会话列表里提取
      try {
        return _chats.firstWhere((c) => c.friend?.userType == 1).friend;
      } catch (e) {
        return null;
      }
    }
  }

  Friend? getFriendById(String id) {
    try {
      return _friends.firstWhere((friend) => friend.id == id);
    } catch (e) {
      return null;
    }
  }

  Chat? getChatById(String id) {
    try {
      return _chats.firstWhere((chat) => chat.id == id);
    } catch (e) {
      return null;
    }
  }

  Chat? getChatByFriendId(String friendId) {
    try {
      return _chats.firstWhere(
          (chat) => chat.friend != null && chat.friend!.friendId == friendId);
    } catch (e) {
      return null;
    }
  }

  Chat createChat(Friend friend, {bool notify = true}) {
    final existingChat = getChatByFriendId(friend.id);
    if (existingChat != null) {
      return existingChat;
    }

    final newChat = Chat(
      id: 'chat_${DateTime.now().millisecondsSinceEpoch}',
      friend: friend,
      messages: [],
      lastTime: DateTime.now(),
      unreadCount: 0,
    );
    _chats.add(newChat);
    if (notify) notifyListeners();
    return newChat;
  }

  void addMessage(String chatId, String content, DateTime time, {bool notify = true}) {
    final chat = getChatById(chatId);
    if (chat != null) {
      final index = _chats.indexWhere((c) => c.id == chatId);
      _chats[index] = chat.copyWith(
        lastMessage: content,
        lastTime: time,
        unreadCount: chat.unreadCount + 1,
      );
      
      // Move to top
      if (index > 0) {
        final updatedChat = _chats.removeAt(index);
        _chats.insert(0, updatedChat);
      }
      
      if (notify) notifyListeners();
    }
  }

  void markAsRead(String chatId) {
    final index = _chats.indexWhere((chat) => chat.id == chatId);
    if (index != -1) {
      _chats[index] = _chats[index].copyWith(unreadCount: 0);
      notifyListeners();
    }
  }

  void updateChatLastTime(String chatId, DateTime time) {
    final index = _chats.indexWhere((chat) => chat.id == chatId);
    if (index != -1) {
      _chats[index] = _chats[index].copyWith(lastTime: time);
      notifyListeners();
    }
  }
}
