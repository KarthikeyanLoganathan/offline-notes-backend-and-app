package com.notesapp.offline.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.notesapp.offline.data.local.dao.*
import com.notesapp.offline.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        UserSessionEntity::class,
        NoteEntity::class,
        LabelEntity::class,
        NoteLabelCrossRef::class,
        SyncMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    
    companion object {
        @Volatile
        private var INSTANCE: NotesDatabase? = null
        
        fun getDatabase(context: Context): NotesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE
                if (instance != null) {
                    instance
                } else {
                    try {
                        android.util.Log.d("NotesDebug", "Attempting to create database 'notes_database'")
                        val appContext = context.applicationContext
                        android.util.Log.d("NotesDebug", "Context package: ${appContext.packageName}")
                        
                        val newInstance = Room.databaseBuilder(
                            appContext,
                            NotesDatabase::class.java,
                            "notes_database"
                        )
                        .fallbackToDestructiveMigration()
                        .build()
                        
                        android.util.Log.d("NotesDebug", "Database builder created successfully")
                        INSTANCE = newInstance
                        newInstance
                    } catch (e: Throwable) {
                        android.util.Log.e("NotesDebug", "CRITICAL: Failed to create database", e)
                        throw RuntimeException("Database creation failed", e)
                    }
                }
            }
        }
    }
}
