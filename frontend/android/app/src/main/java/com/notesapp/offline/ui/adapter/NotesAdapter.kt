package com.notesapp.offline.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notesapp.offline.R
import com.notesapp.offline.data.local.entity.NoteWithLabels
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val onNoteClick: (NoteWithLabels) -> Unit
) : ListAdapter<NoteWithLabels, NotesAdapter.NoteViewHolder>(NotesDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view, onNoteClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NoteViewHolder(
        itemView: View,
        private val onNoteClick: (NoteWithLabels) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val tvTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvNoteContent)
        private val tvDate: TextView = itemView.findViewById(R.id.tvNoteDate)
        private val tvLabels: TextView = itemView.findViewById(R.id.tvNoteLabels)
        private val tvSyncStatus: TextView = itemView.findViewById(R.id.tvSyncStatus)

        fun bind(noteWithLabels: NoteWithLabels) {
            val note = noteWithLabels.note
            
            tvTitle.text = note.title
            tvContent.text = note.content
            
            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val timestamp = try {
                note.updatedAt.toLong()
            } catch (e: NumberFormatException) {
                System.currentTimeMillis()
            }
            tvDate.text = dateFormat.format(Date(timestamp))
            
            // Display labels
            if (noteWithLabels.labels.isNotEmpty()) {
                tvLabels.visibility = View.VISIBLE
                tvLabels.text = noteWithLabels.labels.joinToString(", ") { it.name }
            } else {
                tvLabels.visibility = View.GONE
            }
            
            // Display sync status
            when (note.syncStatus) {
                0 -> {
                    tvSyncStatus.visibility = View.GONE
                }
                1 -> {
                    tvSyncStatus.visibility = View.VISIBLE
                    tvSyncStatus.text = "Creating..."
                    tvSyncStatus.setBackgroundResource(R.color.sync_pending)
                }
                2 -> {
                    tvSyncStatus.visibility = View.VISIBLE
                    tvSyncStatus.text = "Updating..."
                    tvSyncStatus.setBackgroundResource(R.color.sync_pending)
                }
                3 -> {
                    tvSyncStatus.visibility = View.VISIBLE
                    tvSyncStatus.text = "Deleting..."
                    tvSyncStatus.setBackgroundResource(R.color.sync_error)
                }
            }
            
            itemView.setOnClickListener {
                onNoteClick(noteWithLabels)
            }
        }
    }

    class NotesDiffCallback : DiffUtil.ItemCallback<NoteWithLabels>() {
        override fun areItemsTheSame(oldItem: NoteWithLabels, newItem: NoteWithLabels): Boolean {
            return oldItem.note.id == newItem.note.id
        }

        override fun areContentsTheSame(oldItem: NoteWithLabels, newItem: NoteWithLabels): Boolean {
            return oldItem == newItem
        }
    }
}
