import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'config/theme_config.dart';
import 'providers/auth_provider.dart';
import 'providers/notes_provider.dart';
import 'providers/sync_provider.dart';
import 'screens/auth/login_screen.dart';
import 'screens/notes/notes_list_screen.dart';
import 'services/sync_worker.dart';
import 'services/utils/service_locator.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await setupServiceLocator();
  await BackgroundSyncService.initialize();
  await BackgroundSyncService.registerPeriodicSync();
  
  runApp(const OfflineNotesApp());
}

class OfflineNotesApp extends StatelessWidget {
  const OfflineNotesApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => NotesProvider()),
        ChangeNotifierProvider(create: (_) => SyncProvider()),
      ],
      child: Consumer<AuthProvider>(
        builder: (context, authProvider, _) {
          return MaterialApp(
            title: 'Offline Notes',
            theme: ThemeConfig.lightTheme,
            darkTheme: ThemeConfig.darkTheme,
            themeMode: ThemeMode.system,
            home: authProvider.isAuthenticated
                ? const NotesListScreen()
                : const LoginScreen(),
            debugShowCheckedModeBanner: false,
          );
        },
      ),
    );
  }
}
