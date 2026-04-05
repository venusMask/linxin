class ApiConfig {
  static const String baseUrl = 'http://127.0.0.1:9099/lxa';

  // 认证/用户相关接口
  static const String register = '/auth/register';
  static const String login = '/auth/login';
  static const String userInfo = '/auth/userinfo';
  static const String searchUser = '/auth/search';
  static const String sendEmailCode = '/auth/email/send-code';
  static const String updateProfile = '/auth/profile';
  static const String updateEmail = '/auth/email';
  static const String updatePassword = '/auth/password';

  // 聊天相关接口
  static const String conversationList = '/chat/conversations';
  static const String getOrCreateConversation = '/chat/conversations';
  static const String sendMessage = '/chat/messages';
  static const String getMessages = '/chat/messages';
  static const String markMessagesRead = '/chat/messages';

  // 好友相关接口
  static const String friendList = '/friends/list';
  static const String friendApply = '/friends/apply';
  static const String friendApplyReceived = '/friends/apply/received';
  static const String friendApplyHandle = '/friends/apply/handle';
  static String friendDelete(String id) => '/friends/$id';

  // 群组相关接口
  static const String groupCreate = '/group/create';
  static const String groupMy = '/group/my';
  static const String groupMessages = '/chat/group/messages';
}