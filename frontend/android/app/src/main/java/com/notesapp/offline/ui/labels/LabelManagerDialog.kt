package com.notesapp.offline.ui.labels

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.notesapp.offline.R
import com.notesapp.offline.data.local.entity.LabelEntity
import com.notesapp.offline.data.repository.NotesRepository
import com.notesapp.offline.util.Resource
import kotlinx.coroutines.launch

class LabelManagerDialogFragment : DialogFragment() {

    private lateinit var notesRepository: NotesRepository
    private var userId: String = ""
    private lateinit var etNewLabel: TextInputEditText
    private lateinit var btnAddLabel: MaterialButton
    private lateinit var rvLabels: RecyclerView
    private lateinit var adapter: LabelAdapter

    companion object {
        fun newInstance(userId: String): LabelManagerDialogFragment {
            val fragment = LabelManagerDialogFragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_label_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userId = arguments?.getString("USER_ID") ?: return
        notesRepository = NotesRepository(requireContext())
        
        etNewLabel = view.findViewById(R.id.etNewLabel)
        btnAddLabel = view.findViewById(R.id.btnAddLabel)
        rvLabels = view.findViewById(R.id.rvLabels)
        
        adapter = LabelAdapter { label ->
            deleteLabel(label)
        }
        rvLabels.adapter = adapter
        
        setupObservers()
        
        btnAddLabel.setOnClickListener {
            val name = etNewLabel.text.toString().trim()
            if (name.isNotEmpty()) {
                createLabel(name)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupObservers() {
        notesRepository.getAllLabels(userId).observe(viewLifecycleOwner) { labels ->
            adapter.submitList(labels)
        }
    }

    private fun createLabel(name: String) {
        lifecycleScope.launch {
            etNewLabel.isEnabled = false
            btnAddLabel.isEnabled = false
            
            when (val result = notesRepository.createLabel(userId, name)) {
                is Resource.Success -> {
                    etNewLabel.setText("")
                    Toast.makeText(context, "Label created", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
            
            etNewLabel.isEnabled = true
            btnAddLabel.isEnabled = true
        }
    }
    
    private fun deleteLabel(label: LabelEntity) {
        lifecycleScope.launch {
            when (val result = notesRepository.deleteLabel(label.id)) {
                is Resource.Success -> {
                    Toast.makeText(context, "Label deleted", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}

class LabelAdapter(private val onDeleteClick: (LabelEntity) -> Unit) : RecyclerView.Adapter<LabelAdapter.LabelViewHolder>() {
    private var labels = listOf<LabelEntity>()

    fun submitList(newLabels: List<LabelEntity>) {
        labels = newLabels
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return LabelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val label = labels[position]
        val text1 = holder.itemView.findViewById<TextView>(android.R.id.text1)
        val text2 = holder.itemView.findViewById<TextView>(android.R.id.text2)
        
        text1.text = label.name
        text2.text = "Delete" // Simple text button behavior
        
        text2.setOnClickListener {
            onDeleteClick(label)
        }
    }
    
    override fun getItemCount(): Int = labels.size

    class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
