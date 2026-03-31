import '../models/chat.dart';
import '../models/friend.dart';
import '../models/message.dart';

class DataService {
  static final DataService _instance = DataService._internal();
  factory DataService() => _instance;
  DataService._internal();

  final List<Friend> _friends = [];
  final List<Chat> _chats = [];

  List<Friend> get friends => List.unmodifiable(_friends);
  List<Chat> get chats => List.unmodifiable(_chats);

  void initMockData() {
    _friends.clear();
    _chats.clear();
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
    return newChat;
  }

  void addMessage(String chatId, Message message) {
    final chat = getChatById(chatId);
    if (chat != null) {
      final updatedMessages = List<Message>.from(chat.messages)..add(message);
      final index = _chats.indexWhere((c) => c.id == chatId);
      _chats[index] = chat.copyWith(
        messages: updatedMessages,
        lastTime: message.time,
      );
    }
  }

  void markAsRead(String chatId) {
    final index = _chats.indexWhere((chat) => chat.id == chatId);
    if (index != -1) {
      _chats[index] = _chats[index].copyWith(unreadCount: 0);
    }
  }

  void updateChatLastTime(String chatId, DateTime time) {
    final index = _chats.indexWhere((chat) => chat.id == chatId);
    if (index != -1) {
      _chats[index] = _chats[index].copyWith(lastTime: time);
    }
  }
}
