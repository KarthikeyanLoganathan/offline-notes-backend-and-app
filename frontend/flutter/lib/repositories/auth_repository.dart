import 'package:shared_preferences/shared_preferences.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../database/database_helper.dart';
import '../database/entities/user_entity.dart';

class AuthRepository {
  final AuthService _authService;
  final DatabaseHelper _databaseHelper;

  AuthRepository({
    AuthService? authService,
    DatabaseHelper? databaseHelper,
  })  : _authService = authService ?? AuthService(),
        _databaseHelper = databaseHelper ?? DatabaseHelper.instance;

  Future<User?> login(String email, String password) async {
    final response = await _authService.login(email, password);
    
    final token = response['token'];
    final userData = response['user'];
    
    if (token != null && userData != null) {
      await _saveSession(token, userData);
      return User.fromJson(userData);
    }
    return null;
  }

  Future<User?> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  }) async {
    // Registration purely on server side, usually requires email verification
    // But for simplicity/flow, we might autosign in or just return success
    final response = await _authService.register(
      email: email,
      password: password,
      firstName: firstName,
      lastName: lastName,
    );
    
    // Check if registration returns token (some APIs do, some don't)
    if (response['token'] != null && response['user'] != null) {
      await _saveSession(response['token'], response['user']);
      return User.fromJson(response['user']);
    }
    
    return null;
  }

  Future<void> logout() async {
    await _authService.logout();
    await _clearSession();
  }

  Future<User?> getCurrentUser() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('auth_token');
    final userId = prefs.getString('user_id');

    if (token != null && userId != null) {
      // Try to get from local DB
      final db = await _databaseHelper.database;
      final results = await db.query(
        'users',
        where: 'id = ?',
        whereArgs: [userId],
      );

      if (results.isNotEmpty) {
        final entity = UserEntity.fromMap(results.first);
        return User(
          id: entity.id,
          email: entity.email,
          firstName: entity.firstName,
          lastName: entity.lastName,
        );
      }
    }
    return null;
  }

  Future<void> _saveSession(String token, Map<String, dynamic> userData) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('auth_token', token);
    await prefs.setString('user_id', userData['id']);
    
    // Save user to local DB
    final userEntity = UserEntity(
      id: userData['id'],
      email: userData['email'],
      firstName: userData['firstName'] ?? userData['first_name'],
      lastName: userData['lastName'] ?? userData['last_name'],
      createdAt: DateTime.now(), // Approximate if not from server
    );
    
    final db = await _databaseHelper.database;
    await db.insert(
      'users',
      userEntity.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace, // Upsert
    );
  }

  Future<void> _clearSession() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('auth_token');
    await prefs.remove('user_id');
  }
}
