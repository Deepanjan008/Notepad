package com.deepanjanxyz.notepad.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.data.db.DatabaseHelper
import com.deepanjanxyz.notepad.data.model.Note
import com.deepanjanxyz.notepad.databinding.ActivityMainBinding
import com.deepanjanxyz.notepad.ui.editor.NoteEditorActivity
import com.deepanjanxyz.notepad.ui.settings.SettingsActivity

class MainActivity : AppCompatActivity(), NoteAdapter.OnNoteListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: NoteAdapter

    private var mainMenu: Menu? = null
    private var isAuthenticated = false

    // ─── lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)

        if (isAppLockEnabled() && !isAuthenticated) {
            // Show blank screen while biometric prompt is showing
            setContentView(View(this))
            showBiometricPrompt()
        } else {
            initUI()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAuthenticated || !isAppLockEnabled()) loadNotes("")
    }

    override fun onBackPressed() {
        if (adapter.isSelectionMode) adapter.clearSelection()
        else super.onBackPressed()
    }

    // ─── init ─────────────────────────────────────────────────────────────────

    private fun initUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        val spanCount = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("pref_layout", "grid")?.let { if (it == "list") 1 else 2 } ?: 2

        adapter = NoteAdapter(this, this)
        binding.recyclerView.layoutManager =
            StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
        binding.recyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, NoteEditorActivity::class.java))
        }

        loadNotes("")
    }

    // ─── Biometric / App lock ─────────────────────────────────────────────────

    private fun isAppLockEnabled(): Boolean =
        PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_lock", false)

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isAuthenticated = true
                initUI()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                finish()
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_name))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                ).build()
        )
    }

    // ─── NoteAdapter.OnNoteListener ───────────────────────────────────────────

    override fun onNoteClick(note: Note) {
        if (note.isLocked) {
            showNoteUnlockPrompt(note)
        } else {
            openNote(note)
        }
    }

    override fun onSelectionModeChange(isSelectionMode: Boolean, selectedCount: Int) {
        mainMenu?.findItem(R.id.action_delete_selected)?.isVisible = isSelectionMode
        mainMenu?.findItem(R.id.action_search)?.isVisible          = !isSelectionMode
        mainMenu?.findItem(R.id.action_settings)?.isVisible        = !isSelectionMode
        supportActionBar?.title = if (isSelectionMode)
            "$selectedCount ${getString(R.string.selected)}"
        else
            getString(R.string.app_name)
    }

    // ─── Note unlock (biometric) before opening a locked note ─────────────────

    private fun showNoteUnlockPrompt(note: Note) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                openNote(note)
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(this@MainActivity, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show()
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_note))
                .setSubtitle(note.title.ifBlank { getString(R.string.untitled) })
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                ).build()
        )
    }

    private fun openNote(note: Note) {
        startActivity(Intent(this, NoteEditorActivity::class.java).apply {
            putExtra(NoteEditorActivity.EXTRA_NOTE_ID,      note.id)
            putExtra(NoteEditorActivity.EXTRA_NOTE_TITLE,   note.title)
            putExtra(NoteEditorActivity.EXTRA_NOTE_CONTENT, note.content)
            putExtra(NoteEditorActivity.EXTRA_NOTE_COLOR,   note.colorIndex)
            putExtra(NoteEditorActivity.EXTRA_NOTE_LOCKED,  note.isLocked)
        })
    }

    // ─── Menu ─────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        mainMenu = menu

        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String) = false.also { loadNotes(q) }
            override fun onQueryTextChange(q: String) = false.also { loadNotes(q) }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings         -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            R.id.action_delete_selected  -> { showDeleteConfirmation(); true }
            else                          -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmation() {
        val toDelete = adapter.getSelectedNotes()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_notes))
            .setMessage(getString(R.string.delete_notes_msg, toDelete.size))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                db.deleteNotes(toDelete.map { it.id })
                adapter.clearSelection()
                loadNotes("")
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ─── Load notes ───────────────────────────────────────────────────────────

    private fun loadNotes(query: String) {
        val notes = if (query.isEmpty()) db.getAllNotes() else db.searchNotes(query)
        adapter.submitList(notes)
        binding.emptyView.visibility     = if (notes.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility  = if (notes.isEmpty()) View.GONE    else View.VISIBLE
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    private fun applyTheme() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        when (prefs.getString("pref_theme", "system")) {
            "dark"  -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else    -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
