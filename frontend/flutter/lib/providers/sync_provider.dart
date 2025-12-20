import 'package:flutter/material.dart';
import '../repositories/sync_repository.dart';

enum SyncStatus { idle, syncing, success, error }

class SyncProvider extends ChangeNotifier {
  final SyncRepository _syncRepository;
  SyncStatus _status = SyncStatus.idle;

  SyncProvider({SyncRepository? syncRepository})
      : _syncRepository = syncRepository ?? SyncRepository();

  SyncStatus get status => _status;

  Future<void> syncNow(String userId) async {
    if (_status == SyncStatus.syncing) return;
    
    _status = SyncStatus.syncing;
    notifyListeners();
    
    try {
      await _syncRepository.synchronize(userId);
      _status = SyncStatus.success;
    } catch (e) {
      print('Sync error: $e');
      _status = SyncStatus.error;
    } finally {
      // Reset to idle after short delay to show result
      Future.delayed(const Duration(seconds: 2), () {
        _status = SyncStatus.idle;
        notifyListeners();
      });
      notifyListeners();
    }
  }
}
