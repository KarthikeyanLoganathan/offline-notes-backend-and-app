import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:uuid/uuid.dart';
import '../models/note.dart';
import '../services/notes_service.dart';
import '../database/database_helper.dart';
import '../database/entities/note_entity.dart';

class NotesRepository {
  final NotesService _notesService;
  final DatabaseHelper _databaseHelper;
  final Uuid _uuid;

  NotesRepository({
    NotesService? notesService,
    DatabaseHelper? databaseHelper,
  })  : _notesService = notesService ?? NotesService(),
        _databaseHelper = databaseHelper ?? DatabaseHelper.instance,
        _uuid = const Uuid();

  Future<List<Note>> getNotes(String userId) async {
    final db = await _databaseHelper.database;
    final result = await db.query(
      'notes',
      where: 'user_id = ? AND is_deleted = 0',
      whereArgs: [userId],
      orderBy: 'updated_at DESC',
    );

    return result.map((map) {
      final entity = NoteEntity.fromMap(map);
      return Note(
        id: entity.localId,
        serverId: entity.id,
        title: entity.title,
        content: entity.content,
        updatedAt: entity.updatedAt,
        isSynced: entity.syncStatus == 0,
      );
    }).toList();
  }

  Future<Note> createNote(String userId, String title, String content) async {
    final localId = _uuid.v4();
    final now = DateTime.now();

    final entity = NoteEntity(
      localId: localId,
      userId: userId,
      title: title,
      content: content,
      createdAt: now,
      updatedAt: now,
      syncStatus: 1, // Pending create
    );

    final db = await _databaseHelper.database;
    await db.insert('notes', entity.toMap());
    
    // Attempt sync if online
    if (await _isOnline()) {
      try {
        await _syncCreate(entity);
        // If sync succeeds, we need to fetch the updated entity to get serverId
        // But for UI immediate return, we just return the local one as updated in DB
        return Note(
          id: localId,
          title: title,
          content: content,
          updatedAt: now,
          isSynced: true,
        );
      } catch (e) {
        print('Sync failed immediately, will retry later: $e');
      }
    }

    return Note(
      id: localId,
      title: title,
      content: content,
      updatedAt: now,
      isSynced: false,
    );
  }

  Future<void> updateNote(String userId, String localId, String title, String content) async {
    final now = DateTime.now();
    final db = await _databaseHelper.database;

    // Get existing note to check if it has server ID
    final result = await db.query('notes', where: 'local_id = ?', whereArgs: [localId]);
    if (result.isEmpty) return;
    
    final existingParams = NoteEntity.fromMap(result.first);
    
    final updatedEntity = NoteEntity(
      id: existingParams.id, // Keep server ID
      localId: localId,
      userId: userId,
      title: title,
      content: content,
      createdAt: existingParams.createdAt,
      updatedAt: now,
      syncStatus: 2, // Pending update
    );

    await db.update(
      'notes',
      updatedEntity.toMap(),
      where: 'local_id = ?',
      whereArgs: [localId],
    );

    if (await _isOnline()) {
       try {
         await _syncUpdate(updatedEntity);
       } catch (e) {
         print('Update sync failed: $e');
       }
    }
  }

  Future<void> deleteNote(String localId) async {
    final db = await _databaseHelper.database;
    final now = DateTime.now();

    // Soft delete locally first
    await db.update(
      'notes',
      {
        'is_deleted': 1,
        'deleted_at': now.toIso8601String(),
        'sync_status': 3, // Pending delete
        'updated_at': now.toIso8601String(), 
      },
      where: 'local_id = ?',
      whereArgs: [localId],
    );
    
    // Get note to find server ID
    final result = await db.query('notes', where: 'local_id = ?', whereArgs: [localId]);
    if (result.isNotEmpty) {
      final entity = NoteEntity.fromMap(result.first);
      if (await _isOnline()) {
        try {
          await _syncDelete(entity);
        } catch (_) {}
      }
    }
  }

  // --- Sync Helpers ---

  Future<void> _syncCreate(NoteEntity note) async {
    final serverNote = await _notesService.createNote({
      'title': note.title,
      'content': note.content,
      // Backend expects standard fields
    });

    final db = await _databaseHelper.database;
    await db.update(
      'notes',
      {
        'id': serverNote['id'],
        'sync_status': 0,
        'updated_at': serverNote['updatedAt'] ?? note.updatedAt.toIso8601String(),
      },
      where: 'local_id = ?',
      whereArgs: [note.localId],
    );
  }

  Future<void> _syncUpdate(NoteEntity note) async {
    if (note.id == null) return; // Can't update if not yet on server
    
    final serverNote = await _notesService.updateNote(note.id!, {
      'title': note.title,
      'content': note.content,
    });

    final db = await _databaseHelper.database;
    await db.update(
      'notes',
      {
        'sync_status': 0,
         'updated_at': serverNote['updatedAt'] ?? note.updatedAt.toIso8601String(),
      },
      where: 'local_id = ?',
      whereArgs: [note.localId],
    );
  }

  Future<void> _syncDelete(NoteEntity note) async {
    if (note.id == null) return;
    
    await _notesService.deleteNote(note.id!);
     final db = await _databaseHelper.database;
    await db.update(
      'notes',
      {'sync_status': 0},
      where: 'local_id = ?',
      whereArgs: [note.localId],
    );
  }

  Future<bool> _isOnline() async {
    final connectivityResult = await Connectivity().checkConnectivity();
    return connectivityResult != ConnectivityResult.none;
  }
}
