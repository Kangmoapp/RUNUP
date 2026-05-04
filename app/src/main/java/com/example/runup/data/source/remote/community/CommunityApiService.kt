// CommunityApiService.kt
package com.example.runup.data.source.remote.community

import com.example.runup.domain.dto.CommentResponseDto
import com.example.runup.domain.dto.PostResponse
import com.example.runup.domain.model.Comment
import com.example.runup.domain.model.Post
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface CommunityApiService {
    @GET("api/v1/posts")
    suspend fun getPosts(
        @Header("X-USER-UID") uid: String,
        @Query("lastPostId") lastPostId: String?,
        @Query("limit") limit: Long,
        @Query("scope") scope: String,
        @Query("city") city: String,
        @Query("district") district: String,
        @Query("dong") dong: String,
        @Query("friendIds") friendIds: List<String>
    ): Response<List<PostResponse>> // 🌟 [수정] Post -> PostResponse
    @GET("api/v1/posts/user/{targetUid}")
    suspend fun getTargetUserPosts(
        @Header("X-USER-UID") uid: String,
        @Path("targetUid") targetUid: String,
        @Query("tabType") tabType: String,
        @Query("lastPostId") lastPostId: String?,
        @Query("limit") limit: Long,
        // 🌟 아래 3줄을 꼭 추가해 주세요!
        @Query("city") city: String,
        @Query("district") district: String,
        @Query("dong") dong: String
    ): Response<List<PostResponse>>

    @GET("api/v1/posts/{postId}")
    suspend fun getPostById(
        @Header("X-USER-UID") uid: String,
        @Path("postId") postId: String
    ): Response<PostResponse> // 🌟 [수정] 단건 조회도 PostResponse

    @POST("api/v1/posts/{postId}/like")
    suspend fun toggleLike(
        @Header("X-USER-UID") uid: String, // 🌟 필수
        @Path("postId") postId: String
    ): Response<Boolean>

    @POST("api/v1/posts/{postId}/comments")
    suspend fun addComment(
        @Header("X-USER-UID") uid: String, // 🌟 필수
        @Path("postId") postId: String,
        @Body request: Map<String, String>
    ): Response<Boolean>

    @DELETE("api/v1/posts/{postId}/comments/{commentId}")
    suspend fun deleteComment(
        @Header("X-USER-UID") uid: String, // 🌟 필수
        @Path("postId") postId: String,
        @Path("commentId") commentId: String
    ): Response<Boolean>

    @GET("api/v1/posts/{postId}/comments")
    suspend fun getComments(@Path("postId") postId: String): Response<List<CommentResponseDto>>
    // 🌟 Comment -> CommentResponseDto 로 변경!
    @Multipart
    @POST("api/v1/posts")
    suspend fun uploadPost(
        @Header("X-USER-UID") uid: String, // 🌟 필수
        @Part locationImages: List<MultipartBody.Part>,
        @Part commonImages: List<MultipartBody.Part>,
        @Part("postData") postData: RequestBody
    ): Response<Boolean>

    @DELETE("api/v1/posts/{postId}")
    suspend fun deletePost(
        @Header("X-USER-UID") uid: String, // 🌟 필수
        @Path("postId") postId: String
    ): Response<Boolean>

    @POST("api/v1/posts/{postId}/follow")
    suspend fun followRunning(
        @Header("X-USER-UID") uid: String, // 🌟 필수
        @Path("postId") postId: String
    ): Response<Boolean>
}