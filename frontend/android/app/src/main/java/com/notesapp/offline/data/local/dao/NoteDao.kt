package com.notesapp.offline.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.notesapp.offline.data.local.entity.NoteEntity
import com.notesapp.offline.data.local.entity.NoteLabelCrossRef
import com.notesapp.offline.data.local.entity.NoteWithLabels

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllNotes(userId: String): LiveData<List<NoteWithLabels>>
    
    @Transaction
    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedNotes(userId: String): LiveData<List<NoteWithLabels>>
    
    @Transaction
    @Query("""
        SELECT DISTINCT n.* FROM notes n
        INNER JOIN note_labels nl ON n.id = nl.noteId
        WHERE n.userId = :userId AND nl.labelId = :labelId AND n.isDeleted = 0
        ORDER BY n.updatedAt DESC
    """)
    fun getNotesByLabel(userId: String, labelId: String): LiveData<List<NoteWithLabels>>
    
    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteWithLabels?
    
    @Transaction
    @Query("""
        SELECT * FROM notes 
        WHERE userId = :userId 
        AND isDeleted = :includeDeleted
        AND (title LIKE :searchQuery OR content LIKE :searchQuery)
        ORDER BY updatedAt DESC
    """)
    fun searchNotes(userId: String, searchQuery: String, includeDeleted: Boolean = false): LiveData<List<NoteWithLabels>>
    
    @Query("SELECT * FROM notes WHERE syncStatus != 0")
    suspend fun getUnsyncedNotes(): List<NoteEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)
    
    @Update
    suspend fun update(note: NoteEntity)
    
    @Delete
    suspend fun delete(note: NoteEntity)
    
    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteLabel(crossRef: NoteLabelCrossRef)
    
    @Delete
    suspend fun deleteNoteLabel(crossRef: NoteLabelCrossRef)
    
    @Query("DELETE FROM note_labels WHERE noteId = :noteId")
    suspend fun deleteAllLabelsForNote(noteId: String)
    
    @Query("UPDATE notes SET syncStatus = :status WHERE id = :noteId")
    suspend fun updateSyncStatus(noteId: String, status: Int)
}
