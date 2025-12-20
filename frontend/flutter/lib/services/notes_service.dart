import 'package:dio/dio.dart';
import 'api_client.dart';

class NotesService {
  final ApiClient _apiClient;

  NotesService({ApiClient? apiClient}) : _apiClient = apiClient ?? ApiClient();

  Future<List<Map<String, dynamic>>> getAllNotes() async {
    final response = await _apiClient.client.get('/notes');
    return List<Map<String, dynamic>>.from(response.data);
  }

  Future<Map<String, dynamic>> createNote(Map<String, dynamic> noteData) async {
    final response = await _apiClient.client.post('/notes', data: noteData);
    return response.data;
  }

  Future<Map<String, dynamic>> updateNote(String id, Map<String, dynamic> noteData) async {
    final response = await _apiClient.client.put('/notes/$id', data: noteData);
    return response.data;
  }

  Future<void> deleteNote(String id) async {
    await _apiClient.client.delete('/notes/$id');
  }

  Future<List<Map<String, dynamic>>> getSyncChanges(String? since) async {
    final query = since != null ? {'since': since} : null;
    final response = await _apiClient.client.get('/notes/sync/changes', queryParameters: query);
    return List<Map<String, dynamic>>.from(response.data);
  }
}
