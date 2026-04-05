import 'package:logger/logger.dart';
import 'package:lin_xin/core/service/log_service.dart';

/// 灵信日志服务使用示例
void demonstrateLogging() {
  // 1. 记录普通信息 (Info)
  LogService.info('这是一条普通的信息日志，用于记录流程节点。');

  // 2. 记录警告信息 (Warn)
  LogService.warn('这是一条警告日志，用于记录潜在的问题但不影响流程。');

  // 3. 记录错误信息 (Error)
  try {
    throw Exception('演示异常内容');
  } catch (e, stackTrace) {
    // 可以携带 error 对象和 堆栈信息
    LogService.error('发生了一个预料之外的错误', e, stackTrace);
  }

  // 4. 记录调试信息 (Debug)
  LogService.debug('这是一条调试日志，通常只在开发阶段关注。');

  // 5. 使用指定等级记录 (Generic log)
  LogService.log(Level.info, '这是通过通用 log 方法记录的消息');
  
  // 提示：
  // 所有通过上述方法记录的内容都会：
  // 1. 打印到控制台（带颜色和格式）。
  // 2. 写入到本地文件 logs/lin_xin.log 中。
  // 3. 当 lin_xin.log 超过 3MB 时，会自动滚动重命名。
}
