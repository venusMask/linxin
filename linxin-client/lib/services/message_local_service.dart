import 'package:sqflite/sqflite.dart';
import '../models/message.dart';
import 'db_service.dart';

class MessageLocalService {
  final DatabaseService _dbService;

  MessageLocalService(this._dbService);

  Future<void> saveMessage(Message message) async {
    await _dbService.insert('messages', _messageToDb(message));
  }

  Future<void> saveMessages(List<Message> messages) async {
    if (messages.isEmpty) return;

    final db = await _dbService.database;
    await db.transaction((txn) async {
      final batch = txn.batch();
      for (final message in messages) {
        batch.insert(
          'messages',
          _messageToDb(message),
          conflictAlgorithm: ConflictAlgorithm.replace,
        );
      }
      await batch.commit(noResult: true);
    });
  }

  Map<String, dynamic> _messageToDb(Message message) {
    return {
      'id': message.id,
      'conversation_id': message.conversationId,
      'sender_id': message.senderId,
      'content': message.content,
      'message_type': message.messageType,
      'status': message.status,
      'created_at': message.createdAt.toIso8601String(),
      'is_read': message.isRead ? 1 : 0,
    };
  }

  Future<List<Message>> getMessages(
    String conversationId, {
    int limit = 50,
    int offset = 0,
    String? currentUserId,
  }) async {
    final results = await _dbService.query(
      'messages',
      where: 'conversation_id = ? AND deleted_at IS NULL',
      whereArgs: [conversationId],
      orderBy: 'created_at DESC',
      limit: limit,
      offset: offset,
    );

    return results.map((json) => _messageFromDb(json, currentUserId: currentUserId)).toList();
  }

  Future<Message?> getMessageById(String id) async {
    final results = await _dbService.query(
      'messages',
      where: 'id = ?',
      whereArgs: [id],
    );

    if (results.isEmpty) return null;
    return _messageFromDb(results.first);
  }

  Future<void> markAsRead(String conversationId) async {
    final db = await _dbService.database;
    await db.update(
      'messages',
      {'is_read': 1},
      where: 'conversation_id = ? AND is_read = 0',
      whereArgs: [conversationId],
    );
  }

  Future<void> updateMessageStatus(String id, int status) async {
    await _dbService.update(
      'messages',
      {'status': status},
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<List<Message>> searchMessages(String keyword) async {
    final results = await _dbService.rawQuery(
      'SELECT * FROM messages WHERE content LIKE ? AND deleted_at IS NULL ORDER BY created_at DESC LIMIT 100',
      ['%$keyword%'],
    );

    return results.map((json) => _messageFromDb(json)).toList();
  }

  Future<void> deleteMessage(String id) async {
    await _dbService.update(
      'messages',
      {'deleted_at': DateTime.now().toIso8601String()},
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Message _messageFromDb(Map<String, dynamic> json, {String? currentUserId}) {
    final senderId = json['sender_id'] as String;
    return Message(
      id: json['id'] as String,
      conversationId: json['conversation_id'] as String,
      senderId: senderId,
      content: json['content'] as String,
      messageType: json['message_type'] as int,
      status: json['status'] as int,
      createdAt: DateTime.parse(json['created_at'] as String),
      isRead: json['is_read'] == 1,
      isMe: currentUserId != null && senderId == currentUserId,
    );
  }
}