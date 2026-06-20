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

class InstructorHistoryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.instructor_history_page)

        val sectionId  = intent.getStringExtra("section_id")
        val courseCode = intent.getStringExtra("course_code") ?: ""
        val courseName = intent.getStringExtra("course_name") ?: ""
        val sessionId  = intent.getStringExtra("session_id")

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        if (courseCode.isNotEmpty()) {
            findViewById<TextView>(R.id.historyTitle).text = "$courseCode — $courseName"
        }

        if (sectionId == null) {
            Toast.makeText(this, getString(R.string.toast_missing_section), Toast.LENGTH_SHORT).show()
            return
        }

        loadHistory(sectionId, sessionId)
    }

    private fun loadHistory(sectionId: String, sessionId: String?) {
        Log.d("DEBUG", "loadHistory called — sectionId: $sectionId, sessionId: $sessionId")
        RetrofitClient.instance.getInstructorHistory(sectionId, sessionId?: "")
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    val raw = response.body()?.string() ?: run {
                        Toast.makeText(this@InstructorHistoryActivity, getString(R.string.toast_server_error), Toast.LENGTH_SHORT).show()
                        return
                    }

                    Log.d("INSTR_HISTORY", "raw = $raw")

                    try {
                        val outer   = JSONObject(raw)
                        val bodyStr = outer.getString("body")
                        val records = JSONObject(bodyStr).getJSONArray("records")

                        val emptyState  = findViewById<LinearLayout>(R.id.emptyState)
                        val listWrapper = findViewById<LinearLayout>(R.id.instructorHistoryList)
                        val recycler    = findViewById<RecyclerView>(R.id.historyRecycler)

                        if (records.length() == 0) {
                            emptyState.visibility  = View.VISIBLE
                            listWrapper.visibility = View.GONE
                            updateChips(0, 0, 0, 0)
                            return
                        }

                        // Build list and count statuses
                        data class HistoryRow(
                            val name: String,
                            val studentId: String,
                            val date: String,
                            val status: String
                        )

                        val rows = mutableListOf<HistoryRow>()
                        var present = 0; var late = 0; var absent = 0

                        for (i in 0 until records.length()) {
                            val rec       = records.getJSONObject(i)
                            val name      = rec.optString("student_name", "Unknown")
                            val studentId = rec.optString("student_id", "—")
                            val timestamp = rec.optString("timestamp", "")
                            val date      = if (timestamp.length >= 10) timestamp.substring(0, 10) else "—"
                            val status    = rec.optString("status", "-").uppercase()

                            rows.add(HistoryRow(name, studentId, date, status))

                            when (status) {
                                "P" -> present++
                                "L" -> late++
                                "A" -> absent++
                            }
                        }

                        // Total = all records in this session
                        val total = rows.size
                        updateChips(total, present, late, absent)

                        emptyState.visibility  = View.GONE
                        listWrapper.visibility = View.VISIBLE

                        recycler.layoutManager = LinearLayoutManager(this@InstructorHistoryActivity)
                        recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                            inner class RowHolder(v: View) : RecyclerView.ViewHolder(v) {
                                val name      = v.findViewById<TextView>(R.id.studentNameText)
                                val studentId = v.findViewById<TextView>(R.id.studentIdText)
                                val date      = v.findViewById<TextView>(R.id.dateText)
                                val status    = v.findViewById<TextView>(R.id.statusChip)
                            }

                            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                                val v = LayoutInflater.from(parent.context)
                                    .inflate(R.layout.item_instructor_history, parent, false)
                                return RowHolder(v)
                            }

                            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                                val row = rows[position]
                                val h   = holder as RowHolder
                                h.name.text      = row.name
                                h.studentId.text = row.studentId
                                h.date.text      = row.date
                                h.status.text    = row.status
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
                        Log.e("INSTR_HISTORY", "Parse error: ${e.message}")
                        Toast.makeText(
                            this@InstructorHistoryActivity,
                            getString(R.string.toast_invalid_server_response),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@InstructorHistoryActivity, getString(R.string.toast_network_error), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateChips(total: Int, present: Int, late: Int, absent: Int) {
        findViewById<TextView>(R.id.count_total).text   = total.toString()
        findViewById<TextView>(R.id.count_present).text = present.toString()
        findViewById<TextView>(R.id.count_late).text    = late.toString()
        findViewById<TextView>(R.id.count_absent).text  = absent.toString()
    }
}