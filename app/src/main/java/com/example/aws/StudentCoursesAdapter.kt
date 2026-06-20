package com.example.aws

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentCoursesAdapter(
    private val courses: List<StudentCourse>,
    private val onCourseClick: (StudentCourse) -> Unit
) : RecyclerView.Adapter<StudentCoursesAdapter.CourseViewHolder>() {

    private var selectedPosition = RecyclerView.NO_ID.toInt()

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseNameText: TextView = itemView.findViewById(R.id.courseNameText)
        val courseCodeText: TextView = itemView.findViewById(R.id.courseCodeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.course_item_student, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.courseNameText.text = course.course_name
        holder.courseCodeText.text = course.course_code

        // Highlight selected row
        holder.itemView.alpha = if (selectedPosition == position) 1f else 0.85f
        holder.itemView.setBackgroundResource(
            if (selectedPosition == position) R.drawable.course_row_selected
            else R.drawable.course_row_bg
        )

        holder.itemView.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onCourseClick(course)
        }
    }

    override fun getItemCount() = courses.size
}