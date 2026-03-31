class User {
  final dynamic id;
  final String username;
  final String? password;
  final String? nickname;
  final String? avatar;
  final DateTime? createTime;

  User({
    required this.id,
    required this.username,
    this.password,
    this.nickname,
    this.avatar,
    this.createTime,
  });

  User copyWith({
    dynamic id,
    String? username,
    String? password,
    String? nickname,
    String? avatar,
    DateTime? createTime,
  }) {
    return User(
      id: id ?? this.id,
      username: username ?? this.username,
      password: password ?? this.password,
      nickname: nickname ?? this.nickname,
      avatar: avatar ?? this.avatar,
      createTime: createTime ?? this.createTime,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'username': username,
      'password': password,
      'nickname': nickname,
      'avatar': avatar,
      'createTime': createTime?.toIso8601String(),
    };
  }

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'],
      username: json['username'] as String,
      password: json['password'] as String?,
      nickname: json['nickname'] as String?,
      avatar: json['avatar'] as String?,
      createTime: json['createTime'] != null
          ? DateTime.parse(json['createTime'] as String)
          : null,
    );
  }
}
