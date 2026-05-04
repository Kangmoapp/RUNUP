package com.example.runup.data.source.remote.course

import com.example.runup.domain.model.Course
import retrofit2.Response
import retrofit2.http.*

// 🌟 [추가] 서버에서 보내는 코스와 AI 추천 사유를 한 번에 담을 바구니
data class AiCourseResponse(
    val course: Course,
    val reason: String
)

interface CourseApiService {
    // 코스 저장
    @POST("api/v1/courses")
    suspend fun saveCourse(@Body course: Course): Response<Boolean>

    // 주변 코스 소스(Source) 가져오기
    @GET("api/v1/courses/nearby")
    suspend fun getNearbyCourses(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double,
        @Query("sortType") sortType: String
    ): Response<List<Course>>

    // AI 기반 코스 추천 검색
    @POST("api/v1/courses/ai-search")
    suspend fun getCoursesFromAI(
        @Body request: Map<String, Any>
    ): Response<List<AiCourseResponse>> // 🌟 [수정] Map이 아니라 AiCourseResponse로 정확히 받습니다!
}