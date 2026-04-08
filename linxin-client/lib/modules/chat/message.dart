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
  final int? sequenceId;
  final String? senderType; // Agent 名称，如 OpenClaw
  final bool isAi;          // 是否由 AI 发送

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
    this.sequenceId,
    this.senderType,
    this.isAi = false,
  });

  bool get isGroupMessage => conversationType == 1 || groupId != null;
  bool get isFromAgent => isAi;

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
    int? sequenceId,
    String? senderType,
    bool? isAi,
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
      sequenceId: sequenceId ?? this.sequenceId,
      senderType: senderType ?? this.senderType,
      isAi: isAi ?? this.isAi,
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
      'sequenceId': sequenceId,
      'senderType': senderType,
      'isAi': isAi,
    };
  }

  factory Message.fromJson(Map<String, dynamic> json) {
    return Message(
      id: json['id']?.toString() ?? '',
      conversationId: json['conversationId']?.toString() ?? '',
      senderId: json['senderId']?.toString() ?? '',
      senderNickname: json['senderNickname'] as String?,
      senderAvatar: json['senderAvatar'] as String?,
      content: json['content'] as String? ?? '',
      messageType: json['messageType'] as int? ?? 1,
      status: json['status'] as int? ?? 1,
      createdAt: json['sendTime'] != null
          ? DateTime.parse(json['sendTime'] as String)
          : json['createdAt'] != null
              ? DateTime.parse(json['createdAt'] as String)
              : DateTime.now(),
      isRead: json['isRead'] as bool? ?? false,
      isMe: json['isMe'] as bool? ?? false,
      time: json['time'] != null ? DateTime.parse(json['time'] as String) : null,
      groupId: json['groupId']?.toString(),
      conversationType: json['conversationType'] as int? ?? 0,
      sequenceId: json['sequenceId'] is String 
          ? int.tryParse(json['sequenceId'] as String) 
          : (json['sequenceId'] as int?),
      senderType: json['senderType'] as String?,
      isAi: json['isAi'] as bool? ?? false,
    );
  }
}
