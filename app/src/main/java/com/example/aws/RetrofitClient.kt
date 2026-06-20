package com.example.aws

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL =
        "https://tfjudkfbikoeqek-studentaiprojects.adb.me-jeddah-1.oraclecloudapps.com/ords/gp2user2/attendance_api/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://tfjudkfbikoeqek-studentaiprojects.adb.me-jeddah-1.oraclecloudapps.com/ords/gp2user2/attendance_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

            .create(ApiService::class.java)
    }
}
