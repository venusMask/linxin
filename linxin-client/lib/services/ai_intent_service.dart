import 'ai_service.dart';
import 'tool_executor.dart';
import 'executors/send_message_executor.dart';
import 'executors/add_friend_executor.dart';

class AIIntentService {
  static AIIntentService? _instance;
  static AIIntentService get instance => _instance ??= AIIntentService._();

  AIIntentService._() {
    _registerDefaultExecutors();
  }

  final Map<String, ToolExecutor> _executors = {};

  void registerExecutor(ToolExecutor executor) {
    _executors[executor.toolName] = executor;
  }

  void _registerDefaultExecutors() {
    registerExecutor(SendMessageExecutor());
    registerExecutor(AddFriendExecutor());
  }

  Future<ToolExecuteResult> executeToolCall(ToolCall toolCall) async {
    final executor = _executors[toolCall.toolName];
    if (executor == null) {
      return ToolExecuteResult(
        success: false,
        message: '未找到工具: ${toolCall.toolName}',
      );
    }

    try {
      return await executor.execute(toolCall.params);
    } catch (e) {
      return ToolExecuteResult(
        success: false,
        message: '执行失败: $e',
      );
    }
  }

  Future<ToolExecuteResult> executeToolCalls(List<ToolCall> toolCalls) async {
    StringBuffer messages = StringBuffer();
    bool hasError = false;

    for (final toolCall in toolCalls) {
      final result = await executeToolCall(toolCall);
      if (!result.success) hasError = true;
      if (messages.isNotEmpty) messages.write('\n');
      messages.write(result.message);
    }

    return ToolExecuteResult(
      success: !hasError,
      message: messages.toString(),
    );
  }

  bool hasExecutor(String toolName) {
    return _executors.containsKey(toolName);
  }

  List<String> get supportedTools => _executors.keys.toList();
}