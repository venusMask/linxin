import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:lin_xin/config/api_config.dart';
import 'package:lin_xin/modules/auth/user.dart';
import 'package:lin_xin/core/service/http_service.dart';
import 'package:lin_xin/core/service/websocket_service.dart';
import 'package:lin_xin/config/test_config.dart';

class AuthService extends ChangeNotifier {
  static AuthService _instance = AuthService._internal();
  factory AuthService() => _instance;
  AuthService._internal();

  @visibleForTesting
  static void setMock(AuthService mock) {
    _instance = mock;
  }

  final HttpService _httpService = HttpService();
  User? _currentUser;
  bool _isInitialized = false;

  bool get isInitialized => _isInitialized;

  void _markInitialized() {
    _isInitialized = true;
    notifyListeners();
  }

  User? get currentUser => _currentUser;
  bool get isLoggedIn => _currentUser != null;

  Future<void> initialize() async {
    final prefs = await SharedPreferences.getInstance();
    final prefix = TestConfig.storagePrefix;
    final token = prefs.getString('${prefix}auth_token');
    final userId = prefs.getString('${prefix}auth_user_id');

    if (token != null && userId != null) {
      _httpService.setToken(token);
      try {
        final response = await _httpService.get(ApiConfig.userInfo);
        // HttpService 已拆包，直接使用 response.data
        _currentUser = User.fromJson(response.data);
      } catch (e) {
        // 如果请求失败，可能是 Token 已失效，清除本地数据
        debugPrint('Auto login failed: $e');
        await _clearAuthData();
        _httpService.clearToken();
        _currentUser = null;
      }
      WebSocketService().setToken(token);
      await WebSocketService().connect();
    }
    _markInitialized();
  }

  Future<User?> register({
    required String username,
    required String password,
    String? nickname,
    required String email,
    required String verificationCode,
  }) async {
    final response = await _httpService.post(
      ApiConfig.register,
      data: {
        'username': username,
        'password': password,
        'nickname': nickname ?? username,
        'email': email,
        'verificationCode': verificationCode,
      },
    );
    // 从 response.data 中提取数据 (HttpService已拆包)
    return User.fromJson(response.data);
  }

  Future<bool> sendEmailVerificationCode(String email, {String type = 'register'}) async {
    try {
      await _httpService.post(
        ApiConfig.sendEmailCode,
        data: {'email': email, 'type': type},
      );
      return true;
    } catch (e) {
      rethrow;
    }
  }

  Future<void> updateProfile({
    String? nickname,
    String? username,
    String? avatar,
    String? signature,
    int? gender,
  }) async {
    await _httpService.put(
      ApiConfig.updateProfile,
      data: {
        'nickname': nickname,
        'username': username,
        'avatar': avatar,
        'signature': signature,
        'gender': gender,
      },
    );

    if (_currentUser != null) {
      _currentUser = _currentUser!.copyWith(
        nickname: nickname,
        username: username,
        avatar: avatar,
        signature: signature,
        gender: gender,
      );
      final prefs = await SharedPreferences.getInstance();
      final prefix = TestConfig.storagePrefix;
      if (nickname != null) await prefs.setString('${prefix}auth_nickname', nickname);
      if (username != null) await prefs.setString('${prefix}auth_username', username);
      notifyListeners();
    }
  }

  Future<void> updateEmail({
    required String password,
    required String newEmail,
    required String code,
  }) async {
    await _httpService.put(
      ApiConfig.updateEmail,
      data: {
        'password': password,
        'newEmail': newEmail,
        'code': code,
      },
    );

    if (_currentUser != null) {
      _currentUser = _currentUser!.copyWith(email: newEmail);
      notifyListeners();
    }
  }

  Future<void> updatePassword({
    required String oldPassword,
    required String newPassword,
  }) async {
    await _httpService.put(
      ApiConfig.updatePassword,
      data: {
        'oldPassword': oldPassword,
        'newPassword': newPassword,
      },
    );
  }

  Future<User?> login({
    required String username,
    required String password,
  }) async {
    final response = await _httpService.post(
      ApiConfig.login,
      data: {
        'username': username,
        'password': password,
      },
    );

    // 关键点：HttpService 已经在拦截器中执行了 response.data = respData['data']
    // 所以这里的 response.data 直接就是用户信息 Map
    final Map<String, dynamic> userData = response.data;
    
    final String token = userData['token'];
    // 后端返回的是 userId，映射为 User 模型中的 id
    final dynamic userId = userData['userId'];

    _httpService.setToken(token);
    await _persistAuthData(token, userId, userData['username'], userData['nickname']);

    _currentUser = User(
      id: userId,
      username: userData['username'],
      nickname: userData['nickname'],
    );

    WebSocketService().setToken(token);
    await WebSocketService().connect();

    notifyListeners();
    return _currentUser;
  }

  Future<void> _persistAuthData(String token, dynamic userId, String username, String nickname) async {
    final prefs = await SharedPreferences.getInstance();
    final prefix = TestConfig.storagePrefix;
    await prefs.setString('${prefix}auth_token', token);
    await prefs.setString('${prefix}auth_user_id', userId.toString());
    await prefs.setString('${prefix}auth_username', username);
    await prefs.setString('${prefix}auth_nickname', nickname);
  }

  Future<void> _clearAuthData() async {
    final prefs = await SharedPreferences.getInstance();
    final prefix = TestConfig.storagePrefix;
    await prefs.remove('${prefix}auth_token');
    await prefs.remove('${prefix}auth_user_id');
    await prefs.remove('${prefix}auth_username');
    await prefs.remove('${prefix}auth_nickname');
  }

  Future<void> logout() async {
    _currentUser = null;
    _httpService.clearToken();
    WebSocketService().disconnect();
    await _clearAuthData();
    // 移除 clearUserData() 以防止每次退出导致消息数据丢失
    // if (!TestConfig.isWeb) {
    //   await DatabaseService().clearUserData();
    // }
    notifyListeners();
  }

  Future<User?> getCurrentUser() async {
    return _currentUser;
  }
}