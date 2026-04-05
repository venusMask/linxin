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

  Future<void> _handleIncomingMessage(dynamic messageData) async {
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
    final senderUserType = messageData['userType'] as int? ?? 0;
    final currentUserId = AuthService().currentUser?.id.toString();

    // 如果是 AI 代发的，且发送者是自己，则标记为 isMe
    bool isMe = (senderId == currentUserId);

    var chat = getChatById(conversationId);
    if (chat == null) {
      // 尝试通过发送者或接收者 ID 创建会话
      final peerId = isMe ? messageData['receiverId']?.toString() : senderId;
      
      // 如果发送者是系统AI
      if (senderUserType == 1) {
        // AI 助手的特殊处理
        chat = createChat(Friend(
          id: peerId!,
          name: messageData['senderNickname']?.toString() ?? 'AI 助手',
          avatar: messageData['senderAvatar']?.toString() ?? '',
          userType: 1,
        ));
        // 更新会话 ID 为服务器返回的 ID
        final index = _chats.indexWhere((c) => c.friend?.userType == 1);
        if (index != -1) {
          _chats[index] = _chats[index].copyWith(id: conversationId);
          chat = _chats[index];
        }
      } else if (peerId != null) {
        await refreshFriends();
        final friend = getFriendById(peerId);
        if (friend != null) {
          chat = createChat(friend);
          final index = _chats.indexWhere((c) => c.friend?.id == peerId);
          if (index != -1) {
            _chats[index] = _chats[index].copyWith(id: conversationId);
            chat = _chats[index];
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
        sequenceId: messageData['sequenceId'] as int?,
      );
      
      await _messageLocalService.saveMessage(message);

      // 更新会话列表状态
      addMessage(
        conversationId,
        message.content,
        now,
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
      final int? seqId = messageData['sequenceId'] as int?;
      if (seqId != null) {
        await _updateLastSequenceId(seqId);
      }
    }
  }

  Future<void> _handleIncomingGroupMessage(dynamic messageData) async {
    final groupId = messageData['groupId']?.toString();
    final messageId = messageData['id']?.toString();
    if (groupId == null || messageId == null) return;

    // 幂等处理
    final existingMessage = await _messageLocalService.getMessageById(messageId);
    if (existingMessage != null) {
      // 如果已存在，仍尝试更新 sequenceId 以防同步遗漏
      final int? seqId = messageData['sequenceId'] as int?;
      if (seqId != null) {
        await _updateLastSequenceId(seqId);
      }
      return;
    }

    final conversationId = messageData['conversationId']?.toString() ?? 'group_$groupId';
    var chat = getChatById(conversationId);
    if (chat == null) {
      debugPrint('Receive group message but chat is missing: $groupId');
      // 可以创建临时临时会话
      chat = Chat(
        id: conversationId,
        messages: [],
        lastTime: DateTime.now(),
        unreadCount: 0,
      );
      _chats.add(chat);
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
      sequenceId: messageData['sequenceId'] as int?,
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
    final int? seqId = messageData['sequenceId'] as int?;
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
      
      // 加载本地联系人
      final localFriends = await _friendLocalService.getAllFriends();
      _friends.clear();
      _friends.addAll(localFriends);
      
      notifyListeners();
      debugPrint('DataService initialized, loaded ${localFriends.length} friends, lastMsgSeqId: $_lastSequenceId');
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
        final int? seqId = json['sequenceId'] as int?;
        if (seqId != null && seqId > maxSeqId) {
          maxSeqId = seqId;
        }

        // 区分私聊和群聊
        final int? conversationType = json['conversationType'] as int?;
        if (conversationType == 1) { // 群聊
          await _handleIncomingGroupMessage(json);
        } else {
          await _handleIncomingMessage(json);
        }
      }

      await _updateLastSequenceId(maxSeqId);
      debugPrint('Sync ${data.length} messages, new lastSequenceId: $_lastSequenceId');
    } catch (e) {
      debugPrint('Sync messages failed: $e');
    }
  }

  // 增量同步好友列表
  Future<void> syncFriends() async {
    try {
      final lastSeqId = await _friendLocalService.getMaxSequenceId();
      final response = await HttpService().get('/friends/sync', queryParameters: {
        'lastSequenceId': lastSeqId,
      });

      final List<dynamic> data = response.data ?? [];
      if (data.isEmpty) return;

      for (var json in data) {
        final friend = Friend.fromJson(json);
        final deleted = json['deleted'] as int? ?? 0;
        
        if (deleted == 1) {
          await _friendLocalService.deleteFriend(friend.id);
        } else {
          await _friendLocalService.saveFriend(friend);
        }
      }

      // 同步完成后重新加载内存列表
      final updatedFriends = await _friendLocalService.getAllFriends();
      _friends.clear();
      _friends.addAll(updatedFriends);
      notifyListeners();
      
      debugPrint('Sync ${data.length} friends, new max sequenceId: ${await _friendLocalService.getMaxSequenceId()}');
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

  Friend? getFriendById(String id) {
    try {
      return _friends.firstWhere((friend) => friend.id == id);
    } catch (e) {
      return null;
    }
  }

  Friend? getAiAssistant() {
    try {
      return _friends.firstWhere((f) => f.userType == 1);
    } catch (e) {
      // 如果好友列表里没同步到，看看会话列表里有没有
      try {
        return _chats.firstWhere((c) => c.friend?.userType == 1).friend;
      } catch (e) {
        return null;
      }
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
          (chat) => chat.friend != null && chat.friend!.id == friendId);
    } catch (e) {
      return null;
    }
  }

  Chat createChat(Friend friend) {
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
    notifyListeners();
    return newChat;
  }

  void addMessage(String chatId, String content, DateTime time) {
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
      
      notifyListeners();
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
