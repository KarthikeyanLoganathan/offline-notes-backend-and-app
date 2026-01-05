package com.notesapp.offline.data.remote.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceInfo: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postalCode: String? = null
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class User(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val emailVerified: Boolean
)

data class Note(
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    val title: String?,
    val content: String?,
    @SerializedName("is_deleted")
    val isDeleted: Boolean,
    @SerializedName("deleted_at")
    val deletedAt: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val labels: List<Label>?
)

data class CreateNoteRequest(
    val id: String? = null, // Client-generated UUID for offline sync
    val title: String?,
    val content: String?,
    val labelIds: List<String>?
)

data class UpdateNoteRequest(
    val title: String?,
    val content: String?,
    val labelIds: List<String>?
)

data class Label(
    val id: String,
    @SerializedName("user_id")
    val userId: String? = null,
    val name: String,
    val color: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("note_count")
    val noteCount: Int? = null
)

data class CreateLabelRequest(
    val id: String? = null, // Client-generated UUID for offline sync
    val name: String,
    val color: String = "#808080"
)

data class SyncMetadata(
    val lastChangeTimestamp: String
)

data class ApiResponse<T>(
    val data: T? = null,
    val error: String? = null,
    val message: String? = null
)
