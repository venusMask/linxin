import 'dart:async';
import 'package:flutter/material.dart';
import '../models/chat.dart';
import '../models/message.dart';
import '../services/http_service.dart';
import '../services/auth_service.dart';
import '../services/event_bus.dart';
import '../services/db_service.dart';
import '../services/message_local_service.dart';
import '../widgets/message_bubble.dart';

class AIChatPage extends StatefulWidget {
  const AIChatPage({super.key});

  @override
  State<AIChatPage> createState() => _AIChatPageState();
}

class _AIChatPageState extends State<AIChatPage> {
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final HttpService _httpService = HttpService();
  late final MessageLocalService _messageLocalService;
  final EventBus _eventBus = EventBus.instance;

  List<Message> _messages = [];
  String? _currentUserId;
  Chat? _aiChat;
  bool _isInitialLoading = true;

  late StreamSubscription<MessageReceivedEvent> _messageReceivedSubscription;

  @override
  void initState() {
    super.initState();
    _currentUserId = AuthService().currentUser?.id.toString();
    _messageLocalService = MessageLocalService(DatabaseService());
    _initAIChat();
    _initEventBus();
  }

  Future<void> _initAIChat() async {
    // 999 是后端约定的 AI 助手 ID
    try {
      final response = await _httpService.get('/chat/conversations/999');
      final chat = Chat.fromJson(response.data);
      
      setState(() {
        _aiChat = chat;
      });

      final localMessages = await _messageLocalService.getMessages(
        chat.id,
        currentUserId: _currentUserId,
      );
      
      setState(() {
        _messages = localMessages.reversed.toList();
        _isInitialLoading = false;
      });
      _scrollToBottom();
    } catch (e) {
      debugPrint('初始化 AI 会话失败: $e');
      setState(() {
        _isInitialLoading = false;
      });
    }
  }

  void _initEventBus() {
    _messageReceivedSubscription = _eventBus.on<MessageReceivedEvent>().listen((event) {
      if (_aiChat != null && event.conversationId == _aiChat!.id) {
        _addNewMessage(Message(
          id: event.messageId,
          conversationId: event.conversationId,
          senderId: event.senderId,
          content: event.content,
          messageType: 1,
          status: 1,
          createdAt: event.createdAt,
          isMe: event.senderId == _currentUserId,
          isAi: event.senderId == '999',
        ));
      }
    });
  }

  void _addNewMessage(Message message) {
    if (!mounted) return;
    setState(() {
      final exists = _messages.any((m) => m.id == message.id);
      if (!exists) {
        _messages.add(message);
      }
    });
    _scrollToBottom();
  }

  Future<void> _sendMessage() async {
    final text = _messageController.text.trim();
    if (text.isEmpty || _aiChat == null) return;

    final tempId = 'msg_${DateTime.now().millisecondsSinceEpoch}';
    final now = DateTime.now();

    final pendingMessage = Message(
      id: tempId,
      conversationId: _aiChat!.id,
      senderId: _currentUserId ?? '',
      content: text,
      messageType: 1,
      status: 0,
      createdAt: now,
      isMe: true,
    );

    setState(() {
      _messages.add(pendingMessage);
      _messageController.clear();
    });
    _scrollToBottom();

    try {
      final response = await _httpService.post('/chat/messages', data: {
        'receiverId': 999,
        'content': text,
        'messageType': 1,
      });

      // 注意：发送给 AI 的消息，后端 handleAIChat 会立即返回用户消息的持久化结果
      // 随后 AI 的回复会通过 WebSocket 推送过来
      final serverMessage = Message.fromJson(response.data);
      await _messageLocalService.saveMessage(serverMessage);

      if (mounted) {
        setState(() {
          final index = _messages.indexWhere((m) => m.id == pendingMessage.id);
          if (index != -1) {
            _messages[index] = serverMessage;
          }
        });
      }
    } catch (e) {
      debugPrint('发送 AI 消息失败: $e');
      if (mounted) {
        setState(() {
          final index = _messages.indexWhere((m) => m.id == pendingMessage.id);
          if (index != -1) {
            _messages[index] = pendingMessage.copyWith(status: -1);
          }
        });
      }
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  void dispose() {
    _messageReceivedSubscription.cancel();
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        title: const Text('AI 助手', style: TextStyle(fontWeight: FontWeight.bold)),
        elevation: 0.5,
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        centerTitle: true,
      ),
      body: Column(
        children: [
          Expanded(
            child: _isInitialLoading
                ? const Center(child: CircularProgressIndicator())
                : ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.all(16),
                    itemCount: _messages.length,
                    itemBuilder: (context, index) {
                      final message = _messages[index];
                      return MessageBubble(
                        message: message,
                        showSenderInfo: false,
                      );
                    },
                  ),
          ),
          _buildInputBar(),
        ],
      ),
    );
  }

  Widget _buildInputBar() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(top: BorderSide(color: Color(0xFFEEEEEE))),
      ),
      child: SafeArea(
        child: Row(
          children: [
            Expanded(
              child: Container(
                decoration: BoxDecoration(
                  color: const Color(0xFFF3F3F3),
                  borderRadius: BorderRadius.circular(24),
                ),
                child: TextField(
                  controller: _messageController,
                  decoration: const InputDecoration(
                    hintText: '告诉我你想做什么...',
                    hintStyle: TextStyle(color: Color(0xFF9E9E9E), fontSize: 15),
                    border: InputBorder.none,
                    contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  ),
                  maxLines: 4,
                  minLines: 1,
                  textInputAction: TextInputAction.send,
                  onSubmitted: (_) => _sendMessage(),
                ),
              ),
            ),
            const SizedBox(width: 8),
            GestureDetector(
              onTap: _sendMessage,
              child: Container(
                width: 40,
                height: 40,
                decoration: const BoxDecoration(
                  color: Color(0xFF00BFA5),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.send_rounded, color: Colors.white, size: 20),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
