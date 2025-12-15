package com.notesapp.offline.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.notesapp.offline.BuildConfig

class NetworkUtils(private val context: Context) {
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        // In debug mode (and thus on emulator), we relax the check because 
        // NET_CAPABILITY_VALIDATED can be flaky on emulators.
        return if (BuildConfig.DEBUG) {
             hasInternet
        } else {
             hasInternet && isValidated
        }
    }
}
