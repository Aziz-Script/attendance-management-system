package com.example.aws

data class User(
    val id: String,
    val first_name: String,
    val last_name: String,
    val email: String,
    val password: String,
    val role: String,
    val department: String?,
    val major: String?,
    val study_year: Int?,
    val gpa: Double?
)
