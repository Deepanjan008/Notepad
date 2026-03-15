package com.deepanjanxyz.notepad.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.deepanjanxyz.notepad.data.model.Note

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME    = "notes.db"
        const val DATABASE_VERSION = 2          // v1 = legacy Java; v2 = adds color + lock

        const val TABLE_NOTES  = "notes_table"
        const val COL_ID       = "ID"
        const val COL_TITLE    = "TITLE"
        const val COL_CONTENT  = "CONTENT"
        const val COL_DATE     = "DATE"
        const val COL_COLOR    = "COLOR"
        const val COL_LOCKED   = "IS_LOCKED"
        const val COL_LOCK_PIN = "LOCK_PIN"
    }

    // ─────────────────────── lifecycle ────────────────────────────────────────

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_NOTES (
                $COL_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE    TEXT,
                $COL_CONTENT  TEXT,
                $COL_DATE     TEXT,
                $COL_COLOR    INTEGER DEFAULT 0,
                $COL_LOCKED   INTEGER DEFAULT 0,
                $COL_LOCK_PIN TEXT
            )
        """.trimIndent())
    }

    /**
     * Safe migration: ALTER TABLE only adds columns, never drops.
     * Existing user data is fully preserved.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            runCatching { db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COL_COLOR    INTEGER DEFAULT 0") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COL_LOCKED   INTEGER DEFAULT 0") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COL_LOCK_PIN TEXT") }
        }
    }

    // ─────────────────────── CRUD ─────────────────────────────────────────────

    fun insertNote(title: String, content: String, date: String, colorIndex: Int = 0): Long =
        writableDatabase.insert(TABLE_NOTES, null, ContentValues().apply {
            put(COL_TITLE,   title)
            put(COL_CONTENT, content)
            put(COL_DATE,    date)
            put(COL_COLOR,   colorIndex)
            put(COL_LOCKED,  0)
        })

    fun updateNote(id: Long, title: String, content: String, date: String): Boolean =
        writableDatabase.update(TABLE_NOTES,
            ContentValues().apply {
                put(COL_TITLE,   title)
                put(COL_CONTENT, content)
                put(COL_DATE,    date)
            },
            "$COL_ID = ?", arrayOf(id.toString())
        ) > 0

    fun updateNoteLock(id: Long, isLocked: Boolean, pin: String?): Boolean =
        writableDatabase.update(TABLE_NOTES,
            ContentValues().apply {
                put(COL_LOCKED,   if (isLocked) 1 else 0)
                put(COL_LOCK_PIN, pin)
            },
            "$COL_ID = ?", arrayOf(id.toString())
        ) > 0

    fun deleteNote(id: Long): Boolean =
        writableDatabase.delete(TABLE_NOTES, "$COL_ID = ?", arrayOf(id.toString())) > 0

    fun deleteNotes(ids: List<Long>): Int {
        val placeholders = ids.joinToString(",") { "?" }
        return writableDatabase.delete(TABLE_NOTES, "$COL_ID IN ($placeholders)",
            ids.map { it.toString() }.toTypedArray())
    }

    fun getAllNotes(): List<Note> = readableDatabase
        .rawQuery("SELECT * FROM $TABLE_NOTES ORDER BY $COL_ID DESC", null)
        .use { it.toNoteList() }

    fun searchNotes(query: String): List<Note> = readableDatabase
        .rawQuery(
            "SELECT * FROM $TABLE_NOTES WHERE $COL_TITLE LIKE ? OR $COL_CONTENT LIKE ? ORDER BY $COL_ID DESC",
            arrayOf("%$query%", "%$query%")
        ).use { it.toNoteList() }

    // ─────────────────────── Cursor helpers ───────────────────────────────────

    private fun Cursor.toNoteList(): List<Note> {
        val list = mutableListOf<Note>()
        while (moveToNext()) list.add(toNote())
        return list
    }

    private fun Cursor.toNote() = Note(
        id         = getLong(getColumnIndexOrThrow(COL_ID)),
        title      = getString(getColumnIndexOrThrow(COL_TITLE))    ?: "",
        content    = getString(getColumnIndexOrThrow(COL_CONTENT))  ?: "",
        date       = getString(getColumnIndexOrThrow(COL_DATE))     ?: "",
        colorIndex = safeInt(COL_COLOR),
        isLocked   = safeInt(COL_LOCKED) == 1,
        lockPin    = safeString(COL_LOCK_PIN)
    )

    /** Safe column read – returns 0 if column doesn't exist yet (pre-migration reads) */
    private fun Cursor.safeInt(col: String): Int {
        val idx = getColumnIndex(col)
        return if (idx >= 0) getInt(idx) else 0
    }

    private fun Cursor.safeString(col: String): String? {
        val idx = getColumnIndex(col)
        return if (idx >= 0) getString(idx) else null
    }
}
