import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/sync_provider.dart';

class SyncIndicator extends StatelessWidget {
  const SyncIndicator({super.key});

  @override
  Widget build(BuildContext context) {
    final status = context.watch<SyncProvider>().status;

    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 300),
      child: switch (status) {
        SyncStatus.syncing => const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                width: 12,
                height: 12,
                child: CircularProgressIndicator(strokeWidth: 2),
              ),
              SizedBox(width: 8),
              Text('Syncing...', style: TextStyle(fontSize: 12)),
            ],
          ),
        SyncStatus.success => const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.check, size: 16, color: Colors.green),
              SizedBox(width: 4),
              Text('Synced', style: TextStyle(fontSize: 12, color: Colors.green)),
            ],
          ),
        SyncStatus.error => const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.error_outline, size: 16, color: Colors.red),
              SizedBox(width: 4),
              Text('Sync Error', style: TextStyle(fontSize: 12, color: Colors.red)),
            ],
          ),
        SyncStatus.idle => const SizedBox.shrink(),
      },
    );
  }
}
