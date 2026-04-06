import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'package:meta/meta.dart';
import 'package:lin_xin/core/service/log_service.dart';
import 'package:lin_xin/config/api_config.dart';

class WebSocketService {
  WebSocketService._();

  static WebSocketService instance = WebSocketService._();

  factory WebSocketService() => instance;

  @visibleForTesting
  static void setMock(WebSocketService mock) {
    instance = mock;
  }

  WebSocket? _socket;
  String? _token;

  final _messageController = StreamController<dynamic>.broadcast();
  final _groupMessageController = StreamController<dynamic>.broadcast();
  final _friendEventController = StreamController<dynamic>.broadcast();
  final _connectController = StreamController<void>.broadcast();
  final _disconnectController = StreamController<void>.broadcast();

  Stream<dynamic> get messageStream => _messageController.stream;
  Stream<dynamic> get groupMessageStream => _groupMessageController.stream;
  Stream<dynamic> get friendEventStream => _friendEventController.stream;
  Stream<void> get connectStream => _connectController.stream;
  Stream<void> get disconnectStream => _disconnectController.stream;

  int _reconnectAttempts = 0;
  Timer? _reconnectTimer;
  Timer? _heartbeatTimer;
  bool _shouldReconnect = true;
  static const int _maxReconnectDelay = 30;
  static const Duration _heartbeatInterval = Duration(seconds: 30);

  void setToken(String token) {
    _token = token;
  }

  Future<void> connect() async {
    _shouldReconnect = true;
    await _doConnect();
  }

  Future<void> _doConnect() async {
    _heartbeatTimer?.cancel();
    try {
      final wsUrl = '${ApiConfig.baseUrl.replaceFirst('http', 'ws')}/ws';
      _socket = await WebSocket.connect(
        wsUrl,
        headers: {
          'Authorization': 'Bearer $_token',
        },
      ).timeout(const Duration(seconds: 10));
      
      _socket?.listen(
        (data) {
          try {
            var jsonData = jsonDecode(data);
            final type = jsonData['type'] as String?;
            if (type == 'group_message') {
              _groupMessageController.add(jsonData);
            } else if (type == 'friend_apply' || type == 'friend_handle' || type == 'friend_delete') {
              _friendEventController.add(jsonData);
            } else if (type == 'pong') {
              // 心跳响应，暂时不处理，如有必要可记录延迟
            } else {
              _messageController.add(jsonData);
            }
          } catch (e) {
            LogService.warning('WebSocket message parsing error: $e');
          }
        },
        onError: (error) {
          LogService.error('WebSocket error: $error', error);
          _scheduleReconnect();
        },
        onDone: () {
          LogService.info('WebSocket connection closed');
          _disconnectController.add(null);
          _scheduleReconnect();
        },
      );
      _onConnectSuccess();
      _connectController.add(null);
      _startHeartbeat();
      LogService.info('WebSocket connected');
    } catch (e) {
      LogService.error('WebSocket connection error: $e', e);
      _scheduleReconnect();
    }
  }

  void _startHeartbeat() {
    _heartbeatTimer?.cancel();
    _heartbeatTimer = Timer.periodic(_heartbeatInterval, (_) {
      if (isConnected) {
        sendPing();
      }
    });
  }

  void _scheduleReconnect() {
    if (!_shouldReconnect) return;

    _heartbeatTimer?.cancel();
    _reconnectTimer?.cancel();
    final delay = Duration(seconds: min(_maxReconnectDelay, 1 << _reconnectAttempts));
    _reconnectAttempts++;

    LogService.info('WebSocket reconnecting in ${delay.inSeconds}s (attempt $_reconnectAttempts)');

    _reconnectTimer = Timer(delay, () async {
      await _doConnect();
    });
  }

  void _onConnectSuccess() {
    _reconnectAttempts = 0;
    _reconnectTimer?.cancel();
  }

  void disconnect() {
    _shouldReconnect = false;
    _reconnectTimer?.cancel();
    _heartbeatTimer?.cancel();
    _socket?.close();
    _socket = null;
  }

  void send(Map<String, dynamic> data) {
    if (_socket != null && _socket!.readyState == WebSocket.open) {
      _socket!.add(jsonEncode(data));
    }
  }

  void sendMessage({
    required String content,
    String? conversationId,
    int messageType = 1,
  }) {
    send({
      'type': 'message',
      'data': {
        'content': content,
        'conversationId': conversationId,
        'messageType': messageType,
      },
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    });
  }

  void sendGroupMessage({
    required String groupId,
    required String content,
    int messageType = 1,
  }) {
    send({
      'type': 'message',
      'data': {
        'content': content,
        'conversationType': 1,
        'groupId': groupId,
        'messageType': messageType,
      },
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    });
  }

  void sendPing() {
    send({
      'type': 'ping',
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    });
  }

  bool get isConnected => _socket != null && _socket!.readyState == WebSocket.open;

  void dispose() {
    disconnect();
    _messageController.close();
    _groupMessageController.close();
    _friendEventController.close();
    _connectController.close();
    _disconnectController.close();
  }
}
