package com.example.aws
import com.google.gson.annotations.SerializedName


data class CourseItem(
    @SerializedName("course_name") val courseName: String,
    @SerializedName("course_code") val courseCode: String,
    @SerializedName("section_id") val sectionId: String
    
)

