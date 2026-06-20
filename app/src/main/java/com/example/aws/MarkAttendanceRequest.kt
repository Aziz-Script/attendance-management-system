package com.example.aws

data class MarkAttendanceRequest(
    val student_id: String,
    val section_id: String
)
