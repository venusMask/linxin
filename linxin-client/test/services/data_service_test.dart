import 'package:flutter_test/flutter_test.dart';
import 'package:lin_xin/services/data_service.dart';
import 'package:lin_xin/models/friend.dart';
import 'package:lin_xin/services/http_service.dart';
import 'package:lin_xin/services/auth_service.dart';
import 'package:lin_xin/models/user.dart';
import 'package:dio/dio.dart';

class ManualFakeHttpService implements HttpService {
  Map<String, dynamic>? getResponse;
  Map<String, dynamic>? postResponse;
  
  @override
  Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) async {
    return Response(
      data: getResponse,
      requestOptions: RequestOptions(path: path, queryParameters: queryParameters),
    );
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    return Response(
      data: postResponse,
      requestOptions: RequestOptions(path: path, data: data),
    );
  }

  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
  @override void setToken(String token) {}
  @override void clearToken() {}
  @override Future<List> getReceivedApplies() async => [];
}

class ManualFakeAuthService implements AuthService {
  @override
  User? get currentUser => User(id: 1, username: 'testuser', nickname: 'Test');

  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
  @override bool get isLoggedIn => true;
}

void main() {
  group('DataService State Tests', () {
    late DataService dataService;
    late ManualFakeHttpService fakeHttpService;
    late ManualFakeAuthService fakeAuthService;

    setUp(() {
      fakeHttpService = ManualFakeHttpService();
      fakeAuthService = ManualFakeAuthService();
      HttpService.setMock(fakeHttpService);
      AuthService.setMock(fakeAuthService);
      
      dataService = DataService();
      dataService.initMockData(); 
    });

    test('should add messages to chats correctly', () {
      final friend = Friend(id: '101', name: 'Test', avatar: '', userType: 0);
      final chat = dataService.createChat(friend);
      
      expect(dataService.chats.length, 1);
      
      dataService.addMessage(chat.id, 'hello', DateTime.now());
      expect(dataService.chats[0].lastMessageContent, 'hello');
    });

    test('should update unread count', () {
      final friend = Friend(id: '102', name: 'Test2', avatar: '');
      final chat = dataService.createChat(friend);
      
      dataService.markAsRead(chat.id);
      expect(dataService.chats[0].unreadCount, 0);
    });

    test('should get AI assistant correctly', () {
      final aiFriend = Friend(id: '12345', name: 'AI', avatar: '', userType: 1);
      dataService.updateFriends([aiFriend]);
      
      final assistant = dataService.getAiAssistant();
      expect(assistant?.id, '12345');
      expect(assistant?.userType, 1);
    });

    test('refreshFriends should update friend list', () async {
      fakeHttpService.postResponse = {
        'records': [
          {'friendId': '201', 'friendNickname': 'Friend 1', 'avatar': ''},
          {'friendId': '202', 'friendNickname': 'Friend 2', 'avatar': ''},
        ]
      };
      
      await dataService.refreshFriends();
      
      expect(dataService.friends.length, 2);
      expect(dataService.friends[0].name, 'Friend 1');
    });
  });
}
