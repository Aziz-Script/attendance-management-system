package com.example.aws

import com.google.gson.annotations.SerializedName

data class ActivationCodeRequest(
    @SerializedName("SECTION_ID") val sectionId: String,
    @SerializedName("CODE") val code: String
)

