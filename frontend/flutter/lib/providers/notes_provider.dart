import 'package:flutter/material.dart';
import '../models/note.dart';
import '../repositories/notes_repository.dart';

class NotesProvider extends ChangeNotifier {
  final NotesRepository _notesRepository;
  List<Note> _notes = [];
  bool _isLoading = false;
  String? _error;
  String? _currentUserId;

  NotesProvider({NotesRepository? notesRepository})
      : _notesRepository = notesRepository ?? NotesRepository();

  List<Note> get notes => _notes;
  bool get isLoading => _isLoading;
  String? get error => _error;

  void setUserId(String userId) {
    _currentUserId = userId;
    loadNotes();
  }

  Future<void> loadNotes() async {
    if (_currentUserId == null) return;

    _isLoading = true;
    notifyListeners();

    try {
      _notes = await _notesRepository.getNotes(_currentUserId!);
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> addNote(String title, String content) async {
    if (_currentUserId == null) return;
    try {
      final newNote = await _notesRepository.createNote(_currentUserId!, title, content);
      _notes.insert(0, newNote); // Optimistic generic add, though fetch usually better for sort
      notifyListeners();
      // Reload to ensure order and consistency
      await loadNotes();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  Future<void> updateNote(String localId, String title, String content) async {
    if (_currentUserId == null) return;
    try {
      await _notesRepository.updateNote(_currentUserId!, localId, title, content);
      await loadNotes();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  Future<void> deleteNote(String localId) async {
     if (_currentUserId == null) return;
    try {
      await _notesRepository.deleteNote(localId);
      _notes.removeWhere((n) => n.id == localId);
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }
}
