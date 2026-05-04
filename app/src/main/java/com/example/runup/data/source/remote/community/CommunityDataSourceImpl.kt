package com.example.runup.data.source.remote.community

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.example.runup.data.source.local.SessionManager // 🌟 필수 임포트
import com.example.runup.domain.dto.toDomain
import com.example.runup.domain.model.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class CommunityDataSourceImpl @Inject constructor(
    private val apiService: CommunityApiService,
    private val gson: Gson,
    private val sessionManager: SessionManager, // 🌟 [에러 해결 1] SessionManager 주입 추가
    @ApplicationContext private val context: Context
) {
    // 1. 게시글 목록 페이징 조회
    // 1. 게시글 목록 페이징 조회
    suspend fun getPosts(
        lastPostId: String? = null,
        limit: Long = 3,
        filter: FilterState,
        friendIds: List<String> = emptyList()
    ): AuthResult<Pair<List<Post>, String?>> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.getPosts(
                uid = uid,
                lastPostId = lastPostId,
                limit = limit,
                scope = filter.scope.name,
                city = filter.city,
                district = filter.district,
                dong = filter.dong,
                friendIds = friendIds
            )
            if (response.isSuccessful) {
                val responseBody = response.body() ?: emptyList()

                // 🌟 [에러 해결] PostResponse 리스트를 Post 리스트로 변환
                val domainPosts = responseBody.map { it.toDomain() }

                AuthResult.Success(Pair(domainPosts, domainPosts.lastOrNull()?.postId))
            } else {
                AuthResult.Fail("게시글 로드 실패")
            }
        } catch (e: Exception) {
            AuthResult.Fail("게시글 로드 에러: ${e.message}", e)
        }
    }

    // 2. 유저별 탭 게시글 조회
    // 2. 유저별 탭 게시글 조회
    suspend fun getTargetUserPosts(
        targetUid: String,
        tabType: String,
        filter: FilterState, // 🌟 필터 정보를 이미 인자로 받고 있습니다.
        lastPostId: String? = null,
        limit: Long = 4L
    ): AuthResult<Pair<List<Post>, String?>>{
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.getTargetUserPosts(
                uid = uid,
                targetUid = targetUid,
                tabType = tabType,
                lastPostId = lastPostId,
                limit = limit,
                // 🚨 [수정] 아래 3줄이 빠져있어서 필터가 안 된 것입니다!
                city = filter.city,
                district = filter.district,
                dong = filter.dong
            )
            if (response.isSuccessful) {
                val responseBody = response.body() ?: emptyList()
                val domainPosts = responseBody.map { it.toDomain() }
                AuthResult.Success(Pair(domainPosts, domainPosts.lastOrNull()?.postId))
            } else {
                AuthResult.Fail("게시글 로드 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            // 🚨 [진실의 약] 파싱 에러의 진짜 범인을 안드로이드 스튜디오 Logcat에 빨간 글씨로 찍어줍니다!
            Log.e("CommunityData", "데이터 파싱 에러의 진짜 범인: ${e.message}", e)
            AuthResult.Fail("데이터 로드 실패", e)
        }
    }

    // 3. 단건 조회
    suspend fun getPostById(postId: String): AuthResult<Post> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.getPostById(uid = uid, postId = postId)
            if (response.isSuccessful && response.body() != null) {
                // 🌟 [수정] 단건도 toDomain()으로 변환해서 반환
                AuthResult.Success(response.body()!!.toDomain())
            } else AuthResult.Fail("게시물 없음")
        } catch (e: Exception) {
            AuthResult.Fail("로드 실패", e)
        }
    }

    // 4. 좋아요
    suspend fun toggleLike(postId: String): AuthResult<Boolean> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.toggleLike(uid = uid, postId = postId)
            if (response.isSuccessful) AuthResult.Success(response.body() ?: false)
            else AuthResult.Fail("좋아요 처리 실패")
        } catch (e: Exception) {
            AuthResult.Fail("서버 통신 에러", e)
        }
    }

    // 5. 댓글 달기
    suspend fun addComment(postId: String, content: String): AuthResult<Boolean> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.addComment(uid = uid, postId = postId, request = mapOf("content" to content))
            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("댓글 등록 실패")
        } catch (e: Exception) {
            AuthResult.Fail("댓글 통신 에러", e)
        }
    }

    // 6. 댓글 삭제
    suspend fun deleteComment(postId: String, commentId: String): AuthResult<Boolean> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.deleteComment(uid = uid, postId = postId, commentId = commentId)
            AuthResult.Success(response.isSuccessful)
        } catch (e: Exception) {
            AuthResult.Fail("댓글 삭제 실패", e)
        }
    }

    // 실시간 댓글 Flow (조회용이라 헤더 제외)
    fun getCommentsFlow(postId: String): Flow<List<Comment>> = flow {
        try {
            val response = apiService.getComments(postId = postId)
            val dtos = response.body() ?: emptyList()

            // 🌟 서버에서 받은 DTO 리스트를 도메인 모델 리스트로 싹 변환해서 보냅니다!
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    // 7. 업로드
    suspend fun uploadPost(
        content: String,
        locationImageUris: List<Uri>,
        commonImageUris: List<Uri>,
        runRecord: RunRecord?,
        address: AddressModel?
    ): AuthResult<Boolean> = coroutineScope {
        try {
            val uid = sessionManager.getUid()

            val locationParts = locationImageUris.mapIndexed { index, uri ->
                val bytes = compressImage(uri, 80) ?: return@coroutineScope AuthResult.Fail("이미지 압축 실패")
                MultipartBody.Part.createFormData(
                    "locationImages", "loc_$index.jpg", bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
            }

            val commonParts = commonImageUris.mapIndexed { index, uri ->
                val bytes = compressImage(uri, 80) ?: return@coroutineScope AuthResult.Fail("이미지 압축 실패")
                MultipartBody.Part.createFormData(
                    "commonImages", "com_$index.jpg", bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
            }

            val postDataMap = mapOf(
                "content" to content,
                // 🌟 [에러 해결 3] runRecord.id 가 아니라 runRecord.course.id 입니다!
                "runRecord" to if (runRecord != null) mapOf("recordId" to runRecord.course.id.toLongOrNull()) else null,
                "city" to (address?.city ?: ""),
                "district" to (address?.district ?: ""),
                "dong" to (address?.dong ?: "")
            )
            val postDataBody = gson.toJson(postDataMap).toRequestBody("application/json".toMediaTypeOrNull())

            val response = apiService.uploadPost(
                uid = uid,
                locationImages = locationParts,
                commonImages = commonParts,
                postData = postDataBody
            )

            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("업로드 실패: ${response.code()}")
        } catch (e: Exception) {
            AuthResult.Fail("업로드 중 오류 발생", e)
        }
    }

    // 8. 게시글 삭제
    suspend fun deletePost(post: Post): AuthResult<Boolean> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.deletePost(uid = uid, postId = post.postId)
            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("삭제 실패")
        } catch (e: Exception) {
            AuthResult.Fail("삭제 중 서버 에러", e)
        }
    }

    // 9. 따라 뛰기
    suspend fun followRunning(postId: String): AuthResult<Boolean> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.followRunning(uid = uid, postId = postId)
            AuthResult.Success(response.isSuccessful)
        } catch (e: Exception) {
            AuthResult.Fail("팔로우 실패", e)
        }
    }

    // --- Helper Methods ---
    private fun compressImage(uri: Uri, quality: Int): ByteArray? {
        // 🌟 .use { } 를 사용하면 작업이 끝난 후 자동으로 close() 됩니다!
        val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        } ?: return null

        val rotatedBitmap = rotateImageIfRequired(bitmap, uri)
        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap {
        // 🌟 여기서도 .use { } 적용
        val orientation = context.contentResolver.openInputStream(selectedImage)?.use { input ->
            val ei = ExifInterface(input)
            ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true).also { img.recycle() }
    }

    // 🌟 [추가] 회원탈퇴 시 내 커뮤니티 데이터 삭제
    // MySQL의 CASCADE 설정 덕분에 Spring 서버에서 유저를 지우면 게시글/댓글/좋아요가 자동 삭제됩니다!
    // 따라서 안드로이드에서 파이어베이스처럼 일일이 지울 필요 없이 무조건 Success를 반환합니다.
    suspend fun deleteAllMyCommunityData(): AuthResult<Boolean> {
        Log.d("CommunityData", "서버 DB에서 커뮤니티 데이터가 연쇄 삭제(CASCADE) 되었습니다.")
        return AuthResult.Success(true)
    }

    // 🌟 [추가] 상호작용(좋아요 등) 찌꺼기 정리
    // 이 역시 서버 DB에서 자동으로 지워주므로 내용은 비워둡니다.
    private suspend fun cleanupUserInteractions() {
        Log.d("CommunityData", "상호작용 데이터 정리 완료 (서버 위임)")
    }
}