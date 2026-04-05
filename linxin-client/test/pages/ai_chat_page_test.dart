import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dio/dio.dart';
import 'package:lin_xin/modules/ai/pages/ai_chat_page.dart';
import 'package:lin_xin/core/service/http_service.dart';
import 'package:lin_xin/modules/auth/auth_service.dart';
import 'package:lin_xin/core/state/data_service.dart';
import 'package:lin_xin/modules/chat/message_local_service.dart';
import 'package:lin_xin/modules/auth/user.dart';
import 'package:lin_xin/modules/contact/friend.dart';
import 'package:lin_xin/modules/chat/message.dart';

class ManualFakeHttpService implements HttpService {
  Map<String, dynamic>? getResponse;
  Map<String, dynamic>? postResponse;
  Object? getError;
  List<Map<String, dynamic>> postCalls = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) async {
    if (getError != null) throw getError!;
    return Response(
      data: getResponse,
      requestOptions: RequestOptions(path: path, queryParameters: queryParameters),
    );
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postCalls.add({'path': path, 'data': data});
    return Response(
      data: postResponse,
      requestOptions: RequestOptions(path: path, data: data),
    );
  }

  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
  
  @override void setToken(String token) {}
  @override void clearToken() {}
  @override Future<Response> delete(String path) async => Response(requestOptions: RequestOptions(path: path));
  @override Future<Response> put(String path, {data}) async => Response(requestOptions: RequestOptions(path: path));
  @override Future requestRaw(String path, {data, int? connectTimeout, int? receiveTimeout}) async => {};
  @override Future<List> searchUsers(String keyword) async => [];
  @override Future<void> applyAddFriend(toUserId, {String? remark}) async {}
  @override Future<List> getReceivedApplies() async => [];
  @override Future<void> handleFriendApply(applyId, int status) async {}
  @override Future<Map<String, dynamic>> sendMessage({required String receiverId, required int messageType, required String content, String? extra}) async => {};
  @override Future<Map<String, dynamic>> sendGroupMessage({required String groupId, required int messageType, required String content, String? extra}) async => {};
  @override Future<List> getConversationList({int pageNum = 1, int pageSize = 20}) async => [];
  @override Future<Map<String, dynamic>> getOrCreateConversation(String peerId) async => {};
  @override Future<List> getMessageList(String conversationId, {int pageNum = 1, int pageSize = 20}) async => [];
  @override Future<void> markMessagesAsRead(String conversationId) async {}
}

class ManualFakeAuthService implements AuthService {
  @override
  User? get currentUser => User(id: 1, username: 'testuser', nickname: 'Test');

  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
  @override bool get isLoggedIn => true;
  @override bool get isInitialized => true;
  @override Future<User?> getCurrentUser() async => currentUser;
  @override void addListener(VoidCallback listener) {}
  @override void removeListener(VoidCallback listener) {}
  @override void dispose() {}
  @override bool get hasListeners => false;
  @override void notifyListeners() {}
}

class ManualFakeMessageLocalService implements MessageLocalService {
  @override
  Future<List<Message>> getMessages(String conversationId, {int limit = 50, int offset = 0, String? currentUserId}) async => [];
  
  @override
  Future<void> saveMessage(Message message) async {}

  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
  @override Future<Message?> getMessageById(String id) async => null;
  @override Future<void> deleteMessage(String id) async {}
  @override Future<void> markAsRead(String conversationId) async {}
  @override Future<int> getUnreadCount(String conversationId) async => 0;
  @override Future<void> clearConversationMessages(String conversationId) async {}
}

void main() {
  late ManualFakeHttpService fakeHttpService;
  late ManualFakeAuthService fakeAuthService;
  late ManualFakeMessageLocalService fakeMessageLocalService;

  setUp(() {
    fakeHttpService = ManualFakeHttpService();
    fakeAuthService = ManualFakeAuthService();
    fakeMessageLocalService = ManualFakeMessageLocalService();
    
    HttpService.setMock(fakeHttpService);
    AuthService.setMock(fakeAuthService);
    
    DataService().initMockData();
  });

  testWidgets('AIChatPage should initialize and send message correctly', (WidgetTester tester) async {
    final aiFriend = Friend(id: '12345', name: 'AI助手', userType: 1, avatar: '');
    DataService().updateFriends([aiFriend]);

    fakeHttpService.getResponse = {
      'id': 'chat_ai_123',
      'friend': {'id': '12345', 'nickname': 'AI助手', 'userType': 1},
      'lastMessage': 'Hello',
      'lastTime': '2026-04-05T10:00:00',
      'unreadCount': 0
    };

    fakeHttpService.postResponse = {
      'id': 'msg_server_1',
      'conversationId': 'chat_ai_123',
      'senderId': '1',
      'content': 'Hello AI',
      'messageType': 1,
      'status': 1,
      'createdAt': '2026-04-05T10:05:00',
    };

    await tester.pumpWidget(MaterialApp(
      home: AIChatPage(messageLocalService: fakeMessageLocalService),
    ));
    
    await tester.pumpAndSettle();

    expect(find.text('AI 助手'), findsAtLeastNWidgets(1));
    expect(find.byType(TextField), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'Hello AI');
    await tester.tap(find.byIcon(Icons.send_rounded));
    
    await tester.pump();
    expect(find.text('AI 助手正在思考'), findsOneWidget);
    
    await tester.pump(const Duration(milliseconds: 500));
    
    expect(find.text('Hello AI'), findsOneWidget);
    expect(fakeHttpService.postCalls.any((call) => call['data']['content'] == 'Hello AI'), true);
  });

  testWidgets('AIChatPage should show error snackbar when initialization fails', (WidgetTester tester) async {
    fakeHttpService.getError = DioException(
      requestOptions: RequestOptions(path: '/chat/conversations/ai'),
      error: 'User not found',
      type: DioExceptionType.badResponse,
      response: Response(
        requestOptions: RequestOptions(path: '/chat/conversations/ai'),
        statusCode: 500,
        data: {'message': '用户不存在'}
      )
    );

    await tester.pumpWidget(MaterialApp(
      home: AIChatPage(messageLocalService: fakeMessageLocalService),
    ));
    await tester.pump(); 
    await tester.pump(const Duration(milliseconds: 500)); 

    expect(find.byType(SnackBar), findsOneWidget);
    expect(find.textContaining('初始化 AI 会话失败'), findsOneWidget);
  });
}
