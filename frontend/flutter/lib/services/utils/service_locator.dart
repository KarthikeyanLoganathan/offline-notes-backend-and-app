import 'package:shared_preferences/shared_preferences.dart';

// Global access to shared preferences or other singletons if needed
late SharedPreferences prefs;

Future<void> setupServiceLocator() async {
  prefs = await SharedPreferences.getInstance();
}
