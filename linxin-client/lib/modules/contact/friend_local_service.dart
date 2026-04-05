import 'package:lin_xin/modules/contact/friend.dart';
import 'package:lin_xin/core/service/db_service.dart';

class FriendLocalService {
  final DatabaseService _dbService;

  FriendLocalService(this._dbService);

  Future<void> saveFriend(Friend friend) async {
    await _dbService.insert('friends', {
      'id': friend.id,
      'user_id': friend.userId,
      'friend_id': friend.id, // 这里 id 通常就是 friendId
      'username': friend.username,
      'nickname': friend.name,
      'avatar': friend.avatar,
      'tags': friend.tags.join(','),
      'user_type': friend.userType,
      'sequence_id': friend.sequenceId,
      'status': 1,
      'created_at': DateTime.now().toIso8601String(),
    });
  }

  Future<void> deleteFriend(String friendId) async {
    await _dbService.delete('friends', where: 'id = ?', whereArgs: [friendId]);
  }

  Future<List<Friend>> getAllFriends() async {
    final List<Map<String, dynamic>> maps = await _dbService.query('friends', orderBy: 'sequence_id ASC');
    return maps.map((map) {
      return Friend(
        id: map['id'],
        userId: map['user_id'],
        username: map['username'],
        name: map['nickname'] ?? '未知',
        avatar: map['avatar'] ?? '',
        tags: (map['tags'] as String? ?? '').split(',').where((s) => s.isNotEmpty).toList(),
        userType: map['user_type'] ?? 0,
        sequenceId: map['sequence_id'] ?? 0,
      );
    }).toList();
  }

  Future<int> getMaxSequenceId() async {
    final List<Map<String, dynamic>> result = await _dbService.rawQuery(
      'SELECT MAX(sequence_id) as max_sid FROM friends'
    );
    if (result.isNotEmpty && result[0]['max_sid'] != null) {
      return result[0]['max_sid'] as int;
    }
    return 0;
  }
}
