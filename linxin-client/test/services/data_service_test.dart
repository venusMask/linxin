import 'package:flutter_test/flutter_test.dart';
import 'package:lin_xin/services/data_service.dart';
import 'package:lin_xin/models/friend.dart';

// 极简手动 Mock
class MockHttpService {
  Map<String, dynamic>? mockResponse;
  Future<dynamic> get(String path, {Map<String, dynamic>? queryParameters}) async {
    return mockResponse;
  }
}

void main() {
  group('DataService State Tests', () {
    late DataService dataService;

    setUp(() {
      dataService = DataService();
      dataService.initMockData(); // 清空状态
    });

    test('should add messages to chats correctly', () {
      final friend = Friend(id: '101', name: 'Test', avatar: '');
      dataService.createChat(friend);
      
      expect(dataService.chats.length, 1);
      
      dataService.addMessage('101', 'hello', DateTime.now());
      expect(dataService.chats[0].lastMessageContent, 'hello');
    });

    test('should update unread count', () {
      final friend = Friend(id: '102', name: 'Test2', avatar: '');
      final chat = dataService.createChat(friend);
      
      dataService.markAsRead(chat.id);
      expect(dataService.chats[0].unreadCount, 0);
    });
   group('Friend Management', () {
      test('should manage friend list', () {
        // 初始为空
        expect(dataService.friends.isEmpty, true);
      });
    });
  });
}
