package com.notesapp.offline

import android.app.Application
import android.content.Context
import androidx.work.*
import com.notesapp.offline.util.Resource

class NotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        android.util.Log.e("NotesDebug", "NotesApplication: APP STARTING UP")
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("NotesDebug", "CRITICAL GLOBAL CRASH: Uncaught exception in thread ${thread.name}", throwable)
            // Re-throw or let the default handler handle it if needed, but usually we just want to log it first
            // e.printStackTrace() 
            // System.exit(1) // Optional: Kill process
        }
        
        // WorkManager will be initialized lazily when first accessed
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
