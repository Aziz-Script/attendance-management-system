package com.example.aws

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Header

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────
    @POST("login/")
    fun login(
        @Header("email")    email: String,
        @Header("id")       id: String,
        @Header("password") password: String
    ): Call<ResponseBody>

    // ── Student ───────────────────────────────────────────────────
    @GET("courses/")
    fun getCourses(
        @Query("student_id") studentId: String
    ): Call<CoursesResponse>

    @GET("courseDetail/")
    fun getCourseDetail(
        @Query("section_id") sectionId: String
    ): Call<CourseDetailResponse>

    @POST("attend/")
    fun attend(
        @Header("student_id") studentId: String,
        @Header("section_id") sectionId: String,
        @Header("code")       code: String
    ): Call<ResponseBody>

    // Now takes both student_id and section_id — shows history for one section only
    @GET("student_history")
    fun getStudentHistory(
        @Query("student_id") studentId: String,
        @Query("section_id") sectionId: String
    ): Call<ResponseBody>

    @GET("active_session/")
    fun getActiveSession(
        @Query("student_id") studentId: String
    ): Call<ResponseBody>

    // ── Instructor ────────────────────────────────────────────────
    @GET("instructor/")
    fun getInstructorCourses(
        @Query("id") instructorId: String
    ): Call<InstructorResponse>

    @POST("start/")
    fun startAttendanceSession(
        @Header("section_id") sectionId: String,
        @Header("minutes")    minutes: String
    ): Call<ResponseBody>

    @POST("close_session/")
    fun closeSession(
        @Header("section_id") sectionId: String
    ): Call<ResponseBody>

    @POST("cancel_session/")
    fun cancelSession(
        @Header("section_id") sectionId: String
    ): Call<ResponseBody>

    @GET("session_list/")
    fun getSessionList(
        @Query("section_id") sectionId: String
    ): Call<ResponseBody>

    @GET("instructor_history")
    fun getInstructorHistory(
        @Query("section_id") sectionId: String,
        @Header("session_id") sessionId: String
    ): Call<ResponseBody>
}