class User {
  final dynamic id;
  final String username;
  final String? password;
  final String? nickname;
  final String? avatar;
  final String? email;
  final String? signature;
  final int? gender;
  final int? userType; // 用户类型: 0-普通用户, 1-系统AI
  final DateTime? createTime;

  User({
    required this.id,
    required this.username,
    this.password,
    this.nickname,
    this.avatar,
    this.email,
    this.signature,
    this.gender,
    this.userType = 0,
    this.createTime,
  });

  User copyWith({
    dynamic id,
    String? username,
    String? password,
    String? nickname,
    String? avatar,
    String? email,
    String? signature,
    int? gender,
    int? userType,
    DateTime? createTime,
  }) {
    return User(
      id: id ?? this.id,
      username: username ?? this.username,
      password: password ?? this.password,
      nickname: nickname ?? this.nickname,
      avatar: avatar ?? this.avatar,
      email: email ?? this.email,
      signature: signature ?? this.signature,
      gender: gender ?? this.gender,
      userType: userType ?? this.userType,
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
      'email': email,
      'signature': signature,
      'gender': gender,
      'userType': userType,
      'create_time': createTime?.toIso8601String(),
    };
  }

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'],
      username: json['username'] as String,
      password: json['password'] as String?,
      nickname: json['nickname'] as String?,
      avatar: json['avatar'] as String?,
      email: json['email'] as String?,
      signature: json['signature'] as String?,
      gender: json['gender'] as int?,
      userType: json['userType'] as int? ?? 0,
      createTime: json['createTime'] != null
          ? DateTime.parse(json['createTime'] as String)
          : null,
    );
  }
}
