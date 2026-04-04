import 'package:flutter/foundation.dart';
import '../models/chat.dart';
import '../models/friend.dart';
import '../services/websocket_service.dart';
import '../services/http_service.dart';
import '../services/auth_service.dart';
import '../config/api_config.dart';
import '../models/message.dart';
import '../services/db_service.dart';
import '../services/message_local_service.dart';
import '../services/event_bus.dart';

class DataService extends ChangeNotifier {
  static final DataService _instance = DataService._internal();
  factory DataService() => _instance;
  
  DataService._internal() {
    _messageLocalService = MessageLocalService(DatabaseService());
    _initWebSocketListener();
  }

  late final MessageLocalService _messageLocalService;

  void _initWebSocketListener() {
    // 监听 WebSocket 连接成功事件，自动触发增量同步
    WebSocketService.instance.connectStream.listen((_) {
      syncMessages();
    });

    WebSocketService.instance.friendEventStream.listen((event) {
      final type = event['type'];
      if (type == 'friend_apply') {
        refreshPendingApplyCount();
      } else if (type == 'friend_handle' || type == 'friend_delete') {
        refreshFriends();
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
        _handleIncomingIncomingGroupMessage(event['data']);
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
    final currentUserId = AuthService().currentUser?.id.toString();

    // 如果是 AI 代发的，且发送者是自己，则标记为 isMe
    bool isMe = (senderId == currentUserId);

    var chat = getChatById(conversationId);
    if (chat == null) {
      // 尝试通过发送者或接收者 ID 创建会话
      final peerId = isMe ? messageData['receiverId']?.toString() : senderId;
      if (peerId != null) {
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
    }
  }

  Future<void> _handleIncomingIncomingGroupMessage(dynamic messageData) async {
    final groupId = messageData['groupId']?.toString();
    if (groupId == null) return;
    
    // 群聊同理，需要检查本地是否有该群会话
    final conversationId = messageData['conversationId']?.toString() ?? 'group_$groupId';
    var chat = getChatById(conversationId);
    if (chat == null) {
      // 此处逻辑可后续完善：拉取群详情并创建会话
      debugPrint('Receive group message but chat is missing: $groupId');
      return;
    }

    addMessage(
      conversationId,
      messageData['content']?.toString() ?? '',
      DateTime.now(),
    );
  }

  final List<Friend> _friends = [];
  final List<Chat> _chats = [];
  int _pendingApplyCount = 0;
  int _lastSequenceId = 0;

  List<Friend> get friends => List.unmodifiable(_friends);
  List<Chat> get chats => List.unmodifiable(_chats);
  int get pendingApplyCount => _pendingApplyCount;

  // 增量同步消息
  Future<void> syncMessages() async {
    try {
      final response = await HttpService().get('/chat/sync', queryParameters: {
        'lastSequenceId': _lastSequenceId,
      });
      
      final List<dynamic> newMessages = response.data ?? [];
      if (newMessages.isEmpty) return;

      for (var msgData in newMessages) {
        await _handleIncomingMessage(msgData);
        final seqId = msgData['sequenceId'] as int?;
        if (seqId != null && seqId > _lastSequenceId) {
          _lastSequenceId = seqId;
        }
      }
      debugPrint('Sync ${newMessages.length} messages, lastSeqId: $_lastSequenceId');
    } catch (e) {
      debugPrint('Sync messages failed: $e');
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

  Future<void> refreshFriends() async {
    try {
      final response = await HttpService().post(
        ApiConfig.friendList,
        data: {
          'username': AuthService().currentUser?.username,
          'pageNum': 1,
          'pageSize': 200,
        },
      );
      final List<dynamic> records = response.data['records'] ?? [];
      final List<Friend> newFriends = records.map((json) => Friend(
        id: json['friendId']?.toString() ?? '',
        name: json['friendNickname'] ?? json['nickname'] ?? '未知用户',
        avatar: json['avatar'] ?? '',
      )).toList();
      updateFriends(newFriends);
    } catch (e) {
      debugPrint('Refresh friends failed: $e');
    }
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
