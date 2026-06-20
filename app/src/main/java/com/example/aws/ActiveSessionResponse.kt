package com.example.aws

data class ActiveSessionResponse(
    val active: Boolean,
    val section_id: String? = null,
    val code: String? = null,
    val course_name: String? = null,
    val course_code: String? = null
)