package com.notesapp.offline.ui.notes

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.notesapp.offline.R
import com.notesapp.offline.data.repository.AuthRepository
import com.notesapp.offline.data.repository.NotesRepository
import com.notesapp.offline.databinding.ActivityNoteDetailBinding
import com.notesapp.offline.util.Resource
import kotlinx.coroutines.launch

class NoteDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var notesRepository: NotesRepository
    private lateinit var authRepository: AuthRepository
    private var noteId: Int? = null
    private var currentUserId: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        notesRepository = NotesRepository(this)
        authRepository = AuthRepository(this)
        
        noteId = intent.getIntExtra("NOTE_ID", -1).takeIf { it != -1 }
        
        loadSession()
        loadNote()
    }
    
    private fun loadSession() {
        lifecycleScope.launch {
            val session = authRepository.getCurrentSession()
            currentUserId = session?.userId ?: 0
        }
    }
    
    private fun loadNote() {
        noteId?.let { id ->
            lifecycleScope.launch {
                // Load existing note
                supportActionBar?.title = "Edit Note"
            }
        } ?: run {
            supportActionBar?.title = "New Note"
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_detail, menu)
        
        if (noteId == null) {
            menu.findItem(R.id.action_delete)?.isVisible = false
        }
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveNote()
                true
            }
            R.id.action_delete -> {
                deleteNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun saveNote() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        
        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val result = if (noteId == null) {
                notesRepository.createNote(currentUserId, title, content, null)
            } else {
                notesRepository.updateNote(noteId!!, title, content, null)
            }
            
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(this@NoteDetailActivity, "Note saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this@NoteDetailActivity, result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
    
    private fun deleteNote() {
        noteId?.let { id ->
            lifecycleScope.launch {
                when (notesRepository.deleteNote(id)) {
                    is Resource.Success -> {
                        Toast.makeText(this@NoteDetailActivity, "Note deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is Resource.Error -> {
                        Toast.makeText(this@NoteDetailActivity, "Failed to delete note", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
}
