import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:workmanager/workmanager.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../database/database_helper.dart';
import '../../repositories/sync_repository.dart';
import 'utils/service_locator.dart';

// Identify the task
const String simplePeriodicTask = "simplePeriodicTask";
const String syncTaskKey = "com.offline_notes.sync_task";

@pragma('vm:entry-point') // Mandatory if App is obfuscated or using Flutter 3.1+
void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    print("Native called background task: $task");
    
    // We need to re-init dependencies because this runs in a separate isolate
    await setupServiceLocator();
    
    if (task == syncTaskKey) {
      try {
        final prefs = await SharedPreferences.getInstance();
        final userId = prefs.getString('user_id');
        
        if (userId != null) {
          final syncRepo = SyncRepository();
          await syncRepo.synchronize(userId);
          print("Background sync completed successfully");
        } else {
          print("No user logged in, skipping background sync");
        }
      } catch (e) {
        print("Background sync failed: $e");
        return Future.value(false);
      }
    }
    
    return Future.value(true);
  });
}

class BackgroundSyncService {
  static Future<void> initialize() async {
    if (!kIsWeb && (Platform.isAndroid || Platform.isIOS)) {
      await Workmanager().initialize(
        callbackDispatcher,
        isInDebugMode: true, // Set false for production
      );
    }
  }

  static Future<void> registerPeriodicSync() async {
    if (!kIsWeb && (Platform.isAndroid || Platform.isIOS)) {
      await Workmanager().registerPeriodicTask(
        "1", // Unique name
        syncTaskKey,
        frequency: const Duration(minutes: 15),
        constraints: Constraints(
          networkType: NetworkType.connected, 
        ),
      );
    }
  }
}
