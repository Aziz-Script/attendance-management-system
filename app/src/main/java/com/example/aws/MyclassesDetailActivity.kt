package com.example.aws

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

class MyClassesDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.myclasses_detail_page)

        val courseCode = intent.getStringExtra("course_code")
        val courseName = intent.getStringExtra("course_name")
        val sectionId = intent.getStringExtra("section_id")

        findViewById<TextView>(R.id.courseCode).text = "$courseCode - $courseName"

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        // Take Attendance
        findViewById<Button>(R.id.takeAttendanceBtn).setOnClickListener {
            val intent = Intent(this, InstructorAttendanceActivity::class.java)
            intent.putExtra("section_id", sectionId)
            startActivity(intent)
        }

        // Attendance History
        findViewById<Button>(R.id.attendanceHistoryBtn).setOnClickListener {
            val intent = Intent(this, InstructorSessionsActivity::class.java)
            intent.putExtra("section_id", sectionId)
            startActivity(intent)
        }
    }
}

