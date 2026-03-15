# Contributing to Elite Memo Pro

Thanks for your interest! Here's how to contribute.

## Setup

1. Fork the repo
2. Clone: `git clone https://github.com/YOUR_USERNAME/notepad.git`
3. Open in Android Studio Ladybug (2024.2.x) or newer
4. Build: `./gradlew assembleDebug`

## Code Style

- **Kotlin** only (no Java files)
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `ktlint` formatting
- No `!!` null assertions — use safe calls and elvis operators

## Branches

| Branch | Purpose |
|---|---|
| `main` | Stable releases |
| `develop` | Active development |
| `feature/*` | New features |
| `fix/*` | Bug fixes |

## Pull Request Checklist

- [ ] Builds without errors: `./gradlew assembleDebug`
- [ ] No new `INTERNET` permission added (the app must stay 100% offline)
- [ ] Material 3 design guidelines followed
- [ ] New strings added to `strings.xml`
- [ ] DB schema changes include a safe `onUpgrade` migration

## Reporting Bugs

Open an issue with:
- Device model + Android version
- Steps to reproduce
- Expected vs actual behavior
- Logcat output (if available)

## Feature Requests

Open an issue with the `enhancement` label and describe your use case.

---

*Package name: `com.deepanjanxyz.notepad`*  
*Maintainer: [@deepanjanxyz](https://github.com/deepanjanxyz)*
