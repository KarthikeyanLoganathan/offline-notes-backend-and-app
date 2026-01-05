package com.notesapp.offline.data.remote

import com.notesapp.offline.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth endpoints
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<User>>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<String>>
    
    @GET("auth/verify-email")
    suspend fun verifyEmail(@Query("code") code: String): Response<ApiResponse<User>>
    
    // Notes endpoints
    @GET("notes")
    suspend fun getNotes(
        @Query("includeDeleted") includeDeleted: Boolean = false,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<List<Note>>
    
    @GET("notes/label/{labelId}")
    suspend fun getNotesByLabel(
        @Path("labelId") labelId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<List<Note>>
    
    @GET("notes/search")
    suspend fun searchNotes(
        @Query("q") query: String,
        @Query("includeDeleted") includeDeleted: Boolean = false,
        @Query("limit") limit: Int = 100
    ): Response<List<Note>>
    
    @GET("notes/{id}")
    suspend fun getNote(@Path("id") noteId: String): Response<Note>
    
    @POST("notes")
    suspend fun createNote(@Body request: CreateNoteRequest): Response<Note>
    
    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") noteId: String,
        @Body request: UpdateNoteRequest
    ): Response<Note>
    
    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") noteId: String): Response<ApiResponse<String>>
    
    @POST("notes/{id}/restore")
    suspend fun restoreNote(@Path("id") noteId: String): Response<Note>
    
    // Sync endpoints
    @GET("notes/sync/global")
    suspend fun getGlobalSyncMetadata(): Response<SyncMetadata>
    
    @GET("notes/sync/label/{labelId}")
    suspend fun getLabelSyncMetadata(@Path("labelId") labelId: String): Response<SyncMetadata>
    
    @GET("notes/sync/changes")
    suspend fun getNotesChangedSince(
        @Query("since") timestamp: String,
        @Query("labelId") labelId: String? = null
    ): Response<List<Note>>
    
    // Labels endpoints
    @GET("labels")
    suspend fun getLabels(): Response<List<Label>>
    
    @GET("labels/{id}")
    suspend fun getLabel(@Path("id") labelId: String): Response<Label>
    
    @POST("labels")
    suspend fun createLabel(@Body request: CreateLabelRequest): Response<Label>
    
    @PUT("labels/{id}")
    suspend fun updateLabel(
        @Path("id") labelId: String,
        @Body request: CreateLabelRequest
    ): Response<Label>
    
    @DELETE("labels/{id}")
    suspend fun deleteLabel(@Path("id") labelId: String): Response<ApiResponse<String>>
}
