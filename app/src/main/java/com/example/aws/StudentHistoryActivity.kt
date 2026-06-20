package com.example.aws

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentHistoryActivity : BaseActivity() {

    private fun getLoggedInStudentId(): String {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        return prefs.getString("student_id", "") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_attendance_page)

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        val sectionId  = intent.getStringExtra("section_id")  ?: ""
        val courseCode = intent.getStringExtra("course_code") ?: ""
        val courseName = intent.getStringExtra("course_name") ?: ""

        Log.d("STUDENT_HISTORY", "sectionId='$sectionId' studentId='${getLoggedInStudentId()}'")
        if (courseCode.isNotEmpty()) {
            findViewById<TextView>(R.id.courseCode).text = "$courseCode — $courseName"
        }

        val studentId = getLoggedInStudentId()
        if (studentId.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_student_id), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (sectionId.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_section), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadHistory(studentId, sectionId)
    }

    private fun loadHistory(studentId: String, sectionId: String) {
        Log.d("STUDENT_HISTORY", "Calling API — student=$studentId section=$sectionId")
        RetrofitClient.instance.getStudentHistory(studentId, sectionId)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (!response.isSuccessful || response.body() == null) {
                        Toast.makeText(this@StudentHistoryActivity, getString(R.string.toast_server_error), Toast.LENGTH_SHORT).show()
                        return
                    }

                    val raw = response.body()!!.string()
                    Log.d("STUDENT_HISTORY", "raw = $raw")

                    try {
                        val outer   = JSONObject(raw)
                        val bodyStr = outer.getString("body")
                        val items   = JSONObject(bodyStr).getJSONArray("records")

                        val emptyState   = findViewById<LinearLayout>(R.id.emptyState)
                        val recyclerView = findViewById<RecyclerView>(R.id.attendanceList)

                        if (items.length() == 0) {
                            emptyState.visibility   = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            updateChips(0, 0, 0)
                            return
                        }

                        data class HistoryRow(val date: String, val section: String, val status: String)
                        val rows = mutableListOf<HistoryRow>()
                        var present = 0; var late = 0; var absent = 0

                        for (i in 0 until items.length()) {
                            val item      = items.getJSONObject(i)
                            val timestamp = item.optString("timestamp", "")
                            val date      = if (timestamp.length >= 10) timestamp.substring(0, 10) else "—"
                            val section   = item.optString("section_id", "")
                            val status    = item.optString("status", "-").uppercase()

                            rows.add(HistoryRow(date, section, status))
                            when (status) {
                                "P" -> present++
                                "L" -> late++
                                "A" -> absent++
                            }
                        }

                        updateChips(present, late, absent)

                        emptyState.visibility   = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.layoutManager = LinearLayoutManager(this@StudentHistoryActivity)

                        recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                            inner class RowHolder(v: View) : RecyclerView.ViewHolder(v) {
                                val date    = v.findViewById<TextView>(R.id.dateText)
                                val section = v.findViewById<TextView>(R.id.sectionText)
                                val status  = v.findViewById<TextView>(R.id.statusText)
                            }

                            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                                val v = LayoutInflater.from(parent.context)
                                    .inflate(R.layout.student_row_template, parent, false)
                                return RowHolder(v)
                            }

                            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                                val row = rows[position]
                                val h   = holder as RowHolder
                                h.date.text    = row.date
                                h.section.text = row.section
                                h.status.text  = row.status
                                h.status.setBackgroundResource(
                                    when (row.status) {
                                        "P"  -> R.drawable.chip_present
                                        "L"  -> R.drawable.chip_late
                                        "A"  -> R.drawable.chip_absent
                                        else -> R.drawable.chip_pending
                                    }
                                )
                            }

                            override fun getItemCount() = rows.size
                        }

                    } catch (e: Exception) {
                        Log.e("STUDENT_HISTORY", "Parse error: ${e.message}")
                        Toast.makeText(this@StudentHistoryActivity, getString(R.string.toast_invalid_server_response), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("STUDENT_HISTORY", "Network failure: ${t.message}")
                    Toast.makeText(this@StudentHistoryActivity, getString(R.string.toast_network_error), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateChips(present: Int, late: Int, absent: Int) {
        findViewById<TextView>(R.id.count_present).text = present.toString()
        findViewById<TextView>(R.id.count_late).text    = late.toString()
        findViewById<TextView>(R.id.count_absent).text  = absent.toString()
    }
}