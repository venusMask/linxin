import 'dart:async';

class EventBus {
  static EventBus? _instance;
  static EventBus get instance => _instance ??= EventBus._();

  EventBus._();

  final _streamController = StreamController<Event>.broadcast();

  Stream<T> on<T extends Event>() {
    return _streamController.stream.where((event) => event is T).cast<T>();
  }

  void emit(Event event) {
    _streamController.add(event);
  }

  void dispose() {
    _streamController.close();
  }
}

abstract class Event {
  final DateTime timestamp = DateTime.now();
}

class MessageSentEvent extends Event {
  final String conversationId;
  final String messageId;
  final String content;
  final String senderId;

  MessageSentEvent({
    required this.conversationId,
    required this.messageId,
    required this.content,
    required this.senderId,
  });
}

class MessageReceivedEvent extends Event {
  final String conversationId;
  final String messageId;
  final String content;
  final String senderId;
  final DateTime createdAt;

  MessageReceivedEvent({
    required this.conversationId,
    required this.messageId,
    required this.content,
    required this.senderId,
    required this.createdAt,
  });
}

class FriendAppliedEvent extends Event {
  final String targetUserId;
  final String remark;

  FriendAppliedEvent({
    required this.targetUserId,
    required this.remark,
  });
}

class GroupCreatedEvent extends Event {
  final String groupId;
  final String groupName;
  final List<String> memberIds;

  GroupCreatedEvent({
    required this.groupId,
    required this.groupName,
    required this.memberIds,
  });
}

class ConversationUpdatedEvent extends Event {
  final String conversationId;
  final String? lastMessage;
  final DateTime? lastMessageTime;

  ConversationUpdatedEvent({
    required this.conversationId,
    this.lastMessage,
    this.lastMessageTime,
  });
}