# 📝 Elite Memo Pro

**A modern, feature-rich, 100% offline notepad app for Android — built with Kotlin & Material 3.**

[![Android CI](https://github.com/deepanjanxyz/notepad/actions/workflows/android.yml/badge.svg)](https://github.com/deepanjanxyz/notepad/actions)
![Version](https://img.shields.io/badge/version-2.0.0-6750A4)
![Min SDK](https://img.shields.io/badge/minSdk-24-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF)

---

## ✨ Features

| Feature | Description |
|---|---|
| 📝 **Markdown Support** | Bold, Italic, Strikethrough, Code, Tables, Headings |
| ✅ **Rich Text / Checklist** | Insert `- [ ]` checkbox lists with one tap |
| 🎤 **Voice-to-Text** | **100% Offline** — no internet permission required |
| 🔒 **Per-Note Lock** | Lock individual notes with biometric authentication |
| 🏠 **App Lock** | Protect the whole app with fingerprint / PIN |
| 🔐 **Encrypted Export** | AES-256-GCM backup with password-derived key |
| 📌 **Home Screen Shortcut** | Long-press app icon → "New Note" |
| 🧩 **Widget** | Minimalist home screen widget showing latest note |
| 🎨 **Material 3 Design** | Dynamic tonal colors, adaptive layouts |
| 🌙 **Dark / Light / System Theme** | Fully adaptive |
| 🔍 **Real-time Search** | Search by title or content instantly |
| ✏️ **Auto-Save** | Notes are saved as you type |
| 📋 **Multi-select Delete** | Long-press to select, bulk delete |

---

## 🏗️ Architecture

```
app/
└── src/main/kotlin/com/deepanjanxyz/notepad/
    ├── NoteApplication.kt          ← App class, theme init
    ├── data/
    │   ├── model/Note.kt           ← Data model
    │   └── db/DatabaseHelper.kt    ← SQLite, safe migration v1→v2
    ├── ui/
    │   ├── main/
    │   │   ├── MainActivity.kt     ← Note list, biometric app lock
    │   │   └── NoteAdapter.kt      ← RecyclerView, colored cards
    │   ├── editor/
    │   │   └── NoteEditorActivity.kt ← Markdown, voice, note lock
    │   └── settings/
    │       ├── SettingsActivity.kt
    │       └── backup/BackupManager.kt
    ├── widget/
    │   └── NoteWidgetProvider.kt   ← Home screen widget
    └── util/
        ├── CryptoUtil.kt           ← AES-256-GCM + SHA-256
        └── VoiceInputManager.kt    ← Offline SpeechRecognizer
```

---

## 🔧 Tech Stack

- **Language**: Kotlin 2.1.20
- **Build**: AGP 8.9.0 + Gradle 8.11.1
- **UI**: XML Layouts + Material 3 (`Theme.Material3.DayNight.NoActionBar`)
- **Database**: SQLite (SQLiteOpenHelper with safe migration)
- **Markdown**: [Markwon](https://github.com/noties/Markwon) 4.6.2
- **Encryption**: `javax.crypto` AES-256-GCM (built-in, no library)
- **Voice**: `android.speech.SpeechRecognizer` (`EXTRA_PREFER_OFFLINE = true`)
- **Biometric**: `androidx.biometric:biometric`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)

---

## 🔒 Privacy

- **Zero internet permission** — the app has no `INTERNET` permission
- Voice recognition is **100% on-device** (no cloud calls)
- All backups are AES-256-GCM encrypted with your password
- Notes stay on your device

---

## 🚀 Building

```bash
git clone https://github.com/deepanjanxyz/notepad.git
cd notepad
./gradlew assembleDebug
```

APK will be at `app/build/outputs/apk/debug/app-debug.apk`

---

## 🆕 Changelog

### v2.0.0
- Full Java → **Kotlin** rewrite
- Material 3 UI overhaul
- Per-note biometric lock
- Offline Voice-to-Text
- Markdown preview with rich toolbar
- AES-256-GCM encrypted export
- App shortcuts (New Note)
- Home screen widget
- Extensive Settings screen
- Safe database migration (all existing notes preserved)

### v1.0.4 (legacy Java)
- Basic notes CRUD
- Biometric app lock
- Dark/Light theme
- Search

---

## 📄 License

MIT License — see [LICENSE](LICENSE)

---

*Made with ❤️ by [@deepanjanxyz](https://github.com/deepanjanxyz)*
