import 'package:flutter/material.dart';
import '../models/user.dart';
import '../repositories/auth_repository.dart';

class AuthProvider extends ChangeNotifier {
  final AuthRepository _authRepository;
  User? _user;
  bool _isLoading = false;
  String? _error;

  AuthProvider({AuthRepository? authRepository})
      : _authRepository = authRepository ?? AuthRepository() {
    _init(); // Auto-check session on create
  }

  User? get user => _user;
  bool get isAuthenticated => _user != null;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> _init() async {
    _isLoading = true;
    notifyListeners();
    
    try {
      _user = await _authRepository.getCurrentUser();
    } catch (e) {
      print('Auth init error: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> login(String email, String password) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _user = await _authRepository.login(email, password);
      return _user != null;
    } catch (e) {
      _error = e.toString();
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> register(String email, String password, String firstName, String lastName) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      // Typically registration might send email and not login immediately
      // Adjust based on your backend flow. Assuming it returns null if verify needed
      final result = await _authRepository.register(
        email: email,
        password: password,
        firstName: firstName,
        lastName: lastName,
      );
      
      // If result is null, it might mean "check email"
      return true; 
    } catch (e) {
      _error = e.toString();
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    await _authRepository.logout();
    _user = null;
    notifyListeners();
  }
}
