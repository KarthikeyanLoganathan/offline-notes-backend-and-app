package com.notesapp.offline.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.notesapp.offline.data.local.NotesDatabase
import com.notesapp.offline.data.local.entity.*
import com.notesapp.offline.data.remote.ApiService
import com.notesapp.offline.data.remote.RetrofitClient
import com.notesapp.offline.data.remote.model.*
import com.notesapp.offline.util.NetworkUtils
import com.notesapp.offline.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class NotesRepository(context: Context) {
    private val database = NotesDatabase.getDatabase(context)
    private val noteDao = database.noteDao()
    private val labelDao = database.labelDao()
    private val syncMetadataDao = database.syncMetadataDao()
    private val sessionDao = database.userSessionDao()
    private val apiService: ApiService = RetrofitClient.apiService
    private val networkUtils = NetworkUtils(context)
    
    // Get all notes
    fun getAllNotes(userId: String): LiveData<List<NoteWithLabels>> {
        return noteDao.getAllNotes(userId)
    }
    
    // Get deleted notes
    fun getDeletedNotes(userId: String): LiveData<List<NoteWithLabels>> {
        return noteDao.getDeletedNotes(userId)
    }
    
    // Get notes by label
    fun getNotesByLabel(userId: String, labelId: String): LiveData<List<NoteWithLabels>> {
        return noteDao.getNotesByLabel(userId, labelId)
    }
    
    // Search notes
    fun searchNotes(userId: String, query: String): LiveData<List<NoteWithLabels>> {
        val searchQuery = "%$query%"
        return noteDao.searchNotes(userId, searchQuery)
    }
    
    // Create note
    suspend fun createNote(
        userId: String,
        title: String?,
        content: String?,
        labelIds: List<String>?
    ): Resource<NoteWithLabels> = withContext(Dispatchers.IO) {
        try {
            if (networkUtils.isOnline()) {
                // Create note on server
                val request = CreateNoteRequest(title, content, labelIds)
                val response = apiService.createNote(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val note = response.body()!!
                    // Save to local database
                    val noteEntity = note.toEntity()
                    noteDao.insert(noteEntity)
                    
                    // Save labels
                    note.labels?.forEach { label ->
                        noteDao.insertNoteLabel(NoteLabelCrossRef(note.id, label.id))
                    }
                    
                    val savedNote = noteDao.getNoteById(note.id)
                    Resource.Success(savedNote!!)
                } else {
                    Resource.Error("Failed to create note: ${response.message()}")
                }
            } else {
                // Create note locally with UUID and pending status
                val noteEntity = NoteEntity(
                    id = UUID.randomUUID().toString(), // Generate UUID locally
                    userId = userId,
                    title = title,
                    content = content,
                    isDeleted = false,
                    deletedAt = null,
                    createdAt = System.currentTimeMillis().toString(),
                    updatedAt = System.currentTimeMillis().toString(),
                    syncStatus = 1 // pending_create
                )
                
                noteDao.insert(noteEntity)
                
                // Save labels locally
                labelIds?.forEach { labelId ->
                    noteDao.insertNoteLabel(NoteLabelCrossRef(noteEntity.id, labelId))
                }
                
                val savedNote = noteDao.getNoteById(noteEntity.id)
                Resource.Success(savedNote!!)
            }
        } catch (e: Exception) {
            Resource.Error("Error creating note: ${e.localizedMessage}")
        }
    }
    
    // Update note
    suspend fun updateNote(
        noteId: String,
        title: String?,
        content: String?,
        labelIds: List<String>?
    ): Resource<NoteWithLabels> = withContext(Dispatchers.IO) {
        try {
            val existingNote = noteDao.getNoteById(noteId)
            
            if (networkUtils.isOnline() && existingNote?.note?.syncStatus == 0) {
                // Update on server
                val request = UpdateNoteRequest(title, content, labelIds)
                val response = apiService.updateNote(noteId, request)
                
                if (response.isSuccessful && response.body() != null) {
                    val note = response.body()!!
                    // Update local database
                    val noteEntity = note.toEntity()
                    noteDao.update(noteEntity)
                    
                    // Update labels
                    noteDao.deleteAllLabelsForNote(note.id)
                    note.labels?.forEach { label ->
                        noteDao.insertNoteLabel(NoteLabelCrossRef(note.id, label.id))
                    }
                    
                    val updatedNote = noteDao.getNoteById(note.id)
                    Resource.Success(updatedNote!!)
                } else {
                    Resource.Error("Failed to update note: ${response.message()}")
                }
            } else {
                // Update locally with pending status
                existingNote?.let {
                    val updatedEntity = it.note.copy(
                        title = title ?: it.note.title,
                        content = content ?: it.note.content,
                        updatedAt = System.currentTimeMillis().toString(),
                        syncStatus = if (it.note.syncStatus == 1) 1 else 2 // Keep pending_create or set to pending_update
                    )
                    noteDao.update(updatedEntity)
                    
                    // Update labels
                    if (labelIds != null) {
                        noteDao.deleteAllLabelsForNote(noteId)
                        labelIds.forEach { labelId ->
                            noteDao.insertNoteLabel(NoteLabelCrossRef(noteId, labelId))
                        }
                    }
                    
                    val updatedNote = noteDao.getNoteById(noteId)
                    Resource.Success(updatedNote!!)
                } ?: Resource.Error("Note not found")
            }
        } catch (e: Exception) {
            Resource.Error("Error updating note: ${e.localizedMessage}")
        }
    }
    
    // Delete note
    suspend fun deleteNote(noteId: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val note = noteDao.getNoteById(noteId)
            
            if (networkUtils.isOnline() && note?.note?.syncStatus == 0) {
                // Delete on server
                val response = apiService.deleteNote(noteId)
                
                if (response.isSuccessful) {
                    // Update local as deleted
                    note.let {
                        val deletedEntity = it.note.copy(
                            isDeleted = true,
                            deletedAt = System.currentTimeMillis().toString(),
                            updatedAt = System.currentTimeMillis().toString()
                        )
                        noteDao.update(deletedEntity)
                    }
                    Resource.Success("Note deleted successfully")
                } else {
                    Resource.Error("Failed to delete note: ${response.message()}")
                }
            } else {
                // Mark as deleted locally
                note?.let {
                    val deletedEntity = it.note.copy(
                        isDeleted = true,
                        deletedAt = System.currentTimeMillis().toString(),
                        updatedAt = System.currentTimeMillis().toString(),
                        syncStatus = if (it.note.syncStatus == 1) 1 else 3 // Keep pending_create or set to pending_delete
                    )
                    noteDao.update(deletedEntity)
                    Resource.Success("Note deleted successfully")
                } ?: Resource.Error("Note not found")
            }
        } catch (e: Exception) {
            Resource.Error("Error deleting note: ${e.localizedMessage}")
        }
    }
    
    // Sync notes
    suspend fun syncNotes(userId: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isOnline()) {
                return@withContext Resource.Error("No internet connection")
            }
            
            // Get last sync timestamp
            val globalMetadata = syncMetadataDao.getGlobalMetadata(userId)
            val lastSync = globalMetadata?.lastSyncTimestamp
            
            // Fetch changed notes from server
            if (lastSync != null) {
                val response = apiService.getNotesChangedSince(lastSync)
                if (response.isSuccessful && response.body() != null) {
                    val serverNotes = response.body()!!
                    serverNotes.forEach { note ->
                        noteDao.insert(note.toEntity())
                        noteDao.deleteAllLabelsForNote(note.id)
                        note.labels?.forEach { label ->
                            noteDao.insertNoteLabel(NoteLabelCrossRef(note.id, label.id))
                        }
                    }
                }
            } else {
                // First sync - fetch all notes
                val response = apiService.getNotes()
                if (response.isSuccessful && response.body() != null) {
                    val serverNotes = response.body()!!
                    serverNotes.forEach { note ->
                        noteDao.insert(note.toEntity())
                        note.labels?.forEach { label ->
                            noteDao.insertNoteLabel(NoteLabelCrossRef(note.id, label.id))
                        }
                    }
                }
            }
            
            // Push local changes
            val unsyncedNotes = noteDao.getUnsyncedNotes()
            unsyncedNotes.forEach { localNote ->
                when (localNote.syncStatus) {
                    1 -> { // pending_create
                        // Send the client-generated UUID to server
                        val request = CreateNoteRequest(
                            id = localNote.id, // Pass the UUID
                            title = localNote.title,
                            content = localNote.content,
                            labelIds = null
                        )
                        val response = apiService.createNote(request)
                        if (response.isSuccessful && response.body() != null) {
                            // Note keeps same ID, just update sync status
                            noteDao.updateSyncStatus(localNote.id, 0)
                        }
                    }
                    2 -> { // pending_update
                        val request = UpdateNoteRequest(localNote.title, localNote.content, null)
                        val response = apiService.updateNote(localNote.id, request)
                        if (response.isSuccessful) {
                            noteDao.updateSyncStatus(localNote.id, 0)
                        }
                    }
                    3 -> { // pending_delete
                        val response = apiService.deleteNote(localNote.id)
                        if (response.isSuccessful) {
                            noteDao.updateSyncStatus(localNote.id, 0)
                        }
                    }
                }
            }
            
            // Update sync metadata
            val currentTime = System.currentTimeMillis().toString()
            if (globalMetadata != null) {
                syncMetadataDao.update(globalMetadata.copy(lastSyncTimestamp = currentTime))
            } else {
                syncMetadataDao.insert(
                    SyncMetadataEntity(
                        userId = userId,
                        metadataType = "global",
                        lastChangeTimestamp = currentTime,
                        lastSyncTimestamp = currentTime
                    )
                )
            }
            
            Resource.Success("Sync completed successfully")
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.localizedMessage}")
        }
    }
}

// Extension functions to convert API models to entities
fun Note.toEntity() = NoteEntity(
    id = this.id,
    userId = this.userId,
    title = this.title,
    content = this.content,
    isDeleted = this.isDeleted,
    deletedAt = this.deletedAt,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    syncStatus = 0
)

fun Label.toEntity(userId: String) = LabelEntity(
    id = this.id,
    userId = userId,
    name = this.name,
    color = this.color,
    createdAt = this.createdAt ?: System.currentTimeMillis().toString(),
    syncStatus = 0
)
