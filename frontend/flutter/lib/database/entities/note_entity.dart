class NoteEntity {
  final String? id; // Server ID (nullable for offline created notes)
  final String localId; // UUID for local identification
  final String userId;
  final String title;
  final String content;
  final bool isDeleted;
  final DateTime? deletedAt;
  final DateTime createdAt;
  final DateTime updatedAt;
  final int syncStatus; // 0=synced, 1=create, 2=update, 3=delete

  NoteEntity({
    this.id,
    required this.localId,
    required this.userId,
    required this.title,
    required this.content,
    this.isDeleted = false,
    this.deletedAt,
    required this.createdAt,
    required this.updatedAt,
    this.syncStatus = 0,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'local_id': localId,
      'user_id': userId,
      'title': title,
      'content': content,
      'is_deleted': isDeleted ? 1 : 0,
      'deleted_at': deletedAt?.toIso8601String(),
      'created_at': createdAt.toIso8601String(),
      'updated_at': updatedAt.toIso8601String(),
      'sync_status': syncStatus,
    };
  }

  factory NoteEntity.fromMap(Map<String, dynamic> map) {
    return NoteEntity(
      id: map['id'],
      localId: map['local_id'],
      userId: map['user_id'],
      title: map['title'],
      content: map['content'],
      isDeleted: map['is_deleted'] == 1,
      deletedAt: map['deleted_at'] != null ? DateTime.parse(map['deleted_at']) : null,
      createdAt: DateTime.parse(map['created_at']),
      updatedAt: DateTime.parse(map['updated_at']),
      syncStatus: map['sync_status'],
    );
  }
}
