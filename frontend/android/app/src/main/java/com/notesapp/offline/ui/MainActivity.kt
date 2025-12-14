package com.notesapp.offline.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.notesapp.offline.R
import com.notesapp.offline.data.repository.AuthRepository
import com.notesapp.offline.data.repository.NotesRepository
import com.notesapp.offline.databinding.ActivityMainBinding
import com.notesapp.offline.ui.adapter.NotesAdapter
import com.notesapp.offline.ui.auth.LoginActivity
import com.notesapp.offline.ui.notes.NoteDetailActivity
import com.notesapp.offline.util.Resource
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var notesRepository: NotesRepository
    private lateinit var notesAdapter: NotesAdapter
    private var currentUserId: String = ""
    private var currentLabelId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authRepository = AuthRepository(this)
        notesRepository = NotesRepository(this)
        
        checkSession()
    }
    
    private fun checkSession() {
        lifecycleScope.launch {
            val session = authRepository.getCurrentSession()
            if (session != null) {
                currentUserId = session.userId
                setupUI()
                loadNotes()
                setupPeriodicSync() // Initialize WorkManager after session is confirmed
            } else {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        
        setupLabelFilter()
        
        binding.fabAddNote.setOnClickListener {
            startActivity(Intent(this, NoteDetailActivity::class.java))
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            syncNotes()
        }
        
        notesAdapter = NotesAdapter { noteWithLabels ->
            val intent = Intent(this, NoteDetailActivity::class.java).apply {
                putExtra("NOTE_ID", noteWithLabels.note.id)
            }
            startActivity(intent)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = notesAdapter
        }
    }
    
    private fun loadNotes() {
        val notesLiveData = if (currentLabelId == null) {
            notesRepository.getAllNotes(currentUserId)
        } else {
            notesRepository.getNotesByLabel(currentUserId, currentLabelId!!)
        }
        
        notesLiveData.observe(this) { notes ->
            notesAdapter.submitList(notes)
            binding.tvEmptyState.visibility = if (notes.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }
    
    private fun setupLabelFilter() {
        notesRepository.getAllLabels(currentUserId).observe(this) { labels ->
            binding.chipGroupFilter.removeAllViews()
            
            // Add "All" chip (effectively clears filter)
            // Actually, clearing selection in ChipGroup works too since we set selectionRequired=false
            
            labels.forEach { label ->
                val chip = com.google.android.material.chip.Chip(this)
                chip.text = label.name
                chip.isCheckable = true
                chip.tag = label.id
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        currentLabelId = buttonView.tag as String
                    } else {
                        // If unchecked and it was the selected one
                        if (currentLabelId == buttonView.tag as String) {
                            currentLabelId = null
                        }
                    }
                    loadNotes()
                }
                binding.chipGroupFilter.addView(chip)
            }
            
            binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
                if (checkedIds.isEmpty()) {
                    currentLabelId = null
                    loadNotes()
                }
            }
        }
    }
    
    private fun searchNotes(query: String) {
        if (query.isEmpty()) {
            loadNotes()
            return
        }
        
        notesRepository.searchNotes(currentUserId, query).observe(this) { notes ->
            notesAdapter.submitList(notes)
            binding.tvEmptyState.visibility = if (notes.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }
    
    private fun syncNotes() {
        lifecycleScope.launch {
            when (notesRepository.syncNotes(currentUserId)) {
                is Resource.Success -> {
                    Toast.makeText(this@MainActivity, "Sync completed", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(this@MainActivity, "Sync failed", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        
        searchView?.apply {
            queryHint = "Search notes..."
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { searchNotes(it) }
                    return true
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { searchNotes(it) }
                    return true
                }
            })
        }
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                syncNotes()
                true
            }
            R.id.action_manage_labels -> {
                showLabelManager()
                true
            }
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showLabelManager() {
        val dialog = com.notesapp.offline.ui.labels.LabelManagerDialogFragment.newInstance(currentUserId)
        dialog.show(supportFragmentManager, "LabelManager")
    }
    
    private fun logout() {
        lifecycleScope.launch {
            authRepository.logout()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun setupPeriodicSync() {
        try {
            // Initialize WorkManager with configuration if not already initialized
            try {
                WorkManager.getInstance(applicationContext)
            } catch (e: IllegalStateException) {
                // WorkManager not initialized, initialize it now
                val config = Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.INFO)
                    .build()
                WorkManager.initialize(applicationContext, config)
            }
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<com.notesapp.offline.SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    15, TimeUnit.MINUTES
                )
                .build()
            
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "NotesSync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to setup periodic sync", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::notesAdapter.isInitialized) {
            loadNotes()
        }
    }
}
