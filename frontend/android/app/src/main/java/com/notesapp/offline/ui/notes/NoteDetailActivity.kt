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
    private var noteId: String? = null
    private var currentUserId: String = ""
    private val selectedLabelIds = mutableSetOf<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        notesRepository = NotesRepository(this)
        authRepository = AuthRepository(this)
        
        noteId = intent.getStringExtra("NOTE_ID")
        
        loadSession()
        setupLabels()
        loadNote()
    }
    
    private fun loadSession() {
        lifecycleScope.launch {
            val session = authRepository.getCurrentSession()
            currentUserId = session?.userId ?: ""
        }
    }
    
    private fun loadNote() {
        noteId?.let { id ->
            lifecycleScope.launch {
                val noteWithLabels = notesRepository.getNoteById(id)
                noteWithLabels?.let {
                    binding.etTitle.setText(it.note.title)
                    binding.etContent.setText(it.note.content)
                    supportActionBar?.title = "Edit Note"
                    
                    // Pre-select labels
                    it.labels.forEach { label ->
                        selectedLabelIds.add(label.id)
                        // Verify chips are updated in setupLabels observer
                        updateChipSelection() 
                    }
                }
            }
        } ?: run {
            supportActionBar?.title = "New Note"
        }
    }
    
    private fun setupLabels() {
        notesRepository.getAllLabels(currentUserId).observe(this) { labels ->
            binding.chipGroupLabels.removeAllViews()
            
            labels.forEach { label ->
                val chip = com.google.android.material.chip.Chip(this)
                chip.text = label.name
                chip.isCheckable = true
                chip.tag = label.id
                
                // Set checked state if in selected ids (handles sync issue if labels load after note)
                chip.isChecked = selectedLabelIds.contains(label.id)
                
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    val id = buttonView.tag as String
                    if (isChecked) {
                        selectedLabelIds.add(id)
                    } else {
                        selectedLabelIds.remove(id)
                    }
                }
                binding.chipGroupLabels.addView(chip)
            }
        }
    }
    
    private fun updateChipSelection() {
        // Iterate through chips to update state if they exist
        for (i in 0 until binding.chipGroupLabels.childCount) {
            val chip = binding.chipGroupLabels.getChildAt(i) as? com.google.android.material.chip.Chip
            val id = chip?.tag as? String
            if (id != null && selectedLabelIds.contains(id)) {
                chip.isChecked = true
            }
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
                notesRepository.createNote(currentUserId, title, content, selectedLabelIds.toList())
            } else {
                notesRepository.updateNote(noteId!!, title, content, selectedLabelIds.toList())
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
