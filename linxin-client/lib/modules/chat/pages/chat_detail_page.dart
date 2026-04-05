import 'dart:async';
import 'package:flutter/material.dart';
import 'package:lin_xin/modules/chat/chat.dart';
import 'package:lin_xin/modules/chat/message.dart';
import 'package:lin_xin/modules/auth/user.dart';
import 'package:lin_xin/modules/contact/group.dart';
import 'package:lin_xin/core/service/http_service.dart';
import 'package:lin_xin/core/service/websocket_service.dart';
import 'package:lin_xin/core/service/db_service.dart';
import 'package:lin_xin/modules/chat/message_local_service.dart';
import 'package:lin_xin/modules/auth/auth_service.dart';
import 'package:lin_xin/core/service/event_bus.dart';
import 'package:lin_xin/widgets/message_bubble.dart';
import 'package:lin_xin/modules/contact/pages/group_settings_page.dart';

class ChatDetailPage extends StatefulWidget {
  final Chat chat;
  final User? currentUser;
  final MessageLocalService? messageLocalService;

  const ChatDetailPage({
    super.key,
    required this.chat,
    this.currentUser,
    this.messageLocalService,
  });

  @override
  State<ChatDetailPage> createState() => _ChatDetailPageState();
}

class _ChatDetailPageState extends State<ChatDetailPage> {
  final HttpService _httpService = HttpService();
  late final MessageLocalService _messageLocalService;
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final WebSocketService _webSocketService = WebSocketService();
  final EventBus _eventBus = EventBus.instance;

  late final Future<List<Message>> _messagesFuture;
  List<Message> _messages = [];
  String? _currentUserId;
  bool get _isGroupChat => widget.chat.type == ChatType.group;
  Group? _group;

  late final StreamSubscription<MessageSentEvent> _messageSentSubscription;
  late final StreamSubscription<MessageReceivedEvent> _messageReceivedSubscription;

  @override
  void initState() {
    super.initState();
    _currentUserId = AuthService().currentUser?.id?.toString();
    _messageLocalService = widget.messageLocalService ?? MessageLocalService(DatabaseService());
    _messages = List.from(widget.chat.messages);
    _scrollToBottom();
    _initEventBus();
    _messagesFuture = _loadLocalMessages();
    if (_isGroupChat) {
      _group = widget.chat.group;
    }
  }

  Future<List<Message>> _loadLocalMessages() async {
    final localMessages = await _messageLocalService.getMessages(
      widget.chat.id,
      currentUserId: _currentUserId,
    );
    if (localMessages.isNotEmpty && mounted) {
      _messages = localMessages.reversed.toList();
      if (mounted) setState(() {});
    }
    return _messages;
  }

  @override
  void dispose() {
    _messageSentSubscription.cancel();
    _messageReceivedSubscription.cancel();
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _initEventBus() {
    _messageSentSubscription = _eventBus.on<MessageSentEvent>().listen((event) {
      if (!mounted) return;
      if (event.conversationId == widget.chat.id) {
        _addNewMessage(Message(
          id: event.messageId,
          conversationId: event.conversationId,
          senderId: event.senderId,
          content: event.content,
          messageType: 1,
          status: 1,
          createdAt: event.timestamp,
          isRead: false,
          isMe: true,
        ));
      }
    });

    _messageReceivedSubscription = _eventBus.on<MessageReceivedEvent>().listen((event) {
      if (!mounted) return;
      if (event.conversationId == widget.chat.id) {
        _addNewMessage(Message(
          id: event.messageId,
          conversationId: event.conversationId,
          senderId: event.senderId,
          content: event.content,
          messageType: 1,
          status: 1,
          createdAt: event.createdAt,
          isRead: true, // 已经在当前页面，设为已读
          isMe: false,
        ));
        _markAsRead(); // 同步到服务端
      }
    });
  }

  void _addNewMessage(Message message) {
    setState(() {
      final exists = _messages.any((m) => m.id == message.id);
      if (!exists) {
        _messages.add(message);
      }
    });
    _scrollToBottom();
  }

  // _initWebSocket removed to use EventBus instead

  Future<void> _markAsRead() async {
    try {
      await _httpService.markMessagesAsRead(widget.chat.id);
      await _messageLocalService.markAsRead(widget.chat.id);
    } catch (e) {
      debugPrint('标记已读失败: $e');
      if (mounted) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text('同步已读状态失败: $e')),
            );
          }
        });
      }
    }
  }

  Future<void> _sendMessage() async {
    final text = _messageController.text.trim();
    if (text.isEmpty) return;

    final tempId = 'msg_${DateTime.now().millisecondsSinceEpoch}';
    final now = DateTime.now();

    final pendingMessage = Message(
      id: tempId,
      conversationId: widget.chat.id,
      senderId: _currentUserId ?? '',
      content: text,
      messageType: 1,
      status: 0,
      createdAt: now,
      isRead: true,
      isMe: true,
    );

    setState(() {
      _messages.add(pendingMessage);
      _messageController.clear();
    });

    _scrollToBottom();

    try {
      Map<String, dynamic> response;
      if (_isGroupChat) {
        response = await _httpService.sendGroupMessage(
          groupId: widget.chat.group?.id ?? '',
          messageType: 1,
          content: text,
        );
      } else {
        response = await _httpService.sendMessage(
          receiverId: widget.chat.friend?.id ?? '',
          messageType: 1,
          content: text,
        );
      }

      final serverMessage = Message.fromJson({
        'id': response['id']?.toString() ?? tempId,
        'conversationId': widget.chat.id,
        'senderId': _currentUserId ?? '',
        'content': text,
        'messageType': 1,
        'status': 1,
        'createdAt': response['createdAt'] ?? now.toIso8601String(),
        'isRead': true,
        'isMe': true,
        'groupId': _isGroupChat ? widget.chat.group?.id : null,
        'conversationType': _isGroupChat ? 1 : 0,
      });

      await _messageLocalService.saveMessage(serverMessage);

      if (mounted) {
        setState(() {
          final index = _messages.indexWhere((m) => m.id == pendingMessage.id);
          if (index != -1) {
            _messages[index] = serverMessage;
          }
        });
      }

      if (_isGroupChat) {
        _webSocketService.sendGroupMessage(
          groupId: widget.chat.group?.id ?? '',
          content: text,
          messageType: 1,
        );
      } else {
        _webSocketService.sendMessage(
          content: text,
          conversationId: widget.chat.id,
          messageType: 1,
        );
      }

    } catch (e) {
      debugPrint('发送消息失败: $e');

      if (mounted) {
        setState(() {
          final index = _messages.indexWhere((m) => m.id == pendingMessage.id);
          if (index != -1) {
            _messages[index] = pendingMessage.copyWith(status: -1);
          }
        });
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text('发送失败: $e')),
            );
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

  void _openGroupSettings() {
    if (_isGroupChat && _group != null) {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => GroupSettingsPage(group: _group!),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: GestureDetector(
          onTap: _openGroupSettings,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (_isGroupChat) ...[
                Text(_group?.name ?? widget.chat.displayName),
                const SizedBox(width: 4),
                Text(
                  '(${widget.chat.group?.memberCount ?? 0})',
                  style: const TextStyle(fontSize: 14, color: Colors.grey),
                ),
              ] else ...[
                Text(widget.chat.friend?.name ?? ''),
              ],
            ],
          ),
        ),
        actions: [
          if (_isGroupChat)
            IconButton(
              icon: const Icon(Icons.group),
              onPressed: _openGroupSettings,
            ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: FutureBuilder<List<Message>>(
              future: _messagesFuture,
              builder: (context, snapshot) {
                return ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.all(16),
                  itemCount: _messages.length,
                  itemBuilder: (context, index) {
                    final message = _messages[index];
                    return MessageBubble(
                      message: message,
                      showSenderInfo: _isGroupChat && !message.isMe,
                    );
                  },
                );
              },
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            decoration: BoxDecoration(
              color: Colors.white,
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.04),
                  offset: const Offset(0, -2),
                  blurRadius: 10,
                ),
              ],
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
                        decoration: InputDecoration(
                          hintText: _isGroupChat ? '在群聊中说点什么...' : '输入消息...',
                          hintStyle: const TextStyle(color: Color(0xFF9E9E9E), fontSize: 15),
                          border: InputBorder.none,
                          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
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
          ),
        ],
      ),
    );
  }
}
