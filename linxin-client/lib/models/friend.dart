class Friend {
  final String id;
  final String? username; // 灵信号
  final String name;
  final String avatar;
  final List<String> tags; // 标签列表
  final int userType; // 用户类型: 0-普通用户, 1-系统AI

  Friend({
    required this.id,
    this.username,
    required this.name,
    required this.avatar,
    this.tags = const [],
    this.userType = 0,
  });

  Friend copyWith({
    String? id,
    String? username,
    String? name,
    String? avatar,
    List<String>? tags,
    int? userType,
  }) {
    return Friend(
      id: id ?? this.id,
      username: username ?? this.username,
      name: name ?? this.name,
      avatar: avatar ?? this.avatar,
      tags: tags ?? this.tags,
      userType: userType ?? this.userType,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'username': username,
      'name': name,
      'avatar': avatar,
      'tags': tags.join(','),
      'userType': userType,
    };
  }

  factory Friend.fromJson(Map<String, dynamic> json) {
    // 处理后端传来的逗号分隔字符串标签
    final tagsRaw = json['tags'] as String?;
    final List<String> tagsList = (tagsRaw != null && tagsRaw.isNotEmpty) 
        ? tagsRaw.split(',').map((e) => e.trim()).toList()
        : [];

    return Friend(
      id: json['id']?.toString() ?? json['friendId']?.toString() ?? '',
      username: json['username'] as String? ?? json['friendUsername'] as String?,
      name: json['name'] as String? ?? json['friendNickname'] as String? ?? '未知',
      avatar: json['avatar'] as String? ?? '',
      tags: tagsList,
      userType: json['userType'] as int? ?? 0,
    );
  }
}
