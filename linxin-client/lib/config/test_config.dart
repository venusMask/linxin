import 'package:flutter/foundation.dart';

class TestConfig {
  /// 获取当前测试实例 ID，通过 --dart-define=TEST_PROFILE=1 传入
  static const String profileId = String.fromEnvironment('TEST_PROFILE', defaultValue: '');

  /// 获取文件/Key 的后缀，如果是默认环境则为空，否则为 _profileId
  static String get suffix {
    if (profileId.isEmpty) return '';
    return '_$profileId';
  }

  /// 是否是 Web 环境
  static bool get isWeb => kIsWeb;

  /// 获取存储 Key 的前缀
  static String get storagePrefix {
    if (profileId.isEmpty) return '';
    return 'p${profileId}_';
  }
}
