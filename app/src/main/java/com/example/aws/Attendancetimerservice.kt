package com.example.aws

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AttendanceTimerService : Service() {

    inner class TimerBinder : Binder() {
        fun getService(): AttendanceTimerService = this@AttendanceTimerService
    }

    private val binder = TimerBinder()

    private var countDownTimer: CountDownTimer? = null
    var timeLeftSeconds: Int = 0
        private set
    var isRunning: Boolean = false
        private set
    var generatedCode: String = ""
        private set
    private var sectionId: String = ""

    // UI callbacks — set by the bound Activity
    var onTick: ((secondsLeft: Int) -> Unit)? = null
    var onFinish: (() -> Unit)? = null

    companion object {
        const val CHANNEL_ID      = "attendance_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_SECONDS   = "extra_seconds"
        const val EXTRA_CODE      = "extra_code"
        const val EXTRA_SECTION   = "extra_section"
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getIntExtra(EXTRA_SECONDS, 0) ?: 0
        val code    = intent?.getStringExtra(EXTRA_CODE) ?: ""
        val section = intent?.getStringExtra(EXTRA_SECTION) ?: ""

        if (seconds > 0 && !isRunning) {
            generatedCode = code
            sectionId     = section
            startTimer(seconds)
        }

        return START_NOT_STICKY
    }

    fun startTimer(seconds: Int) {
        countDownTimer?.cancel()
        isRunning       = true
        timeLeftSeconds = seconds

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(seconds))

        countDownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {

            override fun onTick(millisUntilFinished: Long) {
                timeLeftSeconds = (millisUntilFinished / 1000).toInt()
                onTick?.invoke(timeLeftSeconds)
                updateNotification(timeLeftSeconds)
            }

            override fun onFinish() {
                Log.d("TIMER_SERVICE", "onFinish called — calling closeSession")
                timeLeftSeconds = 0
                isRunning       = false

                // Automatically mark absent students when the session closes
                closeSession()

                // Notify the UI if the activity is bound
                onFinish?.invoke()

                updateNotification(0)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }.start()
    }

    // ── Close session — mark absent students ───────────────────────────

    private fun closeSession() {
        Log.d("TIMER_SERVICE", "closeSession called — sectionId='$sectionId'")

        if (sectionId.isEmpty()) return
        Log.e("TIMER_SERVICE", "sectionId is empty — aborting closeSession")
        RetrofitClient.instance.closeSession(sectionId)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    val raw = response.body()?.string() ?: return
                    Log.d("TIMER_SERVICE", "closeSession response: $raw")

                    try {
                        val outer  = JSONObject(raw)
                        val body   = JSONObject(outer.optString("body", raw))
                        val absents = body.optInt("absents_marked", 0)
                        Log.d("TIMER_SERVICE", "Session closed — $absents student(s) marked absent")
                    } catch (e: Exception) {
                        Log.e("TIMER_SERVICE", "closeSession parse error: ${e.message}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("TIMER_SERVICE", "closeSession network error: ${t.message}")
                    // Not critical — absents can be resolved next time history loads
                }
            })
    }

    fun stopTimer() {
        countDownTimer?.cancel()
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Notifications ──────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Attendance Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active attendance session timer"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(secondsLeft: Int): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, InstructorAttendanceActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = if (secondsLeft == 0) "Session Ended" else "${formatTime(secondsLeft)} remaining"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Attendance Session Active")
            .setContentText("Code: $generatedCode  •  $timeText")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentIntent(openAppIntent)
            .setOngoing(secondsLeft > 0)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(secondsLeft: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(secondsLeft))
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}