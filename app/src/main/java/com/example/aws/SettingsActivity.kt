package com.example.aws

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import com.google.android.material.button.MaterialButton

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_page)

        // ── Back button ───────────────────────────────────────────
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        // ── Profile section ───────────────────────────────────────
        // Pull name and role from SharedPreferences saved at login
        val prefs      = getSharedPreferences("app_settings", MODE_PRIVATE)
        val userPrefs  = getSharedPreferences("user_session", MODE_PRIVATE)

        // Try to get name from intent extras first, fall back to prefs
        val firstName  = intent.getStringExtra("first_name")
            ?: userPrefs.getString("first_name", "") ?: ""
        val lastName   = intent.getStringExtra("last_name")
            ?: userPrefs.getString("last_name", "") ?: ""
        val role       = intent.getStringExtra("role")
            ?: userPrefs.getString("role", "User") ?: "User"

        val fullName   = "$firstName $lastName".trim().ifEmpty { "User" }
        val initial    = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        val roleLabel  = role.replaceFirstChar { it.uppercaseChar() }

        findViewById<TextView>(R.id.profile_name).text   = fullName
        findViewById<TextView>(R.id.profile_role).text   = roleLabel
        findViewById<TextView>(R.id.profile_avatar).text = initial

        // ── Notifications ─────────────────────────────────────────
        val notificationsSwitch = findViewById<SwitchCompat>(R.id.notifications_switch)
        notificationsSwitch.isChecked = prefs.getBoolean("notifications_enabled", true)
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("notifications_enabled", isChecked) }
            syncToUserPrefs("notifications_enabled", isChecked)
        }

        // ── Theme buttons ─────────────────────────────────────────
        val btnThemeGreen = findViewById<android.widget.Button>(R.id.btn_theme_green)
        val btnThemeDark  = findViewById<android.widget.Button>(R.id.btn_theme_dark)
        val savedTheme    = prefs.getString("app_theme", "green") ?: "green"

        updateThemeButtons(btnThemeGreen, btnThemeDark, savedTheme)

        btnThemeGreen.setOnClickListener {
            prefs.edit { putString("app_theme", "green") }
            syncToUserPrefs("app_theme", "green")
            updateThemeButtons(btnThemeGreen, btnThemeDark, "green")
            setResult(RESULT_OK)
            finish()
        }

        btnThemeDark.setOnClickListener {
            prefs.edit { putString("app_theme", "dark") }
            syncToUserPrefs("app_theme", "dark")
            updateThemeButtons(btnThemeGreen, btnThemeDark, "dark")
            setResult(RESULT_OK)
            finish()
        }

        // ── Language buttons ──────────────────────────────────────
        val btnEnglish = findViewById<android.widget.Button>(R.id.btn_english)
        val btnArabic  = findViewById<android.widget.Button>(R.id.btn_arabic)
        val savedLang  = prefs.getString("app_language", "en")

        updateLanguageButtons(btnEnglish, btnArabic, savedLang ?: "en")

        btnEnglish.setOnClickListener {
            prefs.edit { putString("app_language", "en") }
            syncToUserPrefs("app_language", "en")
            updateLanguageButtons(btnEnglish, btnArabic, "en")
            setLocale("en")
            setResult(RESULT_OK)
            finish()
        }

        btnArabic.setOnClickListener {
            prefs.edit { putString("app_language", "ar") }
            syncToUserPrefs("app_language", "ar")
            updateLanguageButtons(btnEnglish, btnArabic, "ar")
            setLocale("ar")
            setResult(RESULT_OK)
            finish()
        }

        // ── About AMS dialog ──────────────────────────────────────
        findViewById<LinearLayout>(R.id.about_app).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_about_title))
                .setMessage(getString(R.string.dialog_about_message))
                .setPositiveButton(getString(R.string.dialog_close), null)
                .show()
        }

        // ── Logout ────────────────────────────────────────────────
        findViewById<MaterialButton>(R.id.logout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_logout_title))
                .setMessage(getString(R.string.dialog_logout_message))
                .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                    getSharedPreferences("app_settings",  MODE_PRIVATE).edit().clear().apply()
                    getSharedPreferences("user_session",  MODE_PRIVATE).edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun syncToUserPrefs(key: String, value: String) {
        val userId = getSharedPreferences("user_session", MODE_PRIVATE).getString("user_id", "") ?: ""
        if (userId.isNotEmpty()) {
            getSharedPreferences("user_prefs_$userId", MODE_PRIVATE).edit { putString(key, value) }
        }
    }

    private fun syncToUserPrefs(key: String, value: Boolean) {
        val userId = getSharedPreferences("user_session", MODE_PRIVATE).getString("user_id", "") ?: ""
        if (userId.isNotEmpty()) {
            getSharedPreferences("user_prefs_$userId", MODE_PRIVATE).edit { putBoolean(key, value) }
        }
    }

    private fun updateLanguageButtons(
        btnEn: android.widget.Button,
        btnAr: android.widget.Button,
        selected: String
    ) {
        if (selected == "ar") {
            btnAr.setBackgroundResource(R.drawable.language_selected)
            btnAr.setTextColor(android.graphics.Color.parseColor("#2E4F3D"))
            btnEn.setBackgroundResource(R.drawable.language_unselected)
            btnEn.setTextColor(android.graphics.Color.WHITE)
        } else {
            btnEn.setBackgroundResource(R.drawable.language_selected)
            btnEn.setTextColor(android.graphics.Color.parseColor("#2E4F3D"))
            btnAr.setBackgroundResource(R.drawable.language_unselected)
            btnAr.setTextColor(android.graphics.Color.WHITE)
        }
    }

    private fun updateThemeButtons(
        btnGreen: android.widget.Button,
        btnDark: android.widget.Button,
        selected: String
    ) {
        if (selected == "dark") {
            btnDark.setBackgroundResource(R.drawable.language_selected)
            btnDark.setTextColor(android.graphics.Color.parseColor("#2E4F3D"))
            btnGreen.setBackgroundResource(R.drawable.language_unselected)
            btnGreen.setTextColor(android.graphics.Color.WHITE)
        } else {
            btnGreen.setBackgroundResource(R.drawable.language_selected)
            btnGreen.setTextColor(android.graphics.Color.parseColor("#2E4F3D"))
            btnDark.setBackgroundResource(R.drawable.language_unselected)
            btnDark.setTextColor(android.graphics.Color.WHITE)
        }
    }

    private fun setLocale(language: String) {
        val locale = java.util.Locale(language)
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}