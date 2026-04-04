import 'package:flutter_test/flutter_test.dart';
import 'package:lin_xin/models/message.dart';

void main() {
  group('Message Model Tests', () {
    test('should parse standard USER message correctly', () {
      final json = {
        'id': '101',
        'conversationId': 'conv_1',
        'senderId': 'user_1',
        'content': 'hello',
        'isAi': false,
        'createdAt': '2024-04-04T12:00:00Z',
      };

      final message = Message.fromJson(json);

      expect(message.id, '101');
      expect(message.content, 'hello');
      expect(message.isAi, false);
      expect(message.isFromAgent, false);
    });

    test('should parse AGENT message correctly', () {
      final json = {
        'id': '102',
        'conversationId': 'conv_1',
        'senderId': 'user_1',
        'content': 'Agent message',
        'isAi': true,
        'senderType': 'OpenClaw',
        'sequenceId': 5005,
        'createdAt': '2024-04-04T12:00:00Z',
      };

      final message = Message.fromJson(json);

      expect(message.isAi, true);
      expect(message.isFromAgent, true);
      expect(message.senderType, 'OpenClaw');
      expect(message.sequenceId, 5005);
    });

    test('copyWith should create new instance with updated fields', () {
      final message = Message(
        id: '1',
        conversationId: 'c1',
        senderId: 's1',
        content: 'old',
        createdAt: DateTime.now(),
      );

      final updated = message.copyWith(content: 'new', isAi: true);

      expect(updated.content, 'new');
      expect(updated.isAi, true);
      expect(updated.id, message.id);
    });
  });
}
