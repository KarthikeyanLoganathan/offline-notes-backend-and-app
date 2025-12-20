import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'entities/note_entity.dart';

class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  DatabaseHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('notes_app.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);

    return await openDatabase(
      path,
      version: 1,
      onCreate: _createDB,
    );
  }

  Future<void> _createDB(Database db, int version) async {
    const idType = 'TEXT';
    const textType = 'TEXT NOT NULL';
    const boolType = 'INTEGER NOT NULL';
    const integerType = 'INTEGER NOT NULL';
    const timestampType = 'TEXT NOT NULL';
    const nullableTimestampType = 'TEXT';

    // Users table
    await db.execute('''
      CREATE TABLE users (
        id $idType PRIMARY KEY,
        email $textType,
        first_name $textType,
        last_name $textType,
        created_at $timestampType
      )
    ''');

    // Notes table
    await db.execute('''
      CREATE TABLE notes (
        local_id $idType PRIMARY KEY,
        id $nullableTimestampType, -- Server ID can be null initially
        user_id $textType,
        title $textType,
        content $textType,
        is_deleted $boolType DEFAULT 0,
        deleted_at $nullableTimestampType,
        created_at $timestampType,
        updated_at $timestampType,
        sync_status $integerType DEFAULT 0
      )
    ''');

    // Labels table
    await db.execute('''
      CREATE TABLE labels (
        local_id $idType PRIMARY KEY,
        id $nullableTimestampType,
        user_id $textType,
        name $textType,
        color $textType,
        created_at $timestampType,
        sync_status $integerType DEFAULT 0
      )
    ''');

    // Note Labels Junction table
    await db.execute('''
      CREATE TABLE note_labels (
        note_local_id $textType,
        label_local_id $textType,
        sync_status $integerType DEFAULT 0,
        PRIMARY KEY (note_local_id, label_local_id),
        FOREIGN KEY (note_local_id) REFERENCES notes (local_id) ON DELETE CASCADE,
        FOREIGN KEY (label_local_id) REFERENCES labels (local_id) ON DELETE CASCADE
      )
    ''');

    // Sync Metadata table
    await db.execute('''
      CREATE TABLE sync_metadata (
        type $textType PRIMARY KEY, -- 'global' or 'label:ID'
        last_synced_at $timestampType
      )
    ''');
  }

  Future<void> close() async {
    final db = await instance.database;
    db.close();
  }
}
