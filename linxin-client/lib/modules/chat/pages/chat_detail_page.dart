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
  late String _chatId;
  bool get _isGroupChat => widget.chat.type == ChatType.group;
  Group? _group;

  late final StreamSubscription<MessageSentEvent> _messageSentSubscription;
  late final StreamSubscription<MessageReceivedEvent> _messageReceivedSubscription;

  @override
    void initState() {
      super.initState();
      _chatId = widget.chat.id;
      _currentUserId = AuthService().currentUser?.id?.toString();
      _messageLocalService = widget.messageLocalService ?? MessageLocalService(DatabaseService());
      // 这里的 widget.chat.messages 假设是按时间从旧到新（如果有初值），我们需要翻转它以适应 reverse: true
      _messages = List.from(widget.chat.messages.reversed);
      _initEventBus();
      _messagesFuture = _loadLocalMessages();
      if (_isGroupChat) {
        _group = widget.chat.group;
      }
    }

    Future<List<Message>> _loadLocalMessages() async {
      final localMessages = await _messageLocalService.getMessages(
        _chatId,
        currentUserId: _currentUserId,
      );
      if (localMessages.isNotEmpty && mounted) {
        setState(() {
          // MessageLocalService.getMessages 返回的是最新的在前 (DESC)
          // 刚好适配 reverse: true 列表
          _messages = localMessages;
        });
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
        if (event.conversationId == _chatId) {
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
        // 允许通过真实的 conversationId 匹配，或者如果是临时会话且发件人是当前聊天的好友，也认为匹配
        bool isMatch = event.conversationId == _chatId;
        if (!isMatch && _chatId.startsWith('chat_') && widget.chat.friend != null) {
            if (event.senderId == widget.chat.friend!.friendId) {
                isMatch = true;
                // 更新 _chatId 为真实的会话 ID
                setState(() {
                    _chatId = event.conversationId;
                });
            }
        }

        if (isMatch) {
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
      if (!mounted) return;
      setState(() {
        final exists = _messages.any((m) => m.id == message.id);
        if (!exists) {
          // reverse: true 模式下，新消息插入到 index 0
          _messages.insert(0, message);
        }
      });
    }

    // _initWebSocket removed to use EventBus instead

    Future<void> _markAsRead() async {
      try {
        await _httpService.markMessagesAsRead(_chatId);
        await _messageLocalService.markAsRead(_chatId);
      } catch (e) {
        debugPrint('标记已读失败: $e');
      }
    }

    Future<void> _sendMessage() async {
      final text = _messageController.text.trim();
      if (text.isEmpty) return;

      final tempId = 'msg_${DateTime.now().millisecondsSinceEpoch}';
      final now = DateTime.now();

      final pendingMessage = Message(
        id: tempId,
        conversationId: _chatId,
        senderId: _currentUserId ?? '',
        content: text,
        messageType: 1,
        status: 0,
        createdAt: now,
        isRead: true,
        isMe: true,
      );

      _addNewMessage(pendingMessage);
      _messageController.clear();

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
            receiverId: widget.chat.friend?.friendId ?? '',
            messageType: 1,
            content: text,
          );
        }

        final realConversationId = response['conversationId']?.toString() ?? _chatId;
        if (_chatId.startsWith('chat_') && realConversationId != _chatId) {
            setState(() {
                _chatId = realConversationId;
            });
        }

        final serverMessage = Message.fromJson({
          'id': response['id']?.toString() ?? tempId,
          'conversationId': _chatId,
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
            conversationId: _chatId,
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
        }
      }
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
                    reverse: true, // 开启逆序列表
                    padding: const EdgeInsets.all(16),
                    itemCount: _messages.length,
                    itemBuilder: (context, index) {
                      final message = _messages[index];
                      // 使用 RepaintBoundary 隔离每个气泡的重绘，提升长列表滚动性能
                      return RepaintBoundary(
                        child: MessageBubble(
                          message: message,
                          showSenderInfo: _isGroupChat && !message.isMe,
                        ),
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
