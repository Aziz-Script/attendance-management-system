package com.example.aws

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InstructorSessionsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructor_sessions)

        val sectionId = intent.getStringExtra("section_id")

        Log.d("DEBUG", "InstructorSessionsActivity launched, section_id: ${intent.getStringExtra("section_id")}")

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        if (sectionId == null) {
            Toast.makeText(this, getString(R.string.toast_missing_section), Toast.LENGTH_SHORT).show()
            return
        }

        loadSessions(sectionId)
    }

    private fun loadSessions(sectionId: String) {
        RetrofitClient.instance.getSessionList(sectionId)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    val raw = response.body()?.string() ?: run {
                        Toast.makeText(this@InstructorSessionsActivity, getString(R.string.toast_server_error), Toast.LENGTH_SHORT).show()
                        return
                    }

                    Log.d("DEBUG", "Raw response: $raw")

                    try {
                        val outer = JSONObject(raw)
                        val bodyStr = outer.getString("body")
                        val sessionsArr = JSONObject(bodyStr).getJSONArray("sessions")

                        Log.d("DEBUG", "Sessions count: ${sessionsArr.length()}")

                        val emptyState = findViewById<TextView>(R.id.emptyState)
                        val recycler   = findViewById<RecyclerView>(R.id.historyRecycler)

                        if (sessionsArr.length() == 0) {
                            emptyState.visibility = View.VISIBLE
                            recycler.visibility   = View.GONE
                            return
                        }

                        data class SessionRow(
                            val sessionId: String,
                            val sessionDate: String,
                            val sessionTime: String,
                            val present: Int,
                            val late: Int,
                            val absent: Int
                        )

                        val rows = (0 until sessionsArr.length()).map { i ->
                            val obj = sessionsArr.getJSONObject(i)
                            SessionRow(
                                sessionId   = obj.optString("session_id", ""),
                                sessionDate = obj.optString("session_date", "—"),
                                sessionTime = obj.optString("session_time", ""),
                                present     = obj.optInt("present", 0),
                                late        = obj.optInt("late", 0),
                                absent      = obj.optInt("absent", 0)
                            )
                        }

                        emptyState.visibility = View.GONE
                        recycler.visibility   = View.VISIBLE

                        recycler.layoutManager = LinearLayoutManager(this@InstructorSessionsActivity)
                        recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                            inner class SessionHolder(v: View) : RecyclerView.ViewHolder(v) {
                                val dateTime = v.findViewById<TextView>(R.id.sessionDateTime)
                                val present  = v.findViewById<TextView>(R.id.presentCount)
                                val late     = v.findViewById<TextView>(R.id.lateCount)
                                val absent   = v.findViewById<TextView>(R.id.absentCount)
                            }

                            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                                val v = android.view.LayoutInflater.from(parent.context)
                                    .inflate(R.layout.item_session_row, parent, false)
                                return SessionHolder(v)
                            }

                            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                                val h   = holder as SessionHolder
                                val row = rows[position]
                                h.dateTime.text = "${row.sessionDate}  ${row.sessionTime}".trim()
                                h.present.text  = "Present: ${row.present}"
                                h.late.text     = "Late: ${row.late}"
                                h.absent.text   = "Absent: ${row.absent}"
                                h.itemView.setOnClickListener {
                                    Log.d("DEBUG", "Session tapped — session_id: ${row.sessionId}")
                                    startActivity(
                                        Intent(this@InstructorSessionsActivity, InstructorHistoryActivity::class.java)
                                            .putExtra("section_id", sectionId)
                                            .putExtra("session_id", row.sessionId)
                                    )
                                }
                            }

                            override fun getItemCount() = rows.size
                        }

                    } catch (e: Exception) {
                        Log.e("INSTR_SESSIONS", "Parse error: ${e.message}")
                        Toast.makeText(
                            this@InstructorSessionsActivity,
                            getString(R.string.toast_invalid_server_response),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@InstructorSessionsActivity, getString(R.string.toast_network_error), Toast.LENGTH_SHORT).show()
                }
            })
    }
}
