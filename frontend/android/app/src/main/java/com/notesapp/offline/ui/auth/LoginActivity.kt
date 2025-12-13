package com.notesapp.offline.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.notesapp.offline.data.repository.AuthRepository
import com.notesapp.offline.databinding.ActivityLoginBinding
import com.notesapp.offline.ui.MainActivity
import com.notesapp.offline.util.Resource
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authRepository = AuthRepository(this)
        
        // Check if already logged in
        checkSession()
        
        setupUI()
    }
    
    private fun checkSession() {
        lifecycleScope.launch {
            if (authRepository.isLoggedIn()) {
                navigateToMain()
            }
        }
    }
    
    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            login()
        }
        
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        
        val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        
        lifecycleScope.launch {
            when (val result = authRepository.login(email, password, deviceInfo)) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
