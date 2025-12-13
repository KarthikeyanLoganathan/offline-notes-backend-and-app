package com.notesapp.offline.data.local.dao

import androidx.room.*
import com.notesapp.offline.data.local.entity.UserSessionEntity

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_session LIMIT 1")
    suspend fun getSession(): UserSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: UserSessionEntity)
    
    @Query("DELETE FROM user_session")
    suspend fun clearSession()
    
    @Query("SELECT sessionToken FROM user_session LIMIT 1")
    suspend fun getSessionToken(): String?
}
