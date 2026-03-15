package com.deepanjanxyz.notepad.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.data.db.DatabaseHelper
import com.deepanjanxyz.notepad.databinding.ActivityNoteEditorBinding
import com.deepanjanxyz.notepad.util.CryptoUtil
import com.deepanjanxyz.notepad.util.VoiceInputManager
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID      = "note_id"
        const val EXTRA_NOTE_TITLE   = "title"
        const val EXTRA_NOTE_CONTENT = "content"
        const val EXTRA_NOTE_COLOR   = "color"
        const val EXTRA_NOTE_LOCKED  = "is_locked"
        private const val REQ_RECORD_AUDIO = 101
    }

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var db: DatabaseHelper
    private lateinit var voice: VoiceInputManager
    private lateinit var markwon: Markwon

    private var noteId     = -1L
    private var colorIdx   = 0
    private var isLocked   = false
    private var isPreview  = false
    private var isListening = false

    // ─── lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db    = DatabaseHelper(this)
        voice = VoiceInputManager(this)
        markwon = Markwon.builder(this)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(this))
            .usePlugin(TaskListPlugin.create(this))
            .build()

        // Restore extras
        intent?.let {
            noteId   = it.getLongExtra(EXTRA_NOTE_ID, -1L)
            colorIdx = it.getIntExtra(EXTRA_NOTE_COLOR, 0)
            isLocked = it.getBooleanExtra(EXTRA_NOTE_LOCKED, false)
            binding.etTitle.setText(it.getStringExtra(EXTRA_NOTE_TITLE) ?: "")
            binding.etContent.setText(it.getStringExtra(EXTRA_NOTE_CONTENT) ?: "")
        }

        updateLockIcon()
        setupAutoSave()
        setupFormatToolbar()
        setupVoiceButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        voice.stop()
    }

    // ─── Menu ─────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_preview)?.setIcon(
            if (isPreview) R.drawable.ic_edit else R.drawable.ic_preview
        )
        menu.findItem(R.id.action_lock_note)?.setIcon(
            if (isLocked) R.drawable.ic_lock else R.drawable.ic_lock_open
        )
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home     -> { finish(); true }
        R.id.action_save      -> { saveNote(); finish(); true }
        R.id.action_preview   -> { togglePreview(); true }
        R.id.action_lock_note -> { toggleNoteLock(); true }
        else                   -> super.onOptionsItemSelected(item)
    }

    // ─── Auto-save ────────────────────────────────────────────────────────────

    private fun setupAutoSave() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) { saveNote() }
        }
        binding.etTitle.addTextChangedListener(watcher)
        binding.etContent.addTextChangedListener(watcher)
    }

    private fun saveNote() {
        val title   = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        if (title.isEmpty() && content.isEmpty()) return

        val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        if (noteId == -1L) {
            noteId = db.insertNote(title, content, date, colorIdx)
        } else {
            db.updateNote(noteId, title, content, date)
        }
    }

    // ─── Markdown Preview toggle ──────────────────────────────────────────────

    private fun togglePreview() {
        isPreview = !isPreview
        if (isPreview) {
            saveNote()
            markwon.setMarkdown(binding.tvPreview, binding.etContent.text.toString())
            binding.etContent.visibility = View.GONE
            binding.tvPreview.visibility = View.VISIBLE
            binding.formatToolbar.visibility = View.GONE
        } else {
            binding.etContent.visibility = View.VISIBLE
            binding.tvPreview.visibility = View.GONE
            binding.formatToolbar.visibility = View.VISIBLE
        }
        invalidateOptionsMenu()
    }

    // ─── Format toolbar ───────────────────────────────────────────────────────

    private fun setupFormatToolbar() {
        binding.btnBold.setOnClickListener      { wrapSelection("**") }
        binding.btnItalic.setOnClickListener    { wrapSelection("_") }
        binding.btnCode.setOnClickListener      { wrapSelection("`") }
        binding.btnCodeBlock.setOnClickListener { wrapBlock("```\n", "\n```") }
        binding.btnCheckbox.setOnClickListener  { insertAtCursor("- [ ] ") }
        binding.btnH1.setOnClickListener        { insertLinePrefix("# ") }
        binding.btnH2.setOnClickListener        { insertLinePrefix("## ") }
        binding.btnQuote.setOnClickListener     { insertLinePrefix("> ") }
        binding.btnStrike.setOnClickListener    { wrapSelection("~~") }
    }

    private fun wrapSelection(marker: String) {
        val et    = binding.etContent
        val start = et.selectionStart
        val end   = et.selectionEnd
        val text  = et.text ?: return
        if (start == end) {
            text.insert(start, "$marker$marker")
            et.setSelection(start + marker.length)
        } else {
            text.insert(end,   marker)
            text.insert(start, marker)
            et.setSelection(end + marker.length * 2)
        }
    }

    private fun wrapBlock(prefix: String, suffix: String) {
        val et    = binding.etContent
        val start = et.selectionStart
        val end   = et.selectionEnd
        val text  = et.text ?: return
        text.insert(end,   suffix)
        text.insert(start, prefix)
        et.setSelection(start + prefix.length)
    }

    private fun insertAtCursor(s: String) {
        val et  = binding.etContent
        val pos = et.selectionStart.coerceAtLeast(0)
        et.text?.insert(pos, s)
        et.setSelection(pos + s.length)
    }

    private fun insertLinePrefix(prefix: String) {
        val et    = binding.etContent
        val pos   = et.selectionStart.coerceAtLeast(0)
        val text  = et.text?.toString() ?: return
        val lineStart = text.lastIndexOf('\n', pos - 1) + 1
        et.text?.insert(lineStart, prefix)
    }

    // ─── Voice input (offline) ────────────────────────────────────────────────

    private fun setupVoiceButton() {
        if (!voice.isAvailable) {
            binding.btnVoice.visibility = View.GONE
            return
        }
        binding.btnVoice.setOnClickListener {
            if (isListening) {
                voice.stop()
                isListening = false
                binding.btnVoice.setImageResource(R.drawable.ic_mic)
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_RECORD_AUDIO)
                    return@setOnClickListener
                }
                startListening()
            }
        }
    }

    private fun startListening() {
        isListening = true
        binding.btnVoice.setImageResource(R.drawable.ic_mic_active)
        Toast.makeText(this, getString(R.string.listening), Toast.LENGTH_SHORT).show()

        voice.start(object : VoiceInputManager.Listener {
            override fun onReadyForSpeech() = Unit
            override fun onEndOfSpeech() {
                isListening = false
                runOnUiThread { binding.btnVoice.setImageResource(R.drawable.ic_mic) }
            }
            override fun onResult(text: String) {
                isListening = false
                runOnUiThread {
                    binding.btnVoice.setImageResource(R.drawable.ic_mic)
                    insertAtCursor(text)
                }
            }
            override fun onError(message: String) {
                isListening = false
                runOnUiThread {
                    binding.btnVoice.setImageResource(R.drawable.ic_mic)
                    Toast.makeText(this@NoteEditorActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_RECORD_AUDIO &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startListening()
        }
    }

    // ─── Note lock toggle ─────────────────────────────────────────────────────

    private fun toggleNoteLock() {
        if (noteId == -1L) { saveNote() }
        if (isLocked) {
            // Unlock: authenticate first
            showBiometricForUnlock()
        } else {
            // Lock
            showLockConfirmation()
        }
    }

    private fun showLockConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.lock_note))
            .setMessage(getString(R.string.lock_note_msg))
            .setPositiveButton(getString(R.string.lock)) { _, _ ->
                isLocked = true
                db.updateNoteLock(noteId, true, null)
                updateLockIcon()
                invalidateOptionsMenu()
                Toast.makeText(this, getString(R.string.note_locked), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showBiometricForUnlock() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isLocked = false
                db.updateNoteLock(noteId, false, null)
                updateLockIcon()
                invalidateOptionsMenu()
                Toast.makeText(this@NoteEditorActivity, getString(R.string.note_unlocked), Toast.LENGTH_SHORT).show()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(this@NoteEditorActivity, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show()
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_note))
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                ).build()
        )
    }

    private fun updateLockIcon() {
        binding.toolbar.subtitle = if (isLocked)
            "🔒 ${getString(R.string.note_locked_label)}"
        else ""
    }
}
