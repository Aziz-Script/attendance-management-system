package com.example.aws

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aws.activities.InstructorClassesActivity
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InstructorHomeActivity : BaseActivity() {

    private var selectedCourse: CourseItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.instructor_home_page)

        val instructorId = intent.getStringExtra("instructor_id") ?: ""
        val firstName    = intent.getStringExtra("first_name")    ?: ""
        val lastName     = intent.getStringExtra("last_name")     ?: ""
        val department   = intent.getStringExtra("department")    ?: ""

        // Header
        findViewById<TextView>(R.id.instructor_name).text   = "$firstName $lastName"
        findViewById<TextView>(R.id.avatar_initial).text    = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "I"

        // Stat chips
        findViewById<TextView>(R.id.instructor_id).text         = instructorId
        findViewById<TextView>(R.id.instructor_department).text = department.ifEmpty { "—" }

        // Settings

        // Instructor
        findViewById<ImageView>(R.id.settings_button).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("first_name", firstName)
            intent.putExtra("last_name",  lastName)
            intent.putExtra("role",       "instructor")
            startActivityForResult(intent, 1)
        }

        // Buttons — disabled until a class is tapped
        val btnTake    = findViewById<MaterialButton>(R.id.btn_take_attendance)
        val btnHistory = findViewById<MaterialButton>(R.id.btn_history)
        val hint       = findViewById<TextView>(R.id.select_hint)

        btnTake.setOnClickListener {
            selectedCourse?.let { course ->
                val intent = Intent(this, InstructorAttendanceActivity::class.java)
                intent.putExtra("section_id",  course.sectionId)
                intent.putExtra("course_code", course.courseCode)
                intent.putExtra("course_name", course.courseName)
                startActivity(intent)
            }
        }

        btnHistory.setOnClickListener {
            selectedCourse?.let { course ->
                val intent = Intent(this, InstructorSessionsActivity::class.java)
                intent.putExtra("section_id", course.sectionId)
                startActivity(intent)
            }
        }

        // Load classes into the recycler
        val recycler = findViewById<RecyclerView>(R.id.coursesRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

        RetrofitClient.instance.getInstructorCourses(instructorId)
            .enqueue(object : Callback<InstructorResponse> {

                override fun onResponse(
                    call: Call<InstructorResponse>,
                    response: Response<InstructorResponse>
                ) {
                    val courses = response.body()?.items ?: emptyList()

                    recycler.adapter = InstructorCoursesAdapter(courses) { tapped ->
                        // Update selection
                        selectedCourse = tapped

                        // Enable buttons and hide hint
                        btnTake.isEnabled    = true
                        btnTake.alpha        = 1f
                        btnHistory.isEnabled = true
                        btnHistory.alpha     = 1f
                        hint.visibility      = View.GONE
                    }
                }

                override fun onFailure(call: Call<InstructorResponse>, t: Throwable) {
                    Toast.makeText(
                        this@InstructorHomeActivity,
                        getString(R.string.toast_failed_classes),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) recreate()
    }
}