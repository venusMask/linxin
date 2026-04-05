import 'package:lin_xin/modules/contact/friend_service.dart';
import 'package:lin_xin/core/service/tool_executor.dart';

class AddFriendExecutor extends ToolExecutor {
  @override
  String get toolName => 'addFriend';

  @override
  Future<ToolExecuteResult> execute(Map<String, dynamic> params) async {
    final userId = params['userId'] as String?;
    final remark = params['remark'] as String? ?? '你好，我想加你为好友';

    if (userId == null || userId.isEmpty) {
      return ToolExecuteResult(success: false, message: '缺少用户ID');
    }

    try {
      await FriendService.instance.applyFriend(userId, remark: remark);
      return ToolExecuteResult(
        success: true,
        message: '好友申请已发送',
      );
    } catch (e) {
      return ToolExecuteResult(success: false, message: '发送失败: $e');
    }
  }
}