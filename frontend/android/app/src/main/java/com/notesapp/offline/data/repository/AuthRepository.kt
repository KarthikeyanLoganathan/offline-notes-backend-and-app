package com.notesapp.offline.data.repository

import android.content.Context
import com.notesapp.offline.data.local.NotesDatabase
import com.notesapp.offline.data.local.entity.UserSessionEntity
import com.notesapp.offline.data.remote.ApiService
import com.notesapp.offline.data.remote.RetrofitClient
import com.notesapp.offline.data.remote.model.LoginRequest
import com.notesapp.offline.data.remote.model.LoginResponse
import com.notesapp.offline.data.remote.model.RegisterRequest
import com.notesapp.offline.data.remote.model.User
import com.notesapp.offline.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    private val database by lazy { NotesDatabase.getDatabase(context) }
    private val sessionDao by lazy { database.userSessionDao() }
    private val apiService: ApiService = RetrofitClient.apiService
    
    // Register user
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        addressLine1: String? = null,
        addressLine2: String? = null,
        city: String? = null,
        state: String? = null,
        country: String? = null,
        postalCode: String? = null
    ): Resource<User> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(
                email, password, firstName, lastName,
                addressLine1, addressLine2, city, state, country, postalCode
            )
            val response = apiService.register(request)
            
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error("Error: ${e.localizedMessage}")
        }
    }
    
    // Login user
    suspend fun login(
        email: String,
        password: String,
        deviceInfo: String
    ): Resource<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(email, password, deviceInfo)
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                
                // Save session locally
                val session = UserSessionEntity(
                    userId = loginResponse.user.id,
                    email = loginResponse.user.email,
                    sessionToken = loginResponse.token,
                    firstName = loginResponse.user.firstName,
                    lastName = loginResponse.user.lastName,
                    deviceInfo = deviceInfo,
                    expiresAt = (System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)).toString()
                )
                sessionDao.insert(session)
                
                // Set auth token for API calls
                RetrofitClient.setAuthToken(loginResponse.token)
                
                Resource.Success(loginResponse)
            } else {
                Resource.Error("Login failed: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error: ${e.localizedMessage}")
        }
    }
    
    // Logout user
    suspend fun logout(): Resource<String> = withContext(Dispatchers.IO) {
        try {
            apiService.logout()
            sessionDao.clearSession()
            RetrofitClient.setAuthToken(null)
            Resource.Success("Logged out successfully")
        } catch (e: Exception) {
            // Clear local session even if API call fails
            sessionDao.clearSession()
            RetrofitClient.setAuthToken(null)
            Resource.Success("Logged out successfully")
        }
    }
    
    // Get current session
    suspend fun getCurrentSession(): UserSessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.getSession()
    }
    
    // Check if user is logged in
    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession()
        if (session != null) {
            RetrofitClient.setAuthToken(session.sessionToken)
            true
        } else {
            false
        }
    }
}
