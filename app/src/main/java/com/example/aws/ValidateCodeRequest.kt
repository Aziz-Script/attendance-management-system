package com.example.aws

import com.google.gson.annotations.SerializedName

data class ValidateCodeRequest(
    val section_id: String,
    val code: String,
    val student_id: String
)


