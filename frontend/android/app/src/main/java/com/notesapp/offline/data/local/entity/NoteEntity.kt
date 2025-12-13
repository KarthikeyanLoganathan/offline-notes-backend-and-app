package com.notesapp.offline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "notes",
    indices = [Index(value = ["userId"]), Index(value = ["updatedAt"])]
)
data class NoteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String?,
    val content: String?,
    val isDeleted: Boolean = false,
    val deletedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    // Sync status: 0 = synced, 1 = pending_create, 2 = pending_update, 3 = pending_delete
    val syncStatus: Int = 0
)
