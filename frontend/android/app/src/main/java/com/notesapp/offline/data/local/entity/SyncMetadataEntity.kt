package com.notesapp.offline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "sync_metadata",
    indices = [Index(value = ["userId", "metadataType", "labelId"], unique = true)]
)
data class SyncMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val metadataType: String, // "global" or "label"
    val labelId: String? = null,
    val lastChangeTimestamp: String,
    val lastSyncTimestamp: String? = null
)
