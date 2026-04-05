class GroupMember {
  final String id;
  final String groupId;
  final String userId;
  final String nickname;
  final String avatar;
  final int role;
  final DateTime? joinTime;
  final int muteStatus;

  GroupMember({
    required this.id,
    required this.groupId,
    required this.userId,
    required this.nickname,
    this.avatar = '',
    this.role = 0,
    this.joinTime,
    this.muteStatus = 0,
  });

  bool get isOwner => role == 2;
  bool get isAdmin => role == 1;
  bool get isMuted => muteStatus == 1;

  GroupMember copyWith({
    String? id,
    String? groupId,
    String? userId,
    String? nickname,
    String? avatar,
    int? role,
    DateTime? joinTime,
    int? muteStatus,
  }) {
    return GroupMember(
      id: id ?? this.id,
      groupId: groupId ?? this.groupId,
      userId: userId ?? this.userId,
      nickname: nickname ?? this.nickname,
      avatar: avatar ?? this.avatar,
      role: role ?? this.role,
      joinTime: joinTime ?? this.joinTime,
      muteStatus: muteStatus ?? this.muteStatus,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'groupId': groupId,
      'userId': userId,
      'nickname': nickname,
      'avatar': avatar,
      'role': role,
      'joinTime': joinTime?.toIso8601String(),
      'muteStatus': muteStatus,
    };
  }

  factory GroupMember.fromJson(Map<String, dynamic> json) {
    return GroupMember(
      id: json['id'].toString(),
      groupId: json['groupId'].toString(),
      userId: json['userId'].toString(),
      nickname: json['nickname'] as String,
      avatar: json['avatar'] as String? ?? '',
      role: json['role'] as int? ?? 0,
      joinTime: json['joinTime'] != null
          ? DateTime.parse(json['joinTime'] as String)
          : null,
      muteStatus: json['muteStatus'] as int? ?? 0,
    );
  }
}
