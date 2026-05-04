package com.example.runup.data.source.remote.user

import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.RunRecordResponse
import com.example.runup.domain.model.UserData
import com.example.runup.domain.model.UserActivityStats
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    @GET("api/v1/user/check-email")
    suspend fun checkEmail(@Query("email") email: String): Response<Boolean>

    @POST("api/v1/user/register")
    suspend fun registerUser(@Body request: Map<String, String>): Response<Boolean>

    @POST("api/v1/user/login")
    suspend fun loginUser(@Body request: Map<String, String>): Response<Map<String, String>> // 🔹 변경

    @DELETE("api/v1/user/account")
    suspend fun deleteUserAccount(@Query("password") password: String): Response<Boolean>

    @PATCH("api/v1/user/name")
    suspend fun updateUserName(@Query("name") name: String): Response<Boolean>

    @Multipart
    @POST("api/v1/user/profile-image")
    suspend fun uploadProfileImage(@Part image: MultipartBody.Part): Response<String>

    @PATCH("api/v1/user/goal")
    suspend fun updateUserGoal(
        @Header("X-USER-UID") uid: String, // 내 신분증 필수!
        @Query("distance") distance: Int,  // 미터(m) 단위
        @Query("time") time: Int           // 초(s) 단위
    ): Response<Boolean>

    @GET("api/v1/user/goal")
    suspend fun getUserGoal(): Response<Map<String, Int>>


    @DELETE("api/v1/runs/{courseId}")
    suspend fun deleteRunRecord(
        @Header("X-USER-UID") uid: String, // 🌟 [핵심] 서버가 타령하던 바로 그 신분증입니다!
        @Path("courseId") courseId: String
    ): Response<Boolean>
    // 🔹 이 부분의 이름을 확인하세요 (getMyUserData)
    @GET("api/v1/user/me")
    suspend fun getMyUserData(
        @Header("X-USER-UID") uid: String
    ): Response<UserData>

    // 🌟 [에러 157, 160 해결] uid와 pageSize 파라미터 이름 확인
    // UserApiService.kt
    @GET("/api/v1/runs")
    suspend fun getRunsPaged(
        @Header("X-USER-UID") uid: String,
        @Query("filter") filter: String,
        @Query("lastDate") lastDate: Long?,
        @Query("limit") pageSize: Long // 🌟 여기 이름을 pageSize로 맞춰야 160번 에러가 해결됩니다.
    ): Response<List<RunRecordResponse>>

    @GET("api/v1/user/activity-stats/{uid}")
    suspend fun getUserActivityStats(@Path("uid") uid: String): Response<UserActivityStats>


    @POST("api/v1/friends/request/{targetUid}")
    suspend fun sendFriendRequest(@Path("targetUid") targetUid: String): Response<Boolean>

    @POST("api/v1/friends/accept/{targetUid}")
    suspend fun acceptFriendRequest(@Path("targetUid") targetUid: String): Response<Boolean>

    @POST("api/v1/friends/decline/{targetUid}")
    suspend fun declineFriendRequest(@Path("targetUid") targetUid: String): Response<Boolean>

    @HTTP(method = "DELETE", path = "api/v1/friends/{targetUid}")
    suspend fun deleteFriend(@Path("targetUid") targetUid: String): Response<Boolean>

    @GET("api/v1/friends/uids")
    suspend fun getFriendUids(): Response<List<String>>
    @POST("api/v1/user/summary")
    suspend fun getUsersSummary(@Body uidList: List<String>): Response<List<UserData>>
    @POST("api/v1/user/google")
    suspend fun signInWithGoogle(@Body request: Map<String, String>): Response<Map<String, String>> // 🔹 변경


    @POST("api/runs")
    suspend fun saveRunRecord(
        @Header("X-USER-UID") uid: String,
        @Body record: RunRecord
    ): Response<Boolean>
    // UserApiService.kt (확인 및 수정)
    @GET("/api/v1/runs")
    suspend fun getMyRuns(
        @Header("X-USER-UID") uid: String, // 🌟 이 줄이 반드시 있어야 함!
        @Query("filter") filter: String,
        @Query("lastDate") lastDate: Long?,
        @Query("limit") limit: Long
    ): Response<List<RunRecordResponse>> // 👈 Response DTO 타입 확인


    // 기존 UserApiService.kt 파일에 아래 내용들 추가
// 🌟 [수정 완료] 기존에 'users' 였던 주소들을 모두 'user'로 통일했습니다!
    @POST("api/v1/user/friends/request")
    suspend fun requestFriend(@Header("X-USER-UID") uid: String, @Query("targetEmail") targetEmail: String): retrofit2.Response<Boolean>

    @POST("api/v1/user/friends/accept")
    suspend fun acceptFriend(@Header("X-USER-UID") uid: String, @Query("requesterUid") requesterUid: String): retrofit2.Response<Boolean>

    @GET("api/v1/user/friends/pending")
    suspend fun getPendingRequests(@Header("X-USER-UID") uid: String): retrofit2.Response<List<Map<String, String>>>

    @GET("api/v1/user/friends")
    suspend fun getFriends(@Header("X-USER-UID") uid: String): retrofit2.Response<List<Map<String, String>>>

    @GET("api/v1/user/search")
    suspend fun searchUserByEmail(@Query("email") email: String): retrofit2.Response<Map<String, String>>

    @DELETE("api/v1/user/friends/{targetUid}")
    suspend fun deleteFriend(@Header("X-USER-UID") uid: String, @Path("targetUid") targetUid: String): retrofit2.Response<Boolean>
}