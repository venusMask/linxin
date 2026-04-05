import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'package:meta/meta.dart';
import '../config/test_config.dart';
import 'log_service.dart';

class DatabaseService {
  static DatabaseService _instance = DatabaseService._internal();
  factory DatabaseService() => _instance;
  DatabaseService._internal();

  @visibleForTesting
  static void setMock(DatabaseService mock) {
    _instance = mock;
  }

  static Database? _database;
  static const int _dbVersion = 1;

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    if (TestConfig.isWeb) {
      LogService.warn('DatabaseService: SQLite is not supported on Web. Database operations will fail.');
      // 这里如果直接返回 null 可能会导致后续调用崩溃，但在 Web 端目前暂不考虑离线存储
      throw UnsupportedError('SQLite is not supported on Web');
    }

    final dbPath = await getDatabasesPath();
    final dbName = 'lin_xin${TestConfig.suffix}.db';
    final path = join(dbPath, dbName);

    LogService.info('Initializing database at: $path');

    return await openDatabase(
      path,
      version: _dbVersion,
      onCreate: _onCreate,
      onUpgrade: _onUpgrade,
    );
  }

  Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE messages (
        id TEXT PRIMARY KEY,
        conversation_id TEXT NOT NULL,
        sender_id TEXT NOT NULL,
        content TEXT NOT NULL,
        message_type INTEGER DEFAULT 1,
        status INTEGER DEFAULT 1,
        created_at TEXT NOT NULL,
        is_read INTEGER DEFAULT 0,
        deleted_at TEXT
      )
    ''');

    await db.execute('''
      CREATE TABLE conversations (
        id TEXT PRIMARY KEY,
        type TEXT NOT NULL,
        name TEXT,
        avatar_url TEXT,
        owner_id TEXT,
        unread_count INTEGER DEFAULT 0,
        last_message_id TEXT,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT
      )
    ''');

    await db.execute('''
      CREATE TABLE friends (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        friend_id TEXT NOT NULL,
        status TEXT DEFAULT 'pending',
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT
      )
    ''');

    await db.execute('''
      CREATE INDEX idx_messages_conversation ON messages(conversation_id)
    ''');

    await db.execute('''
      CREATE INDEX idx_messages_created_at ON messages(created_at)
    ''');

    await db.execute('''
      CREATE INDEX idx_conversations_updated_at ON conversations(updated_at)
    ''');
  }

  Future<void> _onUpgrade(Database db, int oldVersion, int newVersion) async {}

  Future<int> insert(String table, Map<String, dynamic> data) async {
    final db = await database;
    return await db.insert(table, data, conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<List<Map<String, dynamic>>> query(
    String table, {
    String? where,
    List<Object?>? whereArgs,
    String? orderBy,
    int? limit,
    int? offset,
  }) async {
    final db = await database;
    return await db.query(
      table,
      where: where,
      whereArgs: whereArgs,
      orderBy: orderBy,
      limit: limit,
      offset: offset,
    );
  }

  Future<int> update(
    String table,
    Map<String, dynamic> data, {
    String? where,
    List<Object?>? whereArgs,
  }) async {
    final db = await database;
    return await db.update(table, data, where: where, whereArgs: whereArgs);
  }

  Future<int> delete(
    String table, {
    String? where,
    List<Object?>? whereArgs,
  }) async {
    final db = await database;
    return await db.delete(table, where: where, whereArgs: whereArgs);
  }

  Future<List<Map<String, dynamic>>> rawQuery(String sql, [List<Object?>? arguments]) async {
    final db = await database;
    return await db.rawQuery(sql, arguments);
  }

  Future<void> clearUserData() async {
    final db = await database;
    await db.delete('messages');
    await db.delete('conversations');
    await db.delete('friends');
  }

  Future<void> close() async {
    final db = await database;
    await db.close();
    _database = null;
  }
}