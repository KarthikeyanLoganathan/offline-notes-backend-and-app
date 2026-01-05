package com.notesapp.offline.data.local.dao

import androidx.room.*
import com.notesapp.offline.data.local.entity.SyncMetadataEntity

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE userId = :userId AND metadataType = 'global' AND labelId IS NULL")
    suspend fun getGlobalMetadata(userId: String): SyncMetadataEntity?
    
    @Query("SELECT * FROM sync_metadata WHERE userId = :userId AND metadataType = 'label' AND labelId = :labelId")
    suspend fun getLabelMetadata(userId: String, labelId: String): SyncMetadataEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SyncMetadataEntity)
    
    @Update
    suspend fun update(metadata: SyncMetadataEntity)
    
    @Query("DELETE FROM sync_metadata WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
