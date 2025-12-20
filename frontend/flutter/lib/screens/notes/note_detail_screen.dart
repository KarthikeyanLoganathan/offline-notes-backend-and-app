import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/note.dart';
import '../../providers/notes_provider.dart';

class NoteDetailScreen extends StatefulWidget {
  final Note? note;

  const NoteDetailScreen({super.key, this.note});

  @override
  State<NoteDetailScreen> createState() => _NoteDetailScreenState();
}

class _NoteDetailScreenState extends State<NoteDetailScreen> {
  late final TextEditingController _titleController;
  late final TextEditingController _contentController;
  bool _isDirty = false;

  @override
  void initState() {
    super.initState();
    _titleController = TextEditingController(text: widget.note?.title ?? '');
    _contentController = TextEditingController(text: widget.note?.content ?? '');
  }

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  Future<void> _saveNote() async {
    final title = _titleController.text.trim();
    final content = _contentController.text.trim();

    if (title.isEmpty && content.isEmpty) return;

    final provider = context.read<NotesProvider>();

    if (widget.note == null) {
      await provider.addNote(title, content);
    } else {
      if (_isDirty) {
        await provider.updateNote(widget.note!.id, title, content);
      }
    }
  }

  void _onDelete() async {
    if (widget.note == null) return;
    
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete Note?'),
        content: const Text('This action cannot be undone.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(
             onPressed: () => Navigator.pop(ctx, true),
             style: TextButton.styleFrom(foregroundColor: Colors.red),
             child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirm == true && mounted) {
      await context.read<NotesProvider>().deleteNote(widget.note!.id);
      if (mounted) Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        actions: [
          if (widget.note != null)
            IconButton(
              icon: const Icon(Icons.delete_outline),
              onPressed: _onDelete,
            ),
        ],
      ),
      body: PopScope(
        onPopInvoked: (didPop) async {
           // Auto-save on back if not deleted
           if (didPop && widget.note?.isSynced == true || widget.note == null) {
             // For simplicity, we just save if dirty
             if (_isDirty) {
               await _saveNote();
             }
           }
        },
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
             children: [
               TextField(
                 controller: _titleController,
                 style: Theme.of(context).textTheme.headlineMedium,
                 decoration: const InputDecoration(
                   hintText: 'Title',
                   border: InputBorder.none,
                 ),
                 onChanged: (_) => _isDirty = true,
               ),
               Expanded(
                 child: TextField(
                   controller: _contentController,
                   style: Theme.of(context).textTheme.bodyLarge,
                   decoration: const InputDecoration(
                     hintText: 'Type something...',
                     border: InputBorder.none,
                   ),
                   maxLines: null,
                   expands: true,
                   onChanged: (_) => _isDirty = true,
                 ),
               ),
             ],
          ),
        ),
      ),
    );
  }
}
