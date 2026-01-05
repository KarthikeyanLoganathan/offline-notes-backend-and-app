package com.notesapp.offline.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.notesapp.offline.data.repository.AuthRepository
import com.notesapp.offline.databinding.ActivityRegisterBinding
import com.notesapp.offline.util.Resource
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authRepository = AuthRepository(this)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            register()
        }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun register() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val addressLine1 = binding.etAddressLine1.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val state = binding.etState.text.toString().trim()
        val country = binding.etCountry.text.toString().trim()
        val postalCode = binding.etPostalCode.text.toString().trim()
        
        // Validation
        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
        
        lifecycleScope.launch {
            when (val result = authRepository.register(
                email, password, firstName, lastName,
                addressLine1.ifEmpty { null },
                null, city.ifEmpty { null }, state.ifEmpty { null },
                country.ifEmpty { null }, postalCode.ifEmpty { null }
            )) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration successful! Please check your email to verify your account.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(this@RegisterActivity, result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}
