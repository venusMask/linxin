import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dio/dio.dart';
import 'dart:async';
import 'package:lin_xin/pages/chat_detail_page.dart';
import 'package:lin_xin/services/http_service.dart';
import 'package:lin_xin/services/auth_service.dart';
import 'package:lin_xin/services/data_service.dart';
import 'package:lin_xin/services/message_local_service.dart';
import 'package:lin_xin/services/websocket_service.dart';
import 'package:lin_xin/services/event_bus.dart';
import 'package:lin_xin/models/user.dart';
import 'package:lin_xin/models/friend.dart';
import 'package:lin_xin/models/chat.dart';
import 'package:lin_xin/models/message.dart';
import 'package:lin_xin/models/group.dart';

class ManualFakeHttpService implements HttpService {
  Map<String, dynamic>? getResponse;
  Map<String, dynamic>? postResponse;
  List<Map<String, dynamic>> postCalls = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) async {
    return Response(data: getResponse, requestOptions: RequestOptions(path: path));
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postCalls.add({'path': path, 'data': data});
    return Response(data: postResponse, requestOptions: RequestOptions(path: path));
  }

  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
  @override void setToken(String token) {}
  @override void clearToken() {}
  @override Future<void> markMessagesAsRead(String conversationId) async {}
  
  @override 
  Future<Map<String, dynamic>> sendMessage({required String receiverId, required int messageType, required String content, String? extra}) async {
    postCalls.add({'path': '/chat/messages', 'data': {'receiverId': receiverId, 'content': content}});
    return postResponse ?? {'id': 'msg_123', 'createdAt': DateTime.now().toIso8601String()};
  }

  @override
  Future<Map<String, dynamic>> sendGroupMessage({required String groupId, required int messageType, required String content, String? extra}) async {
    postCalls.add({'path': '/chat/messages', 'data': {'groupId': groupId, 'content': content, 'conversationType': 1}});
    return postResponse ?? {'id': 'msg_group_123', 'createdAt': DateTime.now().toIso8601String()};
  }
}

class ManualFakeAuthService implements AuthService {
  @override
  User? get currentUser => User(id: 1, username: 'testuser', nickname: 'Test');
  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
  @override bool get isLoggedIn => true;
  @override bool get isInitialized => true;
}

class ManualFakeMessageLocalService implements MessageLocalService {
  List<Message> mockMessages = [];
  @override
  Future<List<Message>> getMessages(String conversationId, {int limit = 50, int offset = 0, String? currentUserId}) async => mockMessages;
  @override
  Future<void> saveMessage(Message message) async {}
  @override
  Future<void> markAsRead(String conversationId) async {}
  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class ManualFakeWebSocketService implements WebSocketService {
  List<Map<String, dynamic>> sentMessages = [];
  
  final _messageController = StreamController<dynamic>.broadcast();
  final _groupMessageController = StreamController<dynamic>.broadcast();
  final _friendEventController = StreamController<dynamic>.broadcast();
  final _connectController = StreamController<void>.broadcast();
  final _disconnectController = StreamController<void>.broadcast();

  @override Stream<dynamic> get messageStream => _messageController.stream;
  @override Stream<dynamic> get groupMessageStream => _groupMessageController.stream;
  @override Stream<dynamic> get friendEventStream => _friendEventController.stream;
  @override Stream<void> get connectStream => _connectController.stream;
  @override Stream<void> get disconnectStream => _disconnectController.stream;

  @override
  void sendMessage({required String content, String? conversationId, int messageType = 1}) {
    sentMessages.add({'content': content, 'conversationId': conversationId});
  }
  @override
  void sendGroupMessage({required String groupId, required String content, int messageType = 1}) {
    sentMessages.add({'content': content, 'groupId': groupId});
  }
  @override bool get isConnected => true;
  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  late ManualFakeHttpService fakeHttpService;
  late ManualFakeAuthService fakeAuthService;
  late ManualFakeMessageLocalService fakeMessageLocalService;
  late ManualFakeWebSocketService fakeWebSocketService;

  setUp(() {
    fakeHttpService = ManualFakeHttpService();
    fakeAuthService = ManualFakeAuthService();
    fakeMessageLocalService = ManualFakeMessageLocalService();
    fakeWebSocketService = ManualFakeWebSocketService();

    HttpService.setMock(fakeHttpService);
    AuthService.setMock(fakeAuthService);
    WebSocketService.setMock(fakeWebSocketService);
    
    DataService().initMockData();
  });

  testWidgets('ChatDetailPage displays messages and sends private message', (WidgetTester tester) async {
    final friend = Friend(id: 'friend_1', name: 'Friend Name', avatar: '');
    final chat = Chat(
      id: 'chat_1',
      friend: friend,
      messages: [],
      lastTime: DateTime.now(),
      unreadCount: 0,
    );

    fakeMessageLocalService.mockMessages = [
      Message(
        id: 'old_1',
        conversationId: 'chat_1',
        senderId: 'friend_1',
        content: 'Hello there',
        messageType: 1,
        status: 1,
        createdAt: DateTime.now().subtract(const Duration(minutes: 5)),
        isMe: false,
      ),
    ];

    await tester.pumpWidget(MaterialApp(
      home: ChatDetailPage(
        chat: chat,
        messageLocalService: fakeMessageLocalService,
      ),
    ));

    await tester.pumpAndSettle();

    // Verify initial message
    expect(find.text('Friend Name'), findsOneWidget);
    expect(find.text('Hello there'), findsOneWidget);

    // Send a message
    await tester.enterText(find.byType(TextField), 'Hi friend');
    await tester.tap(find.byIcon(Icons.send_rounded));
    await tester.pump();

    // Verify UI shows pending message
    expect(find.text('Hi friend'), findsOneWidget);
    
    await tester.pumpAndSettle();

    // Verify service calls
    expect(fakeHttpService.postCalls.any((call) => call['data']['content'] == 'Hi friend'), true);
    expect(fakeWebSocketService.sentMessages.any((msg) => msg['content'] == 'Hi friend'), true);
  });

  testWidgets('ChatDetailPage handles incoming messages via EventBus', (WidgetTester tester) async {
    final friend = Friend(id: 'friend_1', name: 'Friend Name', avatar: '');
    final chat = Chat(
      id: 'chat_1',
      friend: friend,
      messages: [],
      lastTime: DateTime.now(),
      unreadCount: 0,
    );

    await tester.pumpWidget(MaterialApp(
      home: ChatDetailPage(
        chat: chat,
        messageLocalService: fakeMessageLocalService,
      ),
    ));

    await tester.pumpAndSettle();

    // Simulate incoming message
    EventBus.instance.emit(MessageReceivedEvent(
      conversationId: 'chat_1',
      messageId: 'new_msg_99',
      content: 'Incoming news!',
      senderId: 'friend_1',
      createdAt: DateTime.now(),
    ));

    // EventBus delivery might be asynchronous, pump multiple frames or add slight delay
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pumpAndSettle();

    // Verify UI update
    expect(find.text('Incoming news!'), findsOneWidget);
  });

  testWidgets('ChatDetailPage displays group chat info', (WidgetTester tester) async {
    final group = Group(
      id: 'group_1',
      name: 'Test Group',
      avatar: '',
      ownerId: 1,
      memberCount: 5,
      announcement: '',
      createTime: DateTime.now(),
    );
    
    final chat = Chat(
      id: 'chat_group_1',
      type: ChatType.group,
      group: group,
      messages: [],
      lastTime: DateTime.now(),
      unreadCount: 0,
    );

    await tester.pumpWidget(MaterialApp(
      home: ChatDetailPage(
        chat: chat,
        messageLocalService: fakeMessageLocalService,
      ),
    ));

    await tester.pumpAndSettle();

    expect(find.text('Test Group'), findsOneWidget);
    expect(find.text('(5)'), findsOneWidget);
    
    // Test sending group message
    await tester.enterText(find.byType(TextField), 'Hello group');
    await tester.tap(find.byIcon(Icons.send_rounded));
    await tester.pumpAndSettle();

    expect(fakeHttpService.postCalls.any((call) => call['data']['groupId'] == 'group_1'), true);
    expect(fakeWebSocketService.sentMessages.any((msg) => msg['groupId'] == 'group_1'), true);
  });
}
