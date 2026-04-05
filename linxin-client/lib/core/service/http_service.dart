import 'package:dio/dio.dart';
import 'package:meta/meta.dart';
import 'package:lin_xin/config/api_config.dart';
import 'package:lin_xin/core/service/log_service.dart';

class HttpService {
  static HttpService _instance = HttpService._internal();
  factory HttpService() => _instance;

  @visibleForTesting
  static void setMock(HttpService mock) {
    _instance = mock;
  }

  late Dio _dio;
  String? _token;

  HttpService._internal() {
    _dio = Dio(BaseOptions(
      baseUrl: ApiConfig.baseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),
      contentType: 'application/json',
    ));

    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        if (_token != null) {
          options.headers['Authorization'] = 'Bearer $_token';
        }
        LogService.info('HTTP Request: [${options.method}] ${options.path}\nData: ${options.data}\nParams: ${options.queryParameters}');
        return handler.next(options);
      },
      onResponse: (response, handler) {
        LogService.info('HTTP Response: [${response.requestOptions.method}] ${response.requestOptions.path}\nStatus: ${response.statusCode}\nData: ${response.data}');

        final dynamic respData = response.data;
        if (respData is Map<String, dynamic>) {
          final int code = respData['code'] ?? 500;
          if (code == 200) {
            response.data = respData['data'];
            return handler.next(response);
          } else {
            return handler.reject(
              DioException(
                requestOptions: response.requestOptions,
                response: response,
                message: respData['message'] ?? '未知错误',
                type: DioExceptionType.badResponse,
              ),
            );
          }
        }
        return handler.next(response);
      },
      onError: (e, handler) {
        LogService.error('HTTP Error: [${e.requestOptions.method}] ${e.requestOptions.path}\nError: ${e.message}\nResponse: ${e.response?.data}', e);

        String message = '网络连接异常';
        if (e.type == DioExceptionType.connectionTimeout) {
          message = '连接超时';
        } else if (e.type == DioExceptionType.badResponse) {
          if (e.response?.data is Map) {
            message = e.response?.data['message'] ?? '服务器响应异常';
          } else {
            message = e.message ?? '服务器响应异常';
          }
        }

        return handler.reject(
          e.copyWith(message: message),
        );
      },
    ));
  }

  void setToken(String token) {
    _token = token;
  }

  void clearToken() {
    _token = null;
  }

  Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) async {
    return await _dio.get(path, queryParameters: queryParameters);
  }

  Future<Response> post(String path, {dynamic data}) async {
    return await _dio.post(path, data: data);
  }

  Future<Response> put(String path, {dynamic data}) async {
    return await _dio.put(path, data: data);
  }

  Future<Response> delete(String path) async {
    return await _dio.delete(path);
  }

  Future<dynamic> requestRaw(String path, {
    dynamic data,
    int? connectTimeout,
    int? receiveTimeout,
  }) async {
    final options = BaseOptions(
      baseUrl: ApiConfig.baseUrl,
      connectTimeout: connectTimeout != null ? Duration(milliseconds: connectTimeout) : const Duration(seconds: 10),
      receiveTimeout: receiveTimeout != null ? Duration(milliseconds: receiveTimeout) : const Duration(seconds: 10),
      contentType: 'application/json',
    );
    final dio = Dio(options);
    if (_token != null) {
      dio.options.headers['Authorization'] = 'Bearer $_token';
    }
    final response = await dio.post(path, data: data);
    return response.data;
  }

  Future<List<dynamic>> searchUsers(String keyword) async {
    final response = await get(ApiConfig.searchUser, queryParameters: {'keyword': keyword});
    return response.data;
  }

  Future<void> applyAddFriend(dynamic toUserId, {String? remark}) async {
    await post(ApiConfig.friendApply, data: {
      'friendId': toUserId,
      'remark': remark ?? '你好，我想加你为好友',
    });
  }

  Future<List<dynamic>> getReceivedApplies() async {
    final response = await get(ApiConfig.friendApplyReceived);
    return response.data;
  }

  Future<void> handleFriendApply(dynamic applyId, int status) async {
    await post(ApiConfig.friendApplyHandle, data: {
      'applyId': applyId,
      'status': status,
    });
  }

  Future<Map<String, dynamic>> sendMessage({
    required String receiverId,
    required int messageType,
    required String content,
    String? extra,
  }) async {
    final response = await post(ApiConfig.sendMessage, data: {
      'receiverId': receiverId,
      'messageType': messageType,
      'content': content,
      'extra': extra,
    });
    return response.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> sendGroupMessage({
    required String groupId,
    required int messageType,
    required String content,
    String? extra,
  }) async {
    final response = await post(ApiConfig.sendMessage, data: {
      'messageType': messageType,
      'content': content,
      'extra': extra,
      'conversationType': 1,
      'groupId': groupId,
    });
    return response.data as Map<String, dynamic>;
  }

  Future<List<dynamic>> getConversationList({int pageNum = 1, int pageSize = 20}) async {
    final response = await get(ApiConfig.conversationList, queryParameters: {
      'pageNum': pageNum,
      'pageSize': pageSize,
    });
    return response.data;
  }

  Future<Map<String, dynamic>> getOrCreateConversation(String peerId) async {
    final response = await post('${ApiConfig.getOrCreateConversation}/$peerId', data: {});
    return response.data;
  }

  Future<List<dynamic>> getMessageList(String conversationId, {int pageNum = 1, int pageSize = 20}) async {
    final response = await get('${ApiConfig.getMessages}/$conversationId', queryParameters: {
      'pageNum': pageNum,
      'pageSize': pageSize,
    });
    return response.data;
  }

  Future<void> markMessagesAsRead(String conversationId) async {
    await post('${ApiConfig.markMessagesRead}/$conversationId/read', data: {});
  }
}