package com.example.aws

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import java.util.Locale
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val language = prefs.getString("app_language", "en") ?: "en"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        if (prefs.getString("app_theme", "green") == "dark") {
            setTheme(R.style.Theme_AWS_Dark)
        }
        super.onCreate(savedInstanceState)
        applyNotificationSettings()
    }

    private fun applyNotificationSettings() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        Log.d("BaseActivity", "Notifications enabled: $notificationsEnabled")
    }
}
