package com.example.aws

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class CoursesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.courses_page)



        val backButton = findViewById<View>(R.id.backButton)
        val recycler = findViewById<RecyclerView>(R.id.coursesRecycler)

        val studentId = intent.getStringExtra("student_id")!!

        backButton.setOnClickListener { finish() }

        recycler.layoutManager = LinearLayoutManager(this)

        RetrofitClient.instance.getCourses(studentId).enqueue(object : retrofit2.Callback<CoursesResponse> {

            override fun onResponse(
                call: retrofit2.Call<CoursesResponse>,
                response: retrofit2.Response<CoursesResponse>

            ) {

                if (!response.isSuccessful) {
                    Toast.makeText(this@CoursesActivity, getString(R.string.toast_server_error), Toast.LENGTH_SHORT).show()
                    return
                }

                val courses = response.body()?.items ?: emptyList()



                recycler.adapter = StudentCoursesAdapter(courses) { selectedCourse ->
                    val intent = Intent (this@CoursesActivity, StudentCourseDetailActivity::class.java)
                    intent.putExtra("section_id", selectedCourse.section_id)
                    intent.putExtra("course_code", selectedCourse.course_code)
                    intent.putExtra("course_name", selectedCourse.course_name)
                    startActivity(intent)

                }

            }

            override fun onFailure(call: retrofit2.Call<CoursesResponse>, t: Throwable) {

            }
        })
    }
}
