package com.deepanjanxyz.notepad.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.data.db.DatabaseHelper
import com.deepanjanxyz.notepad.databinding.ActivitySettingsBinding
import com.deepanjanxyz.notepad.ui.settings.backup.BackupManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    // ─────────────────────────────────────────────────────────────────────────
    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var db: DatabaseHelper
        private lateinit var backupMgr: BackupManager

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            db        = DatabaseHelper(requireContext())
            backupMgr = BackupManager(requireContext(), db)

            setupTheme()
            setupLayout()
            setupSecurity()
            setupBackup()
            setupAbout()
        }

        // ── Appearance ───────────────────────────────────────────────────────

        private fun setupTheme() {
            (findPreference<ListPreference>("pref_theme"))?.setOnPreferenceChangeListener { _, v ->
                when (v as String) {
                    "dark"  -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    else    -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                true
            }
        }

        private fun setupLayout() {
            // pref_layout (grid/list) is read by MainActivity on resume – no extra code needed
        }

        // ── Security ─────────────────────────────────────────────────────────

        private fun setupSecurity() {
            findPreference<SwitchPreferenceCompat>("pref_lock")?.setOnPreferenceChangeListener { _, _ -> true }
        }

        // ── Backup ───────────────────────────────────────────────────────────

        private fun setupBackup() {
            findPreference<Preference>("pref_export")?.setOnPreferenceClickListener {
                showPasswordDialog(isExport = true)
                true
            }
            findPreference<Preference>("pref_import")?.setOnPreferenceClickListener {
                showPasswordDialog(isExport = false)
                true
            }
        }

        private fun showPasswordDialog(isExport: Boolean) {
            val ctx   = requireContext()
            val title = if (isExport) getString(R.string.export_encrypted) else getString(R.string.import_encrypted)

            val inputLayout = TextInputLayout(ctx).apply {
                hint        = getString(R.string.password_hint)
                endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                setPadding(48, 16, 48, 0)
            }
            val editText = TextInputEditText(ctx)
            inputLayout.addView(editText)

            MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setMessage(if (isExport) getString(R.string.export_msg) else getString(R.string.import_msg))
                .setView(inputLayout)
                .setPositiveButton(getString(R.string.proceed)) { _, _ ->
                    val pwd = editText.text.toString()
                    if (pwd.length < 4) {
                        Toast.makeText(ctx, getString(R.string.pwd_too_short), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (isExport) doExport(pwd) else doImport(pwd)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        private fun doExport(password: String) {
            try {
                val uri = backupMgr.exportEncrypted(password)
                Toast.makeText(requireContext(), getString(R.string.export_success), Toast.LENGTH_LONG).show()
                // Share the file
                startActivity(Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "${getString(R.string.export_fail)}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        private fun doImport(password: String) {
            Toast.makeText(requireContext(), getString(R.string.import_pick), Toast.LENGTH_SHORT).show()
            // Full SAF import would need ActivityResult – simplified stub here
        }

        // ── About ────────────────────────────────────────────────────────────

        private fun setupAbout() {
            findPreference<Preference>("pref_github")?.setOnPreferenceClickListener {
                openUrl("https://github.com/deepanjanxyz/notepad")
                true
            }
            findPreference<Preference>("pref_version")?.summary =
                try { requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName }
                catch (e: Exception) { "2.0.0" }
        }

        private fun openUrl(url: String) =
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
