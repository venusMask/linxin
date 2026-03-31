class Group {
  final String id;
  final String name;
  final String avatar;
  final int ownerId;
  final String? ownerNickname;
  final String? announcement;
  final int memberLimit;
  final int memberCount;
  final int status;
  final DateTime? createTime;

  Group({
    required this.id,
    required this.name,
    this.avatar = '',
    required this.ownerId,
    this.ownerNickname,
    this.announcement,
    this.memberLimit = 500,
    this.memberCount = 0,
    this.status = 0,
    this.createTime,
  });

  Group copyWith({
    String? id,
    String? name,
    String? avatar,
    int? ownerId,
    String? ownerNickname,
    String? announcement,
    int? memberLimit,
    int? memberCount,
    int? status,
    DateTime? createTime,
  }) {
    return Group(
      id: id ?? this.id,
      name: name ?? this.name,
      avatar: avatar ?? this.avatar,
      ownerId: ownerId ?? this.ownerId,
      ownerNickname: ownerNickname ?? this.ownerNickname,
      announcement: announcement ?? this.announcement,
      memberLimit: memberLimit ?? this.memberLimit,
      memberCount: memberCount ?? this.memberCount,
      status: status ?? this.status,
      createTime: createTime ?? this.createTime,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'avatar': avatar,
      'ownerId': ownerId,
      'ownerNickname': ownerNickname,
      'announcement': announcement,
      'memberLimit': memberLimit,
      'memberCount': memberCount,
      'status': status,
      'createTime': createTime?.toIso8601String(),
    };
  }

  factory Group.fromJson(Map<String, dynamic> json) {
    return Group(
      id: json['id'].toString(),
      name: json['name'] as String,
      avatar: json['avatar'] as String? ?? '',
      ownerId: json['ownerId'] as int,
      ownerNickname: json['ownerNickname'] as String?,
      announcement: json['announcement'] as String?,
      memberLimit: json['memberLimit'] as int? ?? 500,
      memberCount: json['memberCount'] as int? ?? 0,
      status: json['status'] as int? ?? 0,
      createTime: json['createTime'] != null
          ? DateTime.parse(json['createTime'] as String)
          : null,
    );
  }
}
