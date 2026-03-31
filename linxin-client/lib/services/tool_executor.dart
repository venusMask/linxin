abstract class ToolExecutor {
  String get toolName;

  Future<ToolExecuteResult> execute(Map<String, dynamic> params);
}

class ToolExecuteResult {
  final bool success;
  final String message;
  final dynamic data;

  ToolExecuteResult({
    required this.success,
    required this.message,
    this.data,
  });
}