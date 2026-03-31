import 'dart:async';
import '../models/message.dart';
import 'http_service.dart';
import 'db_service.dart';
import 'message_local_service.dart';
import 'event_bus.dart';

class MessageService {
  MessageService._();

  static final MessageService instance = MessageService._();

  factory MessageService() => instance;

  final _eventBus = EventBus.instance;
  HttpService get _httpService => HttpService();
  final _localService = MessageLocalService(DatabaseService());

  Future<Message?> sendMessage({
    required String conversationId,
    required String receiverId,
    required String content,
    int messageType = 1,
    String? extra,
  }) async {
    final response = await _httpService.sendMessage(
      receiverId: receiverId,
      messageType: messageType,
      content: content,
      extra: extra,
    );

    final message = Message(
      id: response['id']?.toString() ?? '',
      conversationId: conversationId,
      senderId: response['senderId']?.toString() ?? '',
      content: content,
      messageType: messageType,
      status: 1,
      createdAt: DateTime.now(),
      isRead: false,
    );

    await _localService.saveMessage(message);

    _eventBus.emit(MessageSentEvent(
      conversationId: conversationId,
      messageId: message.id,
      content: content,
      senderId: message.senderId,
    ));

    _eventBus.emit(ConversationUpdatedEvent(
      conversationId: conversationId,
      lastMessage: content,
      lastMessageTime: message.createdAt,
    ));

    return message;
  }

  Future<String?> getOrCreateConversation(String peerId) async {
    final conversation = await _httpService.getOrCreateConversation(peerId);
    return conversation['id'] as String?;
  }

  Future<List<Message>> getMessages(String conversationId, {int page = 1, int pageSize = 50}) async {
    return await _localService.getMessages(conversationId, limit: pageSize, offset: (page - 1) * pageSize);
  }

  Future<void> markAsRead(String conversationId) async {
    await _httpService.markMessagesAsRead(conversationId);
    await _localService.markAsRead(conversationId);
  }

  Future<List<Message>> searchMessages(String keyword, {String? conversationId}) async {
    return await _localService.searchMessages(keyword);
  }
}