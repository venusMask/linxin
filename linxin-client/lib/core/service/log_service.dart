import 'dart:io';
import 'package:logger/logger.dart';
import 'package:path_provider/path_provider.dart';
import 'package:lin_xin/config/test_config.dart';

class LogService {
  static final LogService _instance = LogService._internal();
  factory LogService() => _instance;

  late Logger _logger;
  File? _logFile;
  Directory? _logDir;
  static const int _maxFileSize = 3 * 1024 * 1024; // 3MB

  LogService._internal() {
    _logger = Logger(
      printer: PrettyPrinter(
        methodCount: 0,
        errorMethodCount: 8,
        lineLength: 120,
        colors: true,
        printEmojis: true,
        dateTimeFormat: DateTimeFormat.onlyTimeAndSinceStart,
      ),
      output: MultiOutput([
        ConsoleOutput(),
        _FileOutput(this),
      ]),
    );
  }

  Future<void> init() async {
    if (TestConfig.isWeb) {
      i('LogService: Running on Web, skipping file init.');
      return;
    }
    
    try {
      final docDir = await getApplicationDocumentsDirectory();
      _logDir = Directory('${docDir.path}/logs');
      if (!await _logDir!.exists()) {
        await _logDir!.create(recursive: true);
      }
      
      final logName = 'lin_xin${TestConfig.suffix}.log';
      _logFile = File('${_logDir!.path}/$logName');
      if (!await _logFile!.exists()) {
        await _logFile!.create();
      }
      
      // 初始化时检查一次是否需要滚动
      await _checkAndRotate();
      
      i('日志服务已初始化，基础路径: ${_logFile!.path}');
    } catch (e) {
      _logger.e('初始化日志文件错误: $e');
    }
  }

  Future<void> _checkAndRotate() async {
    if (_logFile == null || !await _logFile!.exists()) return;
    
    final length = await _logFile!.length();
    if (length >= _maxFileSize) {
      await _performRotation();
    }
  }

  Future<void> _performRotation() async {
    try {
      int nextNumber = 0;
      final List<FileSystemEntity> files = _logDir!.listSync();
      final regExp = RegExp(r'lin_xin_(\d+)\.log$');
      
      int maxNum = -1;
      for (var file in files) {
        final match = regExp.firstMatch(file.path);
        if (match != null) {
          int num = int.parse(match.group(1)!);
          if (num > maxNum) maxNum = num;
        }
      }
      nextNumber = maxNum + 1;
      
      final logName = 'lin_xin${TestConfig.suffix}_$nextNumber.log';
      final newPath = '${_logDir!.path}/$logName';
      await _logFile!.rename(newPath);
      
      // 创建新的活跃日志文件
      final activeLogName = 'lin_xin${TestConfig.suffix}.log';
      _logFile = File('${_logDir!.path}/$activeLogName');
      await _logFile!.create();
      
      i('日志已滚动，旧文件重命名为: $logName');
    } catch (e) {
      _logger.e('执行日志滚动失败: $e');
    }
  }

  // 静态调用方法
  static void debug(String message) => _instance.d(message);
  static void info(String message) => _instance.i(message);
  static void warn(String message) => _instance.w(message);
  static void warning(String message) => _instance.w(message);
  static void error(String message, [dynamic error, StackTrace? stackTrace]) =>
      _instance.e(message, error, stackTrace);
  static void log(Level level, String message) => _instance._logger.log(level, message);

  // 实例方法
  void d(String message) => _logger.d(message);
  void i(String message) => _logger.i(message);
  void w(String message) => _logger.w(message);
  void e(String message, [dynamic error, StackTrace? stackTrace]) => 
      _logger.e(message, error: error, stackTrace: stackTrace);

  Future<String> getLogs() async {
    if (_logFile != null && await _logFile!.exists()) {
      return await _logFile!.readAsString();
    }
    return '无可用主日志';
  }
}

class _FileOutput extends LogOutput {
  final LogService _service;

  _FileOutput(this._service);

  @override
  void output(OutputEvent event) {
    if (_service._logFile != null) {
      final time = DateTime.now().toString();
      for (var line in event.lines) {
        _service._logFile!.writeAsStringSync(
          '[$time] ${event.level.name.toUpperCase()}: $line\n',
          mode: FileMode.append,
        );
      }
      // 每次写入后异步检查文件大小，触发滚动
      _service._checkAndRotate();
    }
  }
}
