package com.deepanjanxyz.notepad.data.model

data class Note(
    val id: Long = -1L,
    val title: String = "",
    val content: String = "",
    val date: String = "",
    val colorIndex: Int = 0,
    val isLocked: Boolean = false,
    val lockPin: String? = null   // SHA-256 hashed PIN; null = use biometric only
)
