package com.notesapp.offline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "labels",
    indices = [Index(value = ["userId", "name"], unique = true)]
)
data class LabelEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val color: String = "#808080",
    val createdAt: String,
    val syncStatus: Int = 0 // 0 = synced, 1 = pending_create, 2 = pending_update, 3 = pending_delete
)
