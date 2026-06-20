package com.example.aws

data class StudentHistoryResponse(
    val items: List<StudentHistoryItem>
)

data class StudentHistoryItem(
    val date: String,
    val status: String
)
