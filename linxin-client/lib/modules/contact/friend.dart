class Friend {
  final String id; // 关系记录 ID
  final String friendId; // 好友用户 ID
  final String? userId; // 所属用户 ID
  final String? username; // 灵信号
  final String name;
  final String avatar;
  final List<String> tags; // 标签列表
  final int userType; // 用户类型: 0-普通用户, 1-系统AI
  final int sequenceId;

  Friend({
    required this.id,
    String? friendId,
    this.userId,
    this.username,
    required this.name,
    required this.avatar,
    this.tags = const [],
    this.userType = 0,
    this.sequenceId = 0,
  }) : friendId = friendId ?? id;

  Friend copyWith({
    String? id,
    String? friendId,
    String? userId,
    String? username,
    String? name,
    String? avatar,
    List<String>? tags,
    int? userType,
    int? sequenceId,
  }) {
    return Friend(
      id: id ?? this.id,
      friendId: friendId ?? this.friendId,
      userId: userId ?? this.userId,
      username: username ?? this.username,
      name: name ?? this.name,
      avatar: avatar ?? this.avatar,
      tags: tags ?? this.tags,
      userType: userType ?? this.userType,
      sequenceId: sequenceId ?? this.sequenceId,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'friendId': friendId,
      'userId': userId,
      'username': username,
      'name': name,
      'avatar': avatar,
      'tags': tags.join(','),
      'userType': userType,
      'sequenceId': sequenceId,
    };
  }

  factory Friend.fromJson(Map<String, dynamic> json) {
    // 处理后端传来的逗号分隔字符串标签
    final tagsRaw = json['tags'] as String?;
    final List<String> tagsList = (tagsRaw != null && tagsRaw.isNotEmpty) 
        ? tagsRaw.split(',').map((e) => e.trim()).toList()
        : [];

    return Friend(
      id: json['id']?.toString() ?? '',
      friendId: json['friendId']?.toString() ?? json['id']?.toString() ?? '',
      userId: json['userId']?.toString(),
      username: json['username'] as String? ?? json['friendUsername'] as String?,
      name: json['name'] as String? ?? json['nickname'] as String? ?? json['friendNickname'] as String? ?? '未知',
      avatar: json['avatar'] as String? ?? '',
      tags: tagsList,
      userType: json['userType'] as int? ?? 0,
      sequenceId: json['sequenceId'] is String 
          ? int.tryParse(json['sequenceId'] as String) ?? 0 
          : (json['sequenceId'] as int? ?? 0),
    );
  }
}
