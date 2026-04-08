import 'dart:async';
import 'package:lin_xin/modules/chat/message.dart';
import 'package:lin_xin/core/service/http_service.dart';
import 'package:lin_xin/core/service/db_service.dart';
import 'package:lin_xin/modules/chat/message_local_service.dart';
import 'package:lin_xin/core/service/event_bus.dart';
import 'package:lin_xin/core/state/data_service.dart';

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

    final int? sequenceId = int.tryParse(response['sequenceId']?.toString() ?? '');

    final message = Message(
      id: response['id']?.toString() ?? '',
      conversationId: conversationId,
      senderId: response['senderId']?.toString() ?? '',
      content: content,
      messageType: messageType,
      status: 1,
      createdAt: DateTime.now(),
      isRead: false,
      sequenceId: sequenceId,
    );

    await _localService.saveMessage(message);

    if (sequenceId != null) {
      DataService().updateLastSequenceId(sequenceId);
    }

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