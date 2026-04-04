import 'package:flutter_test/flutter_test.dart';
import 'package:lin_xin/models/user.dart';
import 'package:lin_xin/models/friend.dart';
import 'package:lin_xin/models/chat.dart';

void main() {
  group('Core Model Tests', () {
    test('User should parse from JSON', () {
      final json = {'id': 1, 'username': 'test', 'nickname': 'Nick'};
      final user = User.fromJson(json);
      expect(user.username, 'test');
      expect(user.id, 1);
    });

    test('Friend should parse from JSON with tags', () {
      final json = {'id': 2, 'friendNickname': 'Buddy', 'tags': '家人,重要'};
      final friend = Friend.fromJson(json);
      expect(friend.name, 'Buddy');
      expect(friend.tags, contains('家人'));
      expect(friend.tags.length, 2);
    });

    test('Chat should calculate displayName correctly', () {
      final friend = Friend(id: '2', name: 'Buddy', avatar: '');
      final chat = Chat(
        id: 'c1',
        type: ChatType.private,
        friend: friend,
        messages: [],
        lastMessage: 'hi',
        lastTime: DateTime.now(),
      );
      expect(chat.displayName, 'Buddy');
    });
  });
}
