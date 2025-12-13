package com.notesapp.offline

import android.app.Application
import android.content.Context
import androidx.work.*
import com.notesapp.offline.data.local.NotesDatabase
import com.notesapp.offline.util.Resource;
import java.util.concurrent.TimeUnit

class NotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize database
        NotesDatabase.getDatabase(this)
        
        // Schedule periodic sync
        setupPeriodicSync()
    }
    
    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15, TimeUnit.MINUTES
            )
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NotesSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}

// Sync Worker
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val authRepository = com.notesapp.offline.data.repository.AuthRepository(applicationContext)
            val session = authRepository.getCurrentSession()
            
            if (session != null) {
                val notesRepository = com.notesapp.offline.data.repository.NotesRepository(applicationContext)
                val result = notesRepository.syncNotes(session.userId)
                
                when (result) {
                    is Resource.Success -> Result.success()
                    is Resource.Error -> Result.retry()
                    else -> Result.retry()
                }
            } else {
                Result.success() // No session, skip sync
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
