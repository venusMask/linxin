import 'package:dio/dio.dart';
import 'package:lin_xin/core/service/http_service.dart';

class AIChatRequest {
  final String userInput;
  final String? conversationId;
  final Map<String, dynamic>? context;

  AIChatRequest({
    required this.userInput,
    this.conversationId,
    this.context,
  });

  Map<String, dynamic> toJson() => {
        'userInput': userInput,
        if (conversationId != null) 'conversationId': conversationId,
        if (context != null) 'context': context,
      };
}

class AIChatResponse {
  final String intent;
  final List<ToolCall> toolCalls;
  final String aiText;
  final bool needConfirm;
  final String? status;

  AIChatResponse({
    required this.intent,
    required this.toolCalls,
    required this.aiText,
    required this.needConfirm,
    this.status,
  });

  factory AIChatResponse.fromJson(Map<String, dynamic> json) {
    return AIChatResponse(
      intent: json['intent'] as String? ?? '',
      toolCalls: (json['toolCalls'] as List<dynamic>?)
              ?.map((tc) => ToolCall.fromJson(tc as Map<String, dynamic>))
              .toList() ??
          [],
      aiText: json['aiText'] as String? ?? '',
      needConfirm: json['needConfirm'] as bool? ?? false,
      status: json['status'] as String?,
    );
  }
}

class ToolCall {
  final String toolId;
  final String toolName;
  final Map<String, dynamic> params;
  final String description;

  ToolCall({
    required this.toolId,
    required this.toolName,
    required this.params,
    required this.description,
  });

  factory ToolCall.fromJson(Map<String, dynamic> json) {
    return ToolCall(
      toolId: json['toolId'] as String? ?? '',
      toolName: json['toolName'] as String? ?? '',
      params: json['params'] as Map<String, dynamic>? ?? {},
      description: json['description'] as String? ?? '',
    );
  }

  Map<String, dynamic> toJson() => {
        'toolId': toolId,
        'toolName': toolName,
        'params': params,
        'description': description,
      };
}

class ModifyParamsRequest {
  final String modification;
  final AIChatResponse originalResponse;

  ModifyParamsRequest({
    required this.modification,
    required this.originalResponse,
  });

  Map<String, dynamic> toJson() => {
        'modification': modification,
        'originalResponse': {
          'intent': originalResponse.intent,
          'toolCalls': originalResponse.toolCalls.map((tc) => tc.toJson()).toList(),
          'aiText': originalResponse.aiText,
          'needConfirm': originalResponse.needConfirm,
          'status': originalResponse.status,
        },
      };
}

class ExecuteRequest {
  final List<ToolCall> toolCalls;

  ExecuteRequest({required this.toolCalls});

  Map<String, dynamic> toJson() => {
        'toolCalls': toolCalls.map((tc) => tc.toJson()).toList(),
      };
}

class AIService {
  static AIService? _instance;
  static AIService get instance => _instance ??= AIService._();

  final HttpService _httpService = HttpService();

  AIService._();

  Future<AIChatResponse> chat(AIChatRequest request) async {
    try {
      final data = await _httpService.requestRaw(
        '/ai/chat',
        data: request.toJson(),
        connectTimeout: 30000,
        receiveTimeout: 60000,
      );
      if (data['code'] == 200) {
        return AIChatResponse.fromJson(data['data']);
      }
      throw AIServiceException(data['message'] ?? 'AI服务调用失败');
    } on DioException catch (e) {
      throw AIServiceException(_handleDioError(e));
    } catch (e) {
      if (e is AIServiceException) rethrow;
      throw AIServiceException('AI服务调用失败: $e');
    }
  }

  Future<AIChatResponse> modifyParams(ModifyParamsRequest request) async {
    try {
      final data = await _httpService.requestRaw(
        '/ai/modify',
        data: request.toJson(),
        connectTimeout: 30000,
        receiveTimeout: 60000,
      );
      if (data['code'] == 200) {
        return AIChatResponse.fromJson(data['data']);
      }
      throw AIServiceException(data['message'] ?? '修改参数失败');
    } on DioException catch (e) {
      throw AIServiceException(_handleDioError(e));
    } catch (e) {
      if (e is AIServiceException) rethrow;
      throw AIServiceException('修改参数失败: $e');
    }
  }

  Future<List<AIChatResponse>> execute(List<ToolCall> toolCalls) async {
    try {
      final request = ExecuteRequest(toolCalls: toolCalls);
      final data = await _httpService.requestRaw(
        '/ai/execute',
        data: request.toJson(),
        connectTimeout: 30000,
        receiveTimeout: 60000,
      );
      if (data['code'] == 200) {
        return (data['data'] as List<dynamic>)
            .map((item) => AIChatResponse.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      throw AIServiceException(data['message'] ?? '执行操作失败');
    } on DioException catch (e) {
      throw AIServiceException(_handleDioError(e));
    } catch (e) {
      if (e is AIServiceException) rethrow;
      throw AIServiceException('执行操作失败: $e');
    }
  }

  Future<List<ToolCall>> getTools() async {
    try {
      final data = await _httpService.requestRaw(
        '/ai/tools',
        connectTimeout: 30000,
        receiveTimeout: 60000,
      );
      if (data['code'] == 200) {
        return (data['data'] as List<dynamic>)
            .map((item) => ToolCall.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      throw AIServiceException(data['message'] ?? '获取工具列表失败');
    } on DioException catch (e) {
      throw AIServiceException(_handleDioError(e));
    } catch (e) {
      if (e is AIServiceException) rethrow;
      throw AIServiceException('获取工具列表失败: $e');
    }
  }

  String _handleDioError(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
        return '连接超时，请检查网络';
      case DioExceptionType.sendTimeout:
        return '请求超时，请重试';
      case DioExceptionType.receiveTimeout:
        return '响应超时，请重试';
      case DioExceptionType.badResponse:
        final statusCode = e.response?.statusCode;
        if (statusCode == 401) return '认证失败，请重新登录';
        if (statusCode == 403) return '无访问权限';
        if (statusCode == 404) return 'AI服务不存在';
        if (statusCode == 429) return '请求过于频繁，请稍后';
        return '服务器错误: $statusCode';
      case DioExceptionType.cancel:
        return '请求被取消';
      default:
        return '网络异常: ${e.message}';
    }
  }

  void dispose() {
    _instance = null;
  }
}

class AIServiceException implements Exception {
  final String message;
  AIServiceException(this.message);

  @override
  String toString() => message;
}