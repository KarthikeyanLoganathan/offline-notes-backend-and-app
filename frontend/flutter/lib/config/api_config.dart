import 'dart:io';
import 'package:flutter/foundation.dart';

class ApiConfig {
  // Use 10.0.2.2 for Android Emulator, localhost for others
  static String get baseUrl {
    if (kIsWeb) return 'http://localhost:3000/api';
    if (Platform.isAndroid) return 'http://10.0.2.2:3000/api';
    return 'http://127.0.0.1:3000/api';
  }
  
  static const Duration timeout = Duration(seconds: 30);
  
  static const Map<String, String> headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };
}
