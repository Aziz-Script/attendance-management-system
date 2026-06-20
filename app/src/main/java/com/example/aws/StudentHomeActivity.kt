package com.example.aws

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentHomeActivity : BaseActivity() {

    private var selectedCourse: StudentCourse? = null
    private var studentId: String = ""
    private var firstName: String = ""
    private var lastName: String = ""

    // Tracks the last session code we notified about — prevents repeat notifications
    private var lastNotifiedCode: String = ""

    // ── Polling every 15 seconds ───────────────────────────────────
    private val sessionCheckHandler = Handler(Looper.getMainLooper())
    private val sessionCheckRunnable = object : Runnable {
        override fun run() {
            if (studentId.isNotEmpty()) checkActiveSession()
            sessionCheckHandler.postDelayed(this, 5_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_home_page)

        firstName = intent.getStringExtra("first_name") ?: ""
        lastName  = intent.getStringExtra("last_name")  ?: ""
        studentId = intent.getStringExtra("student_id") ?: ""
        val gpa   = intent.getStringExtra("gpa")        ?: "N/A"
        val major = intent.getStringExtra("major")      ?: ""

        // Header
        findViewById<TextView>(R.id.student_name).text   = "$firstName $lastName"
        findViewById<TextView>(R.id.avatar_initial).text =
            firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"

        // Stat chips
        findViewById<TextView>(R.id.student_gpa).text        = gpa
        findViewById<TextView>(R.id.student_id).text         = studentId
        findViewById<TextView>(R.id.student_department).text = major.ifEmpty { "—" }

        // Settings
        findViewById<ImageView>(R.id.settings_button).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("first_name", firstName)
            intent.putExtra("last_name",  lastName)
            intent.putExtra("role",       "student")
            startActivityForResult(intent, 1)
        }

        // Action buttons
        val btnMark    = findViewById<MaterialButton>(R.id.btn_mark_attendance)
        val btnHistory = findViewById<MaterialButton>(R.id.btn_history)
        val hint       = findViewById<TextView>(R.id.select_hint)

        btnMark.setOnClickListener {
            selectedCourse?.let { course ->
                val intent = Intent(this, StudentAttendanceActivity::class.java)
                intent.putExtra("section_id",  course.section_id)
                intent.putExtra("course_code", course.course_code)
                intent.putExtra("course_name", course.course_name)
                startActivity(intent)
            }
        }

        btnHistory.setOnClickListener {
            selectedCourse?.let { course ->
                val intent = Intent(this, StudentHistoryActivity::class.java)
                intent.putExtra("section_id",  course.section_id)
                intent.putExtra("course_code", course.course_code)
                intent.putExtra("course_name", course.course_name)
                startActivity(intent)
            }
        }

        NotificationHelper.createChannels(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1002
                )
            }
        }

        loadCourses(btnMark, btnHistory, hint)
    }

    override fun onResume() {
        super.onResume()
        if (studentId.isNotEmpty()) checkActiveSession()
        sessionCheckHandler.postDelayed(sessionCheckRunnable, 15_000)
    }

    override fun onPause() {
        super.onPause()
        sessionCheckHandler.removeCallbacks(sessionCheckRunnable)
    }

    // ── Load courses ───────────────────────────────────────────────

    private fun loadCourses(
        btnMark: MaterialButton,
        btnHistory: MaterialButton,
        hint: TextView
    ) {
        val recycler = findViewById<RecyclerView>(R.id.coursesRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

        RetrofitClient.instance.getCourses(studentId)
            .enqueue(object : Callback<CoursesResponse> {
                override fun onResponse(
                    call: Call<CoursesResponse>,
                    response: Response<CoursesResponse>
                ) {
                    val courses = response.body()?.items ?: emptyList()
                    recycler.adapter = StudentCoursesAdapter(courses) { tapped ->
                        selectedCourse       = tapped
                        btnMark.isEnabled    = true
                        btnMark.alpha        = 1f
                        btnHistory.isEnabled = true
                        btnHistory.alpha     = 1f
                        hint.visibility      = View.GONE
                    }
                }

                override fun onFailure(call: Call<CoursesResponse>, t: Throwable) {
                    Toast.makeText(
                        this@StudentHomeActivity,
                        getString(R.string.toast_failed_courses),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // ── Active session check ───────────────────────────────────────

    private fun checkActiveSession() {
        RetrofitClient.instance.getActiveSession(studentId)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    val raw = response.body()?.string() ?: run {
                        Log.e("NOTIF", "Empty response from active_session")
                        return
                    }

                    Log.d("NOTIF", "active_session raw = $raw")

                    try {
                        val outer      = JSONObject(raw)
                        val bodyString = outer.optString("body", null) ?: raw
                        val body       = JSONObject(bodyString)
                        val active     = body.optBoolean("active", false)

                        Log.d("NOTIF", "active = $active")

                        // Respect notifications toggle
                        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
                        if (!prefs.getBoolean("notifications_enabled", true)) return

                        // Check permission
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                                != android.content.pm.PackageManager.PERMISSION_GRANTED) return
                        }

                        if (active) {
                            val code       = body.optString("code",        "")
                            val courseName = body.optString("course_name", "")
                            val courseCode = body.optString("course_code", "")
                            val sectionId  = body.optString("section_id",  "")

                            // Only fire once per unique session code
                            if (code == lastNotifiedCode) {
                                Log.d("NOTIF", "Already notified for code=$code — skipping")
                                return
                            }

                            // New session — update tracker and fire notification
                            lastNotifiedCode = code
                            Log.d("NOTIF", "Firing session notification — $courseCode code=$code")

                            NotificationHelper.showActiveSessionNotification(
                                context    = this@StudentHomeActivity,
                                courseName = courseName,
                                courseCode = courseCode,
                                code       = code,
                                sectionId  = sectionId
                            )

                        } else {
                            // Session ended — reset tracker and cancel notification
                            if (lastNotifiedCode.isNotEmpty()) {
                                Log.d("NOTIF", "Session ended — cancelling notification")
                                lastNotifiedCode = ""
                                NotificationHelper.cancel(
                                    this@StudentHomeActivity,
                                    NotificationHelper.NOTIF_ACTIVE_SESSION
                                )
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("NOTIF", "Parse error: ${e.message}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("NOTIF", "Session check failed: ${t.message}")
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) recreate()
    }
}