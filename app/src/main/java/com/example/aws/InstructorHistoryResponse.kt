package com.example.aws

data class InstructorHistoryResponse(
    val records: List<InstructorRecord>
)

data class InstructorRecord(
    val student_id: String,
    val student_name: String,
    val status: String,
    val timestamp: String?
)
