class Friend {
  final String id;
  final String name;
  final String avatar;

  Friend({
    required this.id,
    required this.name,
    required this.avatar,
  });

  Friend copyWith({
    String? id,
    String? name,
    String? avatar,
  }) {
    return Friend(
      id: id ?? this.id,
      name: name ?? this.name,
      avatar: avatar ?? this.avatar,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'avatar': avatar,
    };
  }

  factory Friend.fromJson(Map<String, dynamic> json) {
    return Friend(
      id: json['id'] as String,
      name: json['name'] as String,
      avatar: json['avatar'] as String,
    );
  }
}
