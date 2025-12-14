package com.notesapp.offline.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.notesapp.offline.data.local.entity.LabelEntity

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels WHERE userId = :userId ORDER BY name ASC")
    fun getAllLabels(userId: String): LiveData<List<LabelEntity>>
    
    @Query("SELECT * FROM labels WHERE id = :labelId")
    suspend fun getLabelById(labelId: String): LabelEntity?
    
    @Query("SELECT * FROM labels WHERE syncStatus != 0")
    suspend fun getUnsyncedLabels(): List<LabelEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(label: LabelEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(labels: List<LabelEntity>)
    
    @Update
    suspend fun update(label: LabelEntity)
    
    @Delete
    suspend fun delete(label: LabelEntity)
    
    @Query("DELETE FROM labels WHERE id = :labelId")
    suspend fun deleteById(labelId: String)
    
    @Query("DELETE FROM labels WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
    
    @Query("UPDATE labels SET syncStatus = :status WHERE id = :labelId")
    suspend fun updateSyncStatus(labelId: String, status: Int)
}
