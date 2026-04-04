import 'dart:async';
import 'http_service.dart';
import 'event_bus.dart';

class FriendService {
  static FriendService? _instance;
  static FriendService get instance => _instance ??= FriendService._();

  FriendService._();

  final _eventBus = EventBus.instance;
  HttpService get _httpService => HttpService();

  Future<bool> applyFriend(String userId, {String? remark}) async {
    try {
      await _httpService.applyAddFriend(userId, remark: remark ?? '你好，我想加你为好友');

      _eventBus.emit(FriendAppliedEvent(
        targetUserId: userId,
        remark: remark ?? '你好，我想加你为好友',
      ));

      return true;
    } catch (e) {
      rethrow;
    }
  }

  Future<List<dynamic>> getReceivedApplies() async {
    return await _httpService.getReceivedApplies();
  }

  Future<void> handleFriendApply(dynamic applyId, int status) async {
    await _httpService.handleFriendApply(applyId, status);
  }

  Future<void> deleteFriend(String friendId) async {
    await _httpService.delete('/friends/$friendId');
  }
}