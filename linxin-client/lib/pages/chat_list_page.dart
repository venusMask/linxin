import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/chat.dart';
import '../services/data_service.dart';
import '../services/websocket_service.dart';
import '../widgets/avatar_widget.dart';
import 'ai_chat_page.dart';
import 'chat_detail_page.dart';
import 'create_group_page.dart';

class ChatListPage extends StatefulWidget {
  const ChatListPage({super.key});

  @override
  State<ChatListPage> createState() => _ChatListPageState();
}

class _ChatListPageState extends State<ChatListPage> {
  final WebSocketService _webSocketService = WebSocketService();
  StreamSubscription<dynamic>? _messageSubscription;
  StreamSubscription<dynamic>? _groupMessageSubscription;

  @override
  void initState() {
    super.initState();
    _initWebSocketListeners();
  }

  void _initWebSocketListeners() {
    final dataService = context.read<DataService>();
    _messageSubscription = _webSocketService.messageStream.listen((data) {
      _handleWebSocketMessage(data, dataService);
    });

    _groupMessageSubscription = _webSocketService.groupMessageStream.listen((data) {
      _handleGroupWebSocketMessage(data, dataService);
    });
  }

  @override
  void dispose() {
    _messageSubscription?.cancel();
    _groupMessageSubscription?.cancel();
    super.dispose();
  }

  String _formatTime(DateTime time) {
    final now = DateTime.now();
    final difference = now.difference(time);

    if (difference.inMinutes < 1) {
      return '刚刚';
    } else if (difference.inHours < 1) {
      return '${difference.inMinutes}分钟前';
    } else if (difference.inDays < 1) {
      return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
    } else if (difference.inDays < 2) {
      return '昨天';
    } else {
      return '${time.month}/${time.day}';
    }
  }

  void _handleWebSocketMessage(dynamic data, DataService dataService) {
    if (data['type'] == 'new_message') {
      _updateChatFromMessage(data['data'], dataService);
    } else if (data['type'] == 'read_status') {
      final messageData = data['data'];
      final conversationId = messageData['conversationId']?.toString();
      if (conversationId != null) {
        dataService.markAsRead(conversationId);
      }
    }
  }

  void _handleGroupWebSocketMessage(dynamic data, DataService dataService) {
    if (data['type'] == 'group_message') {
      _updateChatFromMessage(data['data'], dataService);
    }
  }

  void _updateChatFromMessage(dynamic messageData, DataService dataService) {
    if (!mounted) return;
    final conversationId = messageData['conversationId']?.toString();
    if (conversationId == null) return;
    
    final chat = dataService.getChatById(conversationId);
    if (chat == null) {
      return;
    }

    dataService.addMessage(
      conversationId, 
      messageData['content']?.toString() ?? '', 
      DateTime.now()
    );
  }

  void _createGroup() async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => const CreateGroupPage(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final chats = context.watch<DataService>().chats;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text(
          '信息',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
        leading: IconButton(
          icon: Icon(Icons.psychology_outlined, color: Colors.green[600]),
          onPressed: () {
            Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => const AIChatPage()),
            );
          },
        ),
        actions: [
          IconButton(
            icon: Icon(Icons.group_add, color: Colors.green[600]),
            onPressed: _createGroup,
          ),
        ],
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0.5,
        centerTitle: true,
      ),
      body: chats.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.chat_bubble_outline,
                    size: 64,
                    color: Colors.grey[400],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    '暂无会话',
                    style: TextStyle(
                      fontSize: 16,
                      color: Colors.grey[600],
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextButton.icon(
                    onPressed: _createGroup,
                    icon: const Icon(Icons.add),
                    label: const Text('创建群聊'),
                  ),
                ],
              ),
            )
          : ListView.builder(
              itemCount: chats.length,
              itemBuilder: (context, index) {
                final chat = chats[index];
                return _buildChatItem(chat, context);
              },
            ),
    );
  }

  Widget _buildChatItem(Chat chat, BuildContext context) {
    final isGroup = chat.type == ChatType.group;
    final avatarTag = isGroup ? 'group_avatar_${chat.id}' : 'avatar_${chat.friend?.id ?? chat.id}';
    final dataService = context.read<DataService>();

    return InkWell(
      onTap: () async {
        dataService.markAsRead(chat.id);

        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => ChatDetailPage(chat: chat),
          ),
        );
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        child: Row(
          children: [
            Hero(
              tag: avatarTag,
              child: AvatarWidget(
                imageUrl: chat.displayAvatar,
                name: chat.displayName,
                size: 52,
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          Text(
                            chat.displayName,
                            style: const TextStyle(
                              fontSize: 16.5,
                              fontWeight: FontWeight.w600,
                              color: Colors.black87,
                            ),
                          ),
                          if (isGroup) ...[
                            const SizedBox(width: 4),
                            Icon(
                              Icons.group,
                              size: 14,
                              color: Colors.grey[500],
                            ),
                          ],
                        ],
                      ),
                      Text(
                        _formatTime(chat.lastTime),
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.grey[500],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          chat.lastMessageContent,
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.grey[500],
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      if (chat.unreadCount > 0) ...[
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 6,
                            vertical: 2,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.redAccent,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          constraints: const BoxConstraints(
                            minWidth: 18,
                            minHeight: 18,
                          ),
                          child: Text(
                            chat.unreadCount > 99
                                ? '99+'
                                : chat.unreadCount.toString(),
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                            ),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
