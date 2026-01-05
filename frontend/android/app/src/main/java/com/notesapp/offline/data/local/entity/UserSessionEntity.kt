package com.notesapp.offline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey
    val userId: String,
    val email: String,
    val sessionToken: String,
    val firstName: String,
    val lastName: String,
    val deviceInfo: String,
    val expiresAt: String,
    val createdAt: String = System.currentTimeMillis().toString()
)
