import 'package:lin_xin/modules/chat/message_service.dart';
import 'package:lin_xin/core/service/tool_executor.dart';

class SendMessageExecutor extends ToolExecutor {
  @override
  String get toolName => 'sendMessage';

  @override
  Future<ToolExecuteResult> execute(Map<String, dynamic> params) async {
    final receiverId = params['receiverId'] as String?;
    final content = params['content'] as String?;

    if (receiverId == null || receiverId.isEmpty) {
      return ToolExecuteResult(success: false, message: '缺少接收者ID');
    }

    if (content == null || content.isEmpty) {
      return ToolExecuteResult(success: false, message: '缺少消息内容');
    }

    try {
      final conversationId = await MessageService.instance.getOrCreateConversation(receiverId);
      if (conversationId == null) {
        return ToolExecuteResult(success: false, message: '获取会话失败');
      }

      final message = await MessageService.instance.sendMessage(
        conversationId: conversationId,
        receiverId: receiverId,
        content: content,
      );

      if (message != null) {
        return ToolExecuteResult(
          success: true,
          message: '消息已发送',
          data: message,
        );
      }

      return ToolExecuteResult(success: false, message: '发送消息失败');
    } catch (e) {
      return ToolExecuteResult(success: false, message: '发送失败: $e');
    }
  }
}