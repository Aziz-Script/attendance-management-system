package com.example.aws

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    // ── Channel IDs ────────────────────────────────────────────────
    private const val CHANNEL_SESSION = "ams_session_channel"
    private const val CHANNEL_CONFIRM = "ams_confirm_channel"

    // ── Notification IDs ───────────────────────────────────────────
    const val NOTIF_ACTIVE_SESSION      = 2001
    const val NOTIF_ATTENDANCE_CONFIRMED = 2002

    // ── Create channels ────────────────────────────────────────────
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            // HIGH importance = drops from top as heads-up notification
            val sessionChannel = NotificationChannel(
                CHANNEL_SESSION,
                context.getString(R.string.notif_channel_session_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_session_desc)
                enableVibration(true)
                enableLights(true)
            }

            // LOW importance = silent, appears in shade only
            val confirmChannel = NotificationChannel(
                CHANNEL_CONFIRM,
                context.getString(R.string.notif_channel_confirm_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_confirm_desc)
            }

            manager.createNotificationChannel(sessionChannel)
            manager.createNotificationChannel(confirmChannel)
        }
    }

    // ── Notification 1: Active session — drops from top ────────────
    fun showActiveSessionNotification(
        context: Context,
        courseName: String,
        courseCode: String,
        code: String,
        sectionId: String
    ) {
        // Tapping opens mark attendance directly
        val intent = Intent(context, StudentAttendanceActivity::class.java).apply {
            putExtra("section_id",  sectionId)
            putExtra("course_code", courseCode)
            putExtra("course_name", courseName)
            putExtra("auto_code", code)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, NOTIF_ACTIVE_SESSION, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notif_session_title))
            .setContentText(context.getString(R.string.notif_session_text, courseName, courseCode, code))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notif_session_big_text, courseName, code))
            )
            // These three together guarantee heads-up drop from top
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ACTIVE_SESSION, notification)
    }

    // ── Notification 2: Attendance confirmed — silent receipt ───────
    fun showAttendanceConfirmedNotification(
        context: Context,
        courseName: String,
        courseCode: String,
        status: String
    ) {
        val statusText = when (status) {
            "P"  -> context.getString(R.string.notif_status_present)
            "L"  -> context.getString(R.string.notif_status_late)
            else -> context.getString(R.string.notif_status_recorded)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_CONFIRM)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notif_confirm_title, statusText))
            .setContentText(context.getString(R.string.notif_confirm_text, courseName, courseCode))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notif_confirm_big_text, courseName, statusText))
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ATTENDANCE_CONFIRMED, notification)
    }

    // ── Cancel a specific notification ─────────────────────────────
    fun cancel(context: Context, notifId: Int) {
        context.getSystemService(NotificationManager::class.java).cancel(notifId)
    }
}