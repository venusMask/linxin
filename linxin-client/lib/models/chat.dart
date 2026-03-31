import 'friend.dart';
import 'message.dart';
import 'group.dart';

enum ChatType { private, group }

class Chat {
  final String id;
  final Friend? friend;
  final Group? group;
  final List<Message> messages;
  final DateTime lastTime;
  final int unreadCount;
  final ChatType type;
  final String? lastMessage;
  final String? groupName;
  final String? groupAvatar;

  Chat({
    required this.id,
    this.friend,
    this.group,
    required this.messages,
    required this.lastTime,
    this.unreadCount = 0,
    this.type = ChatType.private,
    this.lastMessage,
    this.groupName,
    this.groupAvatar,
  });

  String get displayName {
    if (type == ChatType.group) {
      return groupName ?? group?.name ?? '群聊';
    }
    return friend?.name ?? '';
  }

  String get displayAvatar {
    if (type == ChatType.group) {
      return groupAvatar ?? group?.avatar ?? '';
    }
    return friend?.avatar ?? '';
  }

  String get lastMessageContent {
    if (lastMessage != null) return lastMessage!;
    if (messages.isEmpty) return '';
    return messages.last.content;
  }

  Chat copyWith({
    String? id,
    Friend? friend,
    Group? group,
    List<Message>? messages,
    DateTime? lastTime,
    int? unreadCount,
    ChatType? type,
    String? lastMessage,
    String? groupName,
    String? groupAvatar,
  }) {
    return Chat(
      id: id ?? this.id,
      friend: friend ?? this.friend,
      group: group ?? this.group,
      messages: messages ?? this.messages,
      lastTime: lastTime ?? this.lastTime,
      unreadCount: unreadCount ?? this.unreadCount,
      type: type ?? this.type,
      lastMessage: lastMessage ?? this.lastMessage,
      groupName: groupName ?? this.groupName,
      groupAvatar: groupAvatar ?? this.groupAvatar,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'friend': friend?.toJson(),
      'group': group?.toJson(),
      'messages': messages.map((m) => m.toJson()).toList(),
      'lastTime': lastTime.toIso8601String(),
      'unreadCount': unreadCount,
      'type': type.index,
      'lastMessage': lastMessage,
      'groupName': groupName,
      'groupAvatar': groupAvatar,
    };
  }

  factory Chat.fromJson(Map<String, dynamic> json) {
    return Chat(
      id: json['id'] as String,
      friend: json['friend'] != null
          ? Friend.fromJson(json['friend'] as Map<String, dynamic>)
          : null,
      group: json['group'] != null
          ? Group.fromJson(json['group'] as Map<String, dynamic>)
          : null,
      messages: (json['messages'] as List?)
              ?.map((m) => Message.fromJson(m as Map<String, dynamic>))
              .toList() ??
          [],
      lastTime: DateTime.parse(json['lastTime'] as String),
      unreadCount: json['unreadCount'] as int? ?? 0,
      type: json['type'] == 1 ? ChatType.group : ChatType.private,
      lastMessage: json['lastMessage'] as String?,
      groupName: json['groupName'] as String?,
      groupAvatar: json['groupAvatar'] as String?,
    );
  }

  factory Chat.fromConversationJson(Map<String, dynamic> json) {
    final type = json['type'] as int? ?? 0;
    final isGroup = type == 1;

    if (isGroup) {
      return Chat(
        id: json['id'].toString(),
        group: json['groupId'] != null
            ? Group(
                id: json['groupId'].toString(),
                name: json['peerNickname'] as String? ?? '群聊',
                avatar: json['peerAvatar'] as String? ?? '',
                ownerId: 0,
              )
            : null,
        messages: [],
        lastTime: json['lastMessageTime'] != null
            ? DateTime.parse(json['lastMessageTime'] as String)
            : DateTime.now(),
        unreadCount: json['unreadCount'] as int? ?? 0,
        type: ChatType.group,
        lastMessage: json['lastMessageContent'] as String?,
        groupName: json['peerNickname'] as String?,
        groupAvatar: json['peerAvatar'] as String?,
      );
    } else {
      return Chat(
        id: json['id'] as String,
        friend: Friend(
          id: json['peerId'].toString(),
          name: json['peerNickname'] as String? ?? '未知',
          avatar: json['peerAvatar'] as String? ?? '',
        ),
        messages: [],
        lastTime: json['lastMessageTime'] != null
            ? DateTime.parse(json['lastMessageTime'] as String)
            : DateTime.now(),
        unreadCount: json['unreadCount'] as int? ?? 0,
        type: ChatType.private,
        lastMessage: json['lastMessageContent'] as String?,
      );
    }
  }
}
