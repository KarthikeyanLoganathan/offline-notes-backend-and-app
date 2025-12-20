import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../providers/notes_provider.dart';
import '../../providers/sync_provider.dart';
import '../../widgets/note_card.dart';
import '../../widgets/sync_indicator.dart';
import 'note_detail_screen.dart';
import '../auth/login_screen.dart';

class NotesListScreen extends StatefulWidget {
  const NotesListScreen({super.key});

  @override
  State<NotesListScreen> createState() => _NotesListScreenState();
}

class _NotesListScreenState extends State<NotesListScreen> {
  final _searchController = TextEditingController();
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final user = context.read<AuthProvider>().user;
      if (user != null) {
        context.read<NotesProvider>().setUserId(user.id);
        context.read<SyncProvider>().syncNow(user.id);
      }
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _onLogout() {
    context.read<AuthProvider>().logout();
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => const LoginScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Basic filter logic handled in UI for simplicity here, ideally in Provider
    final notes = context.watch<NotesProvider>().notes.where((note) {
      if (_searchQuery.isEmpty) return true;
      return note.title.toLowerCase().contains(_searchQuery.toLowerCase()) ||
          note.content.toLowerCase().contains(_searchQuery.toLowerCase());
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('My Notes'),
        actions: [
          const Center(child: Padding(
            padding: EdgeInsets.only(right: 16.0),
            child: SyncIndicator(),
          )),
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: _onLogout,
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              controller: _searchController,
              decoration: const InputDecoration(
                hintText: 'Search notes...',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(30))),
                contentPadding: EdgeInsets.symmetric(horizontal: 20, vertical: 0),
              ),
              onChanged: (val) {
                setState(() {
                  _searchQuery = val;
                });
              },
            ),
          ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () async {
                final user = context.read<AuthProvider>().user;
                if (user != null) {
                  await context.read<SyncProvider>().syncNow(user.id);
                  await context.read<NotesProvider>().loadNotes(); // Reload local
                }
              },
              child: notes.isEmpty
                  ? Center(
                      child: Text(
                        _searchQuery.isEmpty ? 'No notes yet' : 'No matches found',
                        style: TextStyle(color: Colors.grey[600]),
                      ),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      itemCount: notes.length,
                      itemBuilder: (context, index) {
                        return NoteCard(
                          note: notes[index],
                          onTap: () {
                             Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => NoteDetailScreen(note: notes[index]),
                              ),
                            );
                          },
                        );
                      },
                    ),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => const NoteDetailScreen(),
            ),
          );
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
