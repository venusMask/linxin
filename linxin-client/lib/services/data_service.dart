import 'package:flutter/foundation.dart';
import '../models/chat.dart';
import '../models/friend.dart';
import '../models/message.dart';

class DataService extends ChangeNotifier {
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
    notifyListeners();
  }

  void updateFriends(List<Friend> newFriends) {
    _friends.clear();
    _friends.addAll(newFriends);
    notifyListeners();
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
