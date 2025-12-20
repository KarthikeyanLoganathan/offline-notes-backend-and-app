class Note {
  final String id; // This serves as the UI stable ID (local_id)
  final String? serverId;
  final String title;
  final String content;
  final DateTime updatedAt;
  final bool isSynced;

  Note({
    required this.id,
    this.serverId,
    required this.title,
    required this.content,
    required this.updatedAt,
    this.isSynced = true,
  });

  Note copyWith({
    String? title,
    String? content,
    String? serverId,
    bool? isSynced,
  }) {
    return Note(
      id: id,
      serverId: serverId ?? this.serverId,
      title: title ?? this.title,
      content: content ?? this.content,
      updatedAt: DateTime.now(), // Always update timestamp on modify
      isSynced: isSynced ?? this.isSynced,
    );
  }
}
