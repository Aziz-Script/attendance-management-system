package com.example.aws

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InstructorAttendanceActivity : BaseActivity() {

    private lateinit var sectionId: String
    private var selectedMinutes: Int? = null
    private var sessionDurationSeconds: Int = 0

    private var timerService: AttendanceTimerService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val b = binder as AttendanceTimerService.TimerBinder
            timerService = b.getService()
            isBound = true
            timerService?.let { svc ->
                if (svc.isRunning) {
                    updateCodeDisplay(svc.generatedCode)
                    sessionDurationSeconds = svc.timeLeftSeconds
                    updateTimerUI(svc.timeLeftSeconds)
                    setSessionActiveUI()
                    hookCallbacks(svc)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
            timerService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.take_attendance_page)

        sectionId = intent.getStringExtra("section_id") ?: ""
        val courseCode = intent.getStringExtra("course_code") ?: ""
        val courseName = intent.getStringExtra("course_name") ?: ""

        Log.d("ATTEND_SESSION", "sectionId received = '$sectionId'")

        // Notification permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        val btnChoose   = findViewById<MaterialButton>(R.id.btnChooseTimer)
        val btnGenerate = findViewById<MaterialButton>(R.id.btnGenerateCode)

        btnGenerate.isEnabled = false
        btnGenerate.alpha     = 0.45f

        btnChoose.setOnClickListener { showTimerPickerDialog() }
        btnGenerate.setOnClickListener { selectedMinutes?.let { startAttendanceSession(it) } }

        // ── Slide to cancel ────────────────────────────────────────
        val slideView = findViewById<SlideToCancelView>(R.id.slideToCancelView)
        slideView.onCancelled = { confirmCancel() }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, AttendanceTimerService::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            timerService?.onTick   = null
            timerService?.onFinish = null
            unbindService(serviceConnection)
            isBound = false
        }
    }

    // ── Timer picker ───────────────────────────────────────────────

    private fun showTimerPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_timer_picker, null)
        val picker     = dialogView.findViewById<NumberPicker>(R.id.minutePicker)

        val durations = arrayOf("1", "3", "5", "10", "15")
        picker.minValue = 0
        picker.maxValue = durations.size - 1
        picker.displayedValues = durations

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                selectedMinutes = durations[picker.value].toInt()
                findViewById<TextView>(R.id.timerDisplay).text =
                    String.format("%02d:00", selectedMinutes!!)
                findViewById<MaterialButton>(R.id.btnGenerateCode).apply {
                    isEnabled = true
                    alpha     = 1f
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    // ── Start session ──────────────────────────────────────────────

    private fun startAttendanceSession(minutes: Int) {
        val btnGenerate = findViewById<MaterialButton>(R.id.btnGenerateCode)
        btnGenerate.isEnabled = false
        btnGenerate.alpha     = 0.45f
        btnGenerate.text      = getString(R.string.msg_starting)

        Log.d("ATTEND_SESSION", "Starting session — sectionId='$sectionId' minutes=$minutes")

        RetrofitClient.instance.startAttendanceSession(sectionId, minutes.toString())
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    val source = response.body()?.string()
                        ?: response.errorBody()?.string()
                        ?: run {
                            Toast.makeText(this@InstructorAttendanceActivity,
                                getString(R.string.toast_empty_response), Toast.LENGTH_SHORT).show()
                            resetGenerateButton()
                            return
                        }

                    Log.d("ATTEND_SESSION", "raw = $source")

                    try {
                        val outer    = JSONObject(source)
                        val bodyStr  = outer.optString("body", null)
                        val bodyJson = if (!bodyStr.isNullOrEmpty()) JSONObject(bodyStr) else outer

                        if (bodyJson.has("error")) {
                            Toast.makeText(this@InstructorAttendanceActivity,
                                bodyJson.getString("error"), Toast.LENGTH_SHORT).show()
                            resetGenerateButton()
                            return
                        }

                        val code = bodyJson.optString("code", "").ifEmpty {
                            Toast.makeText(this@InstructorAttendanceActivity,
                                getString(R.string.toast_no_code_returned), Toast.LENGTH_SHORT).show()
                            resetGenerateButton()
                            return
                        }

                        Log.d("ATTEND_SESSION", "Code = $code, starting service")

                        updateCodeDisplay(code)
                        Toast.makeText(this@InstructorAttendanceActivity,
                            getString(R.string.toast_session_started), Toast.LENGTH_SHORT).show()

                        val diffSeconds = minutes * 60
                        sessionDurationSeconds = diffSeconds

                        val serviceIntent = Intent(
                            this@InstructorAttendanceActivity,
                            AttendanceTimerService::class.java
                        ).apply {
                            putExtra(AttendanceTimerService.EXTRA_SECONDS, diffSeconds)
                            putExtra(AttendanceTimerService.EXTRA_CODE,    code)
                            putExtra(AttendanceTimerService.EXTRA_SECTION, sectionId)
                        }
                        startService(serviceIntent)
                        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

                        findViewById<View>(android.R.id.content).postDelayed({
                            timerService?.let { hookCallbacks(it) }
                        }, 200)

                        setSessionActiveUI()

                    } catch (e: Exception) {
                        Log.e("ATTEND_SESSION", "Parse error: ${e.message}")
                        Toast.makeText(this@InstructorAttendanceActivity,
                            getString(R.string.toast_unexpected_response), Toast.LENGTH_SHORT).show()
                        resetGenerateButton()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("ATTEND_SESSION", "Network failure: ${t.message}")
                    Toast.makeText(this@InstructorAttendanceActivity,
                        getString(R.string.toast_network_error), Toast.LENGTH_SHORT).show()
                    resetGenerateButton()
                }
            })
    }

    // ── Cancel session ─────────────────────────────────────────────

    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_cancel_session_title))
            .setMessage(getString(R.string.dialog_cancel_session_message))
            .setPositiveButton(getString(R.string.dialog_btn_cancel_session)) { _, _ -> cancelSession() }
            .setNegativeButton(getString(R.string.dialog_btn_keep_session), null)
            .show()
    }

    private fun cancelSession() {
        RetrofitClient.instance.cancelSession(sectionId)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    Log.d("ATTEND_SESSION", "Session cancelled")
                    Toast.makeText(this@InstructorAttendanceActivity,
                        getString(R.string.toast_session_cancelled), Toast.LENGTH_SHORT).show()

                    // Stop the timer service
                    timerService?.stopTimer()

                    // Reset UI completely
                    resetToIdle()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("ATTEND_SESSION", "Cancel failed: ${t.message}")
                    Toast.makeText(this@InstructorAttendanceActivity,
                        getString(R.string.toast_network_error), Toast.LENGTH_SHORT).show()

                    // Reset slide view even if server call failed
                    findViewById<SlideToCancelView>(R.id.slideToCancelView).reset()
                }
            })
    }

    // ── UI helpers ─────────────────────────────────────────────────

    private fun updateCodeDisplay(code: String) {
        val spaced = code.toCharArray().joinToString(" ")
        findViewById<TextView>(R.id.generatedCode).text = spaced
    }

    private fun updateTimerUI(secondsLeft: Int) {
        val m = secondsLeft / 60
        val s = secondsLeft % 60
        findViewById<TextView>(R.id.timerDisplay).text = String.format("%02d:%02d", m, s)
    }

    private fun setSessionActiveUI() {
        // Disable both buttons
        findViewById<MaterialButton>(R.id.btnGenerateCode).apply {
            isEnabled = false; alpha = 0.45f; text = getString(R.string.btn_generate_code)
        }
        findViewById<MaterialButton>(R.id.btnChooseTimer).apply {
            isEnabled = false; alpha = 0.45f
        }
        // Show slide to cancel
        findViewById<SlideToCancelView>(R.id.slideToCancelView).visibility = View.VISIBLE
    }

    private fun onSessionEnded() {
        Log.d("ATTEND_SESSION", "Session ended — UI reset")
        resetToIdle()
    }

    private fun resetToIdle() {
        // Reset code display
        findViewById<TextView>(R.id.generatedCode).text = "- - - - -"
        // Reset timer display
        findViewById<TextView>(R.id.timerDisplay).text  = getString(R.string.text_no_timer)
        // Re-enable choose duration, disable generate
        findViewById<MaterialButton>(R.id.btnChooseTimer).apply {
            isEnabled = true; alpha = 1f
        }
        findViewById<MaterialButton>(R.id.btnGenerateCode).apply {
            isEnabled = false; alpha = 0.45f; text = getString(R.string.btn_generate_code)
        }
        // Hide and reset slide view
        val slideView = findViewById<SlideToCancelView>(R.id.slideToCancelView)
        slideView.reset()
        slideView.visibility = View.GONE

        selectedMinutes = null
    }

    private fun resetGenerateButton() {
        findViewById<MaterialButton>(R.id.btnGenerateCode).apply {
            isEnabled = true; alpha = 1f; text = getString(R.string.btn_generate_code)
        }
    }

    private fun hookCallbacks(svc: AttendanceTimerService) {
        svc.onTick   = { s -> runOnUiThread { updateTimerUI(s) } }
        svc.onFinish = { runOnUiThread { onSessionEnded() } }
    }
}