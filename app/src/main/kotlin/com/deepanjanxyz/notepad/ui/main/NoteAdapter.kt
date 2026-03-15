package com.deepanjanxyz.notepad.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.data.model.Note
import com.deepanjanxyz.notepad.databinding.ItemNoteBinding

class NoteAdapter(
    private val context: Context,
    private val listener: OnNoteListener
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    interface OnNoteListener {
        fun onNoteClick(note: Note)
        fun onSelectionModeChange(isSelectionMode: Boolean, selectedCount: Int)
    }

    private val notes     = mutableListOf<Note>()
    private val selected  = mutableSetOf<Long>()
    var isSelectionMode   = false
        private set

    // M3 note card tonal surface colors (10 palette entries)
    private val cardColors by lazy {
        intArrayOf(
            context.getColor(R.color.note_color_0),
            context.getColor(R.color.note_color_1),
            context.getColor(R.color.note_color_2),
            context.getColor(R.color.note_color_3),
            context.getColor(R.color.note_color_4),
            context.getColor(R.color.note_color_5),
            context.getColor(R.color.note_color_6),
            context.getColor(R.color.note_color_7),
            context.getColor(R.color.note_color_8),
            context.getColor(R.color.note_color_9),
        )
    }

    // ─── DiffUtil update ─────────────────────────────────────────────────────

    fun submitList(newList: List<Note>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = notes.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(o: Int, n: Int) = notes[o].id == newList[n].id
            override fun areContentsTheSame(o: Int, n: Int) = notes[o] == newList[n]
        })
        notes.clear()
        notes.addAll(newList)
        diff.dispatchUpdatesTo(this)
    }

    // ─── Selection ────────────────────────────────────────────────────────────

    fun clearSelection() {
        isSelectionMode = false
        selected.clear()
        notifyDataSetChanged()
        listener.onSelectionModeChange(false, 0)
    }

    fun getSelectedNotes(): List<Note> = notes.filter { it.id in selected }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun getItemCount() = notes.size

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) =
        holder.bind(notes[position])

    inner class NoteViewHolder(private val b: ItemNoteBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(note: Note) {
            b.textTitle.text   = note.title.ifBlank { context.getString(R.string.untitled) }
            b.textContent.text = note.content
            b.textDate.text    = note.date
            b.iconLock.visibility = if (note.isLocked) View.VISIBLE else View.GONE

            // Colored card background
            val colorIdx = note.colorIndex.coerceIn(0, cardColors.size - 1)
            b.cardNote.setCardBackgroundColor(cardColors[colorIdx])

            // Selection overlay
            if (isSelectionMode) {
                b.checkbox.visibility = View.VISIBLE
                b.checkbox.isChecked  = note.id in selected
            } else {
                b.checkbox.visibility = View.GONE
                b.checkbox.isChecked  = false
            }

            b.root.setOnClickListener {
                if (isSelectionMode) toggleSelection(note)
                else listener.onNoteClick(note)
            }

            b.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    selected.clear()
                }
                toggleSelection(note)
                true
            }
        }

        private fun toggleSelection(note: Note) {
            if (note.id in selected) selected.remove(note.id)
            else selected.add(note.id)

            if (selected.isEmpty()) isSelectionMode = false
            notifyItemChanged(bindingAdapterPosition)
            listener.onSelectionModeChange(isSelectionMode, selected.size)
        }
    }
}
