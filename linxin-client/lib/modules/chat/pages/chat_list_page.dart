import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:lin_xin/modules/chat/chat.dart';
import 'package:lin_xin/core/state/data_service.dart';
import 'package:lin_xin/widgets/avatar_widget.dart';
import 'package:lin_xin/modules/ai/pages/ai_chat_page.dart';
import 'package:lin_xin/modules/chat/pages/chat_detail_page.dart';
import 'package:lin_xin/modules/contact/pages/create_group_page.dart';

class ChatListPage extends StatefulWidget {
  const ChatListPage({super.key});

  @override
  State<ChatListPage> createState() => _ChatListPageState();
}

class _ChatListPageState extends State<ChatListPage> {
  @override
  void dispose() {
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
    final avatarTag = isGroup ? 'chat_group_avatar_${chat.id}' : 'chat_avatar_${chat.friend?.id ?? chat.id}';
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
