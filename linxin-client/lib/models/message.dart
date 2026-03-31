class Message {
  final String id;
  final String conversationId;
  final String senderId;
  final String? senderNickname;
  final String? senderAvatar;
  final String content;
  final int messageType;
  final int status;
  final DateTime createdAt;
  final bool isRead;
  final bool isMe;
  final DateTime? time;
  final String? groupId;
  final int conversationType;

  Message({
    required this.id,
    required this.conversationId,
    required this.senderId,
    this.senderNickname,
    this.senderAvatar,
    required this.content,
    this.messageType = 1,
    this.status = 1,
    required this.createdAt,
    this.isRead = false,
    this.isMe = false,
    this.time,
    this.groupId,
    this.conversationType = 0,
  });

  bool get isGroupMessage => conversationType == 1 || groupId != null;

  Message copyWith({
    String? id,
    String? conversationId,
    String? senderId,
    String? senderNickname,
    String? senderAvatar,
    String? content,
    int? messageType,
    int? status,
    DateTime? createdAt,
    bool? isRead,
    bool? isMe,
    DateTime? time,
    String? groupId,
    int? conversationType,
  }) {
    return Message(
      id: id ?? this.id,
      conversationId: conversationId ?? this.conversationId,
      senderId: senderId ?? this.senderId,
      senderNickname: senderNickname ?? this.senderNickname,
      senderAvatar: senderAvatar ?? this.senderAvatar,
      content: content ?? this.content,
      messageType: messageType ?? this.messageType,
      status: status ?? this.status,
      createdAt: createdAt ?? this.createdAt,
      isRead: isRead ?? this.isRead,
      isMe: isMe ?? this.isMe,
      time: time ?? this.time,
      groupId: groupId ?? this.groupId,
      conversationType: conversationType ?? this.conversationType,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'conversationId': conversationId,
      'senderId': senderId,
      'senderNickname': senderNickname,
      'senderAvatar': senderAvatar,
      'content': content,
      'messageType': messageType,
      'status': status,
      'createdAt': createdAt.toIso8601String(),
      'isRead': isRead,
      'isMe': isMe,
      'groupId': groupId,
      'conversationType': conversationType,
    };
  }

  factory Message.fromJson(Map<String, dynamic> json) {
    return Message(
      id: json['id'] as String,
      conversationId: json['conversationId'] as String? ?? json['conversation_id'] as String? ?? '',
      senderId: json['senderId'] as String? ?? json['sender_id'] as String? ?? '',
      senderNickname: json['senderNickname'] as String? ?? json['sender_nickname'] as String?,
      senderAvatar: json['senderAvatar'] as String? ?? json['sender_avatar'] as String?,
      content: json['content'] as String,
      messageType: json['messageType'] as int? ?? json['message_type'] as int? ?? 1,
      status: json['status'] as int? ?? 1,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : json['created_at'] != null
              ? DateTime.parse(json['created_at'] as String)
              : DateTime.now(),
      isRead: json['isRead'] as bool? ?? json['is_read'] == 1 ?? false,
      isMe: json['isMe'] as bool? ?? json['is_me'] as bool? ?? false,
      time: json['time'] != null ? DateTime.parse(json['time'] as String) : null,
      groupId: json['groupId']?.toString(),
      conversationType: json['conversationType'] as int? ?? 0,
    );
  }

  factory Message.fromServerJson(Map<String, dynamic> json) {
    return Message(
      id: json['id'] as String,
      conversationId: json['conversationId'] as String? ?? json['conversation_id'] as String? ?? '',
      senderId: json['senderId'] as String? ?? json['sender_id'] as String? ?? '',
      senderNickname: json['senderNickname'] as String? ?? json['sender_nickname'] as String?,
      senderAvatar: json['senderAvatar'] as String? ?? json['sender_avatar'] as String?,
      content: json['content'] as String,
      messageType: json['messageType'] as int? ?? json['message_type'] as int? ?? 1,
      status: json['status'] as int? ?? 1,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : json['created_at'] != null
              ? DateTime.parse(json['created_at'] as String)
              : DateTime.now(),
      isRead: json['isRead'] as bool? ?? json['is_read'] == 1 ?? false,
      isMe: json['isMe'] as bool? ?? json['is_me'] as bool? ?? false,
      groupId: json['groupId']?.toString(),
      conversationType: json['conversationType'] as int? ?? 0,
    );
  }
}
