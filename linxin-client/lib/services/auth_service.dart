import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../config/api_config.dart';
import '../models/user.dart';
import 'http_service.dart';
import 'websocket_service.dart';

class AuthService extends ChangeNotifier {
  static final AuthService _instance = AuthService._internal();
  factory AuthService() => _instance;
  AuthService._internal();

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
    final token = prefs.getString('auth_token');
    final userId = prefs.getString('auth_user_id');
    final username = prefs.getString('auth_username');
    final nickname = prefs.getString('auth_nickname');

    if (token != null && userId != null) {
      _httpService.setToken(token);
      _currentUser = User(
        id: int.parse(userId),
        username: username ?? '',
        nickname: nickname ?? username ?? '',
      );
      WebSocketService().setToken(token);
      await WebSocketService().connect();
    }
    _markInitialized();
  }

  Future<User?> register({
    required String username,
    required String password,
    String? nickname,
  }) async {
    final response = await _httpService.post(
      ApiConfig.register,
      data: {
        'username': username,
        'password': password,
        'nickname': nickname ?? username,
      },
    );
    return User.fromJson(response.data);
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

    final Map<String, dynamic> data = response.data;
    final String token = data['token'];

    _httpService.setToken(token);
    await _persistAuthData(token, data['userId'], data['username'], data['nickname']);

    _currentUser = User(
      id: data['userId'],
      username: data['username'],
      nickname: data['nickname'],
    );

    WebSocketService().setToken(token);
    await WebSocketService().connect();

    notifyListeners();
    return _currentUser;
  }

  Future<void> _persistAuthData(String token, int userId, String username, String nickname) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('auth_token', token);
    await prefs.setString('auth_user_id', userId.toString());
    await prefs.setString('auth_username', username);
    await prefs.setString('auth_nickname', nickname);
  }

  Future<void> _clearAuthData() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('auth_token');
    await prefs.remove('auth_user_id');
    await prefs.remove('auth_username');
    await prefs.remove('auth_nickname');
  }

  void logout() {
    _currentUser = null;
    _httpService.clearToken();
    WebSocketService().disconnect();
    _clearAuthData();
    notifyListeners();
  }

  Future<User?> getCurrentUser() async {
    return _currentUser;
  }
}