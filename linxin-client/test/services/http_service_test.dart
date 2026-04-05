import 'package:flutter_test/flutter_test.dart';
import 'package:dio/dio.dart';
import 'package:lin_xin/services/http_service.dart';
import 'package:lin_xin/config/api_config.dart';

class ManualFakeDio implements Dio {
  BaseOptions options = BaseOptions();
  Interceptors get interceptors => Interceptors();
  
  Response? mockResponse;
  DioException? mockException;

  @override
  Future<Response<T>> get<T>(String path, {Object? data, Map<String, dynamic>? queryParameters, Options? options, CancelToken? cancelToken, ProgressCallback? onReceiveProgress}) async {
    if (mockException != null) throw mockException!;
    return mockResponse as Response<T>;
  }

  @override
  Future<Response<T>> post<T>(String path, {Object? data, Map<String, dynamic>? queryParameters, Options? options, CancelToken? cancelToken, ProgressCallback? onSendProgress, ProgressCallback? onReceiveProgress}) async {
    if (mockException != null) throw mockException!;
    return mockResponse as Response<T>;
  }

  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  // HttpService is a singleton, tricky to test its internal Dio without setMock for Dio.
  // But we can test its methods if we expose setMock for HttpService itself.
  
  group('HttpService API Tests', () {
    late ManualFakeHttpService fakeHttpService;

    setUp(() {
      fakeHttpService = ManualFakeHttpService();
      HttpService.setMock(fakeHttpService);
    });

    test('applyAddFriend sends correct friendId', () async {
      await fakeHttpService.applyAddFriend('user123', remark: 'Hi');
      
      expect(fakeHttpService.postCalls.length, 1);
      expect(fakeHttpService.postCalls[0]['path'], ApiConfig.friendApply);
      expect(fakeHttpService.postCalls[0]['data']['friendId'], 'user123');
      expect(fakeHttpService.postCalls[0]['data']['remark'], 'Hi');
    });

    test('sendMessage sends correct parameters', () async {
      await fakeHttpService.sendMessage(
        receiverId: 'user456',
        messageType: 1,
        content: 'Hello',
      );
      
      expect(fakeHttpService.postCalls.length, 1);
      expect(fakeHttpService.postCalls[0]['path'], ApiConfig.sendMessage);
      expect(fakeHttpService.postCalls[0]['data']['receiverId'], 'user456');
      expect(fakeHttpService.postCalls[0]['data']['content'], 'Hello');
    });
  });
}

class ManualFakeHttpService implements HttpService {
  List<Map<String, dynamic>> postCalls = [];

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postCalls.add({'path': path, 'data': data});
    return Response(data: {'code': 200, 'data': {}}, requestOptions: RequestOptions(path: path));
  }

  @override
  Future<void> applyAddFriend(dynamic toUserId, {String? remark}) async {
    await post(ApiConfig.friendApply, data: {
      'friendId': toUserId,
      'remark': remark ?? '你好，我想加你为好友',
    });
  }

  @override
  Future<Map<String, dynamic>> sendMessage({required String receiverId, required int messageType, required String content, String? extra}) async {
    final response = await post(ApiConfig.sendMessage, data: {
      'receiverId': receiverId,
      'messageType': messageType,
      'content': content,
      'extra': extra,
    });
    return {};
  }

  @override noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
  @override void setToken(String token) {}
  @override void clearToken() {}
  @override Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) async => Response(requestOptions: RequestOptions(path: path));
}
