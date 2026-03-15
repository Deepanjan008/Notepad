package com.deepanjanxyz.notepad.ui.settings.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.deepanjanxyz.notepad.data.db.DatabaseHelper
import com.deepanjanxyz.notepad.data.model.Note
import com.deepanjanxyz.notepad.util.CryptoUtil
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BackupManager(private val context: Context, private val db: DatabaseHelper) {

    /** Exports all notes as encrypted JSON → saves to cache dir → returns URI */
    fun exportEncrypted(password: String): Uri {
        val notes = db.getAllNotes()
        val json  = notesToJson(notes)
        val encrypted = CryptoUtil.encrypt(json.toByteArray(Charsets.UTF_8), password)

        val file = File(context.cacheDir, "elite_memo_backup.emb")
        file.writeBytes(encrypted)

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    /** Imports encrypted backup file – overwrites nothing, just inserts new notes */
    fun importEncrypted(uri: Uri, password: String): Int {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: error("Cannot read file")
        val json  = CryptoUtil.decrypt(bytes, password).toString(Charsets.UTF_8)
        val arr   = JSONArray(json)
        var count = 0
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            db.insertNote(
                title      = obj.optString("title"),
                content    = obj.optString("content"),
                date       = obj.optString("date"),
                colorIndex = obj.optInt("color", 0)
            )
            count++
        }
        return count
    }

    // ─── JSON helpers ─────────────────────────────────────────────────────────

    private fun notesToJson(notes: List<Note>): String {
        val arr = JSONArray()
        notes.forEach { note ->
            arr.put(JSONObject().apply {
                put("id",      note.id)
                put("title",   note.title)
                put("content", note.content)
                put("date",    note.date)
                put("color",   note.colorIndex)
            })
        }
        return arr.toString()
    }
}
