package com.example.runup.data.source.remote.user

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.example.runup.data.source.local.SessionManager
import com.example.runup.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class UserDataSourceImpl @Inject constructor(
    private val apiService: UserApiService,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : UserDataSource {

    override suspend fun isEmailAlreadyRegistered(email: String): AuthResult<Boolean> {
        return try {
            val response = apiService.checkEmail(email)
            if (response.isSuccessful && response.body() == false) {
                AuthResult.Success(true)
            } else {
                AuthResult.Fail("이미 등록된 이메일입니다.")
            }
        } catch (e: Exception) {
            AuthResult.Fail("네트워크 오류가 발생했습니다.", e)
        }
    }

    override suspend fun registerUser(email: String, pw: String): AuthResult<Boolean> {
        return try {
            val request = mapOf("userEmail" to email, "userPassword" to pw)
            val response = apiService.registerUser(request)
            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("회원가입 실패: ${response.message()}")
        } catch (e: Exception) {
            AuthResult.Fail("회원가입 중 오류 발생", e)
        }
    }

    override suspend fun loginUser(email: String, pw: String): AuthResult<Boolean> {
        return try {
            val request = mapOf("userEmail" to email, "userPassword" to pw)
            val response = apiService.loginUser(request)

            // 💡 response.body()는 이제 Boolean이 아니라 Map입니다.
            if (response.isSuccessful && response.body() != null) {
                val uidFromServer = response.body()!!["uid"] ?: ""
                sessionManager.saveUid(uidFromServer) // 🔹 성공적으로 UID 저장 완료!
                AuthResult.Success(true)
            } else {
                AuthResult.Fail("이메일 또는 비밀번호가 틀렸습니다.")
            }
        } catch (e: Exception) {
            AuthResult.Fail("로그인 중 서버 연결 오류", e)
        }
    }

    override suspend fun updateUserName(name: String): AuthResult<Boolean> {
        return try {
            val response = apiService.updateUserName(name)
            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("이름 업데이트 실패")
        } catch (e: Exception) {
            AuthResult.Fail("서버 통신 실패", e)
        }
    }

    override suspend fun uploadUserProfileImage(imageUri: Uri): AuthResult<String> {
        return try {
            val imageData = resizeAndCompressImage(imageUri, 200, 200)
                ?: return AuthResult.Fail("이미지 압축 실패")

            val requestFile = imageData.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("profileImage", "profile_${System.currentTimeMillis()}.jpg", requestFile)

            val response = apiService.uploadProfileImage(body)
            if (response.isSuccessful) {
                AuthResult.Success(response.body() ?: "")
            } else {
                AuthResult.Fail("프로필 사진 업로드 실패")
            }
        } catch (e: Exception) {
            AuthResult.Fail("이미지 처리 중 오류 발생", e)
        }
    }
    override suspend fun updateUserGoal(goalDistance: Int, goalTime: Int): AuthResult<Boolean> {
        return try {
            // 🌟 1. 세션매니저에서 내 신분증(UID)을 꺼냅니다!
            val uid = sessionManager.getUid()

            // 🌟 2. apiService에 uid, 거리, 시간 3가지를 순서대로 딱 맞춰서 넣어줍니다!
            val response = apiService.updateUserGoal(uid, goalDistance, goalTime)

            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("목표 업데이트 실패")
        } catch (e: Exception) {
            AuthResult.Fail("서버 통신 오류", e)
        }
    }

    override suspend fun saveRunRecord(record: RunRecord): AuthResult<Boolean> {
        return try {
            // 🔹 SessionManager에서 내 UID를 가져와서 헤더에 넣습니다.
            val uid = sessionManager.getUid()
            val response = apiService.saveRunRecord(uid, record)

            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("서버 거절 (코드: ${response.code()})") // 코드를 찍어보면 원인을 더 잘 알 수 있습니다.
        } catch (e: Exception) {
            AuthResult.Fail("기록 저장 중 오류 발생: ${e.message}", e)
        }
    }

    override suspend fun deleteRunRecord(courseId: String): AuthResult<Boolean> {
        return try {
            // 🌟 1. 세션에서 내 UID(신분증)를 꺼냅니다!
            val uid = sessionManager.getUid()

            // 🌟 2. 방금 수정한 apiService에 uid와 courseId를 나란히 넣어줍니다!
            val response = apiService.deleteRunRecord(uid, courseId)

            if (response.isSuccessful) {
                AuthResult.Success(true)
            } else {
                AuthResult.Fail("러닝 기록 삭제 실패 (Error: ${response.code()})")
            }
        } catch (e: Exception) {
            AuthResult.Fail("삭제 요청 실패", e)
        }
    }

    // 🔹 고쳐진 부분: apiService.getMyUserData() 호출
    override suspend fun getMyUserData(): AuthResult<UserData> {
        return try {
            // 🌟 [수정] sessionManager에서 UID를 꺼내와서 전달해야 합니다.
            val uid = sessionManager.getUid()
            val response = apiService.getMyUserData(uid) // 👈 헤더에 UID 포함

            if (response.isSuccessful) {
                AuthResult.Success(response.body() ?: UserData())
            } else {
                AuthResult.Fail("데이터 로드 실패 (코드: ${response.code()})")
            }
        } catch (e: Exception) {
            AuthResult.Fail("사용자 정보 요청 오류", e)
        }
    }
    // 2. 마이페이지 러닝 기록 페이징 조회 수정
    // UserDataSourceImpl.kt (151번, 160번 에러 해결 구간)

    // UserDataSourceImpl.kt

    override suspend fun getRunsPaged(
        filter: RunFilter,
        lastDate: Long?,
        pageSize: Long
    ): AuthResult<List<RunRecord>> {
        return try {
            val uid = sessionManager.getUid()

            // 🌟 [에러 해결] 파라미터 이름을 apiService와 동일하게 'pageSize'로 맞춰줍니다.
            val response = apiService.getRunsPaged(
                uid = uid,
                filter = filter.name,
                lastDate = lastDate,
                pageSize = pageSize // 👈 'limit' 대신 인터페이스에 정의된 이름을 사용하세요.
            )

            if (response.isSuccessful) {
                val domainRecords = response.body()?.map { it.toDomain() } ?: emptyList()

                // 🌟 로그 추가: 첫 번째 기록의 좌표 개수를 출력해 봅니다.
                Log.d("MyPageData", "첫 번째 코스 좌표 개수: ${domainRecords.firstOrNull()?.course?.locationPoints?.size}")

                AuthResult.Success(domainRecords)
            } else {
                AuthResult.Fail("기록 로드 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            AuthResult.Fail("서버 통신 오류: ${e.message}")
        }
    }

    override suspend fun deleteUserAccount(password: String): AuthResult<Boolean> {
        return try {
            val response = apiService.deleteUserAccount(password)
            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("회원탈퇴 실패")
        } catch (e: Exception) {
            AuthResult.Fail("탈퇴 요청 오류", e)
        }
    }

    override suspend fun getUserGoal(): AuthResult<Pair<Int, Int>> {
        return try {
            val response = apiService.getUserGoal()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                AuthResult.Success(Pair(data["goalDistance"] ?: 0, data["goalTime"] ?: 0))
            } else {
                AuthResult.Fail("목표 정보 로드 실패")
            }
        } catch (e: Exception) {
            AuthResult.Fail("목표 요청 오류", e)
        }
    }

    override suspend fun getUserActivityStats(uid: String): AuthResult<UserActivityStats> {
        return try {
            val response = apiService.getUserActivityStats(uid)
            if (response.isSuccessful) {
                AuthResult.Success(response.body() ?: UserActivityStats())
            } else {
                AuthResult.Fail("활동 통계 로드 실패")
            }
        } catch (e: Exception) {
            AuthResult.Fail("통계 요청 오류", e)
        }
    }

    override suspend fun searchUserByEmail(searchId: String): AuthResult<UserData> {
        return try {
            val response = apiService.searchUserByEmail(searchId)
            // 서버가 넘겨준 Map<String, String> 데이터를 UserData로 예쁘게 포장합니다.
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val userData = UserData(
                    userId = data["uid"] ?: "",
                    userEmail = searchId,
                    userName = data["nickname"] ?: "",
                    userProfileUrl = data["profileImageUrl"] ?: ""
                )
                AuthResult.Success(userData)
            } else {
                AuthResult.Fail("사용자를 찾을 수 없습니다.")
            }
        } catch (e: Exception) {
            AuthResult.Fail("검색 중 오류 발생", e)
        }
    }

    // 🚨 파라미터 이름이 targetUid -> targetEmail 로 바뀌었습니다!
    override suspend fun sendFriendRequest(targetEmail: String): AuthResult<Boolean> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.requestFriend(uid, targetEmail) // Spring API 호출
            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("친구 신청 실패")
        } catch (e: Exception) {
            AuthResult.Fail("신청 요청 오류", e)
        }
    }

    override suspend fun acceptFriendRequest(targetUid: String): AuthResult<Boolean> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.acceptFriend(uid, targetUid)
            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("친구 수락 실패")
        } catch (e: Exception) {
            AuthResult.Fail("수락 요청 오류", e)
        }
    }

    override suspend fun declineFriendRequest(targetUid: String): AuthResult<Boolean> = deleteFriend(targetUid)
    // 🌟 [추가] 내 친구 목록 가져오기
    override suspend fun getMyFriends(): AuthResult<List<FriendSummary>> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.getFriends(uid)
            if (response.isSuccessful) {
                val list = response.body()?.map {
                    FriendSummary(
                        userId = it["uid"] ?: "",
                        userEmail = "",
                        userName = it["nickname"] ?: "",
                        userProfileUrl = it["profileImageUrl"] ?: ""
                    )
                } ?: emptyList()
                AuthResult.Success(list)
            } else AuthResult.Fail("목록 불러오기 실패")
        } catch (e: Exception) {
            AuthResult.Fail("서버 에러", e)
        }
    }
    // 🌟 [추가] 받은 요청 목록 가져오기
    override suspend fun getPendingRequests(): AuthResult<List<FriendSummary>> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.getPendingRequests(uid)
            if (response.isSuccessful) {
                val list = response.body()?.map {
                    FriendSummary(
                        userId = it["uid"] ?: "",
                        userEmail = "",
                        userName = it["nickname"] ?: "",
                        userProfileUrl = it["profileImageUrl"] ?: ""
                    )
                } ?: emptyList()
                AuthResult.Success(list)
            } else AuthResult.Fail("목록 불러오기 실패")
        } catch (e: Exception) {
            AuthResult.Fail("서버 에러", e)
        }
    }
    override suspend fun getUsersSummary(uidList: List<String>): AuthResult<List<UserData>> {
        return try {
            val response = apiService.getUsersSummary(uidList)
            if (response.isSuccessful) {
                AuthResult.Success(response.body() ?: emptyList())
            } else {
                AuthResult.Fail("요약 정보 로드 실패")
            }
        } catch (e: Exception) {
            AuthResult.Fail("서버 통신 오류", e)
        }
    }

    // 🌟 [수정] Firebase 찌꺼기 걷어내고 Spring API로 변경!
    override suspend fun getFriendUids(): AuthResult<List<String>> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.getFriends(uid) // 내 친구 목록을 불러온 뒤
            if (response.isSuccessful) {
                val list = response.body()?.mapNotNull { it["uid"] } ?: emptyList() // UID만 쏙 뽑아냅니다
                AuthResult.Success(list)
            } else {
                AuthResult.Fail("친구 목록 로드 실패")
            }
        } catch (e: Exception) {
            AuthResult.Fail("목록 요청 오류", e)
        }
    }

    override suspend fun deleteFriend(targetUid: String): AuthResult<Boolean> {
        return try {
            val uid = sessionManager.getUid()
            val response = apiService.deleteFriend(uid, targetUid)
            if (response.isSuccessful) AuthResult.Success(true)
            else AuthResult.Fail("친구 삭제 실패")
        } catch (e: Exception) {
            AuthResult.Fail("서버 통신 오류", e)
        }
    }

    // --- Helper Methods ---

    private fun resizeAndCompressImage(uri: Uri, width: Int, height: Int): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val rotatedBitmap = rotateImageIfRequired(originalBitmap, uri)
            val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width, height, true)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)

            if (rotatedBitmap != originalBitmap) rotatedBitmap.recycle()
            scaledBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e("ImageProcess", "Resizing failed", e)
            null
        }
    }

    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap {
        val input = context.contentResolver.openInputStream(selectedImage) ?: return img
        val ei = ExifInterface(input)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

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
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }


    override suspend fun signInWithGoogle(idToken: String): AuthResult<Boolean> {
        return try {
            val request = mapOf("idToken" to idToken)
            val response = apiService.signInWithGoogle(request)

            if (response.isSuccessful && response.body() != null) {
                val uidFromServer = response.body()!!["uid"] ?: ""
                sessionManager.saveUid(uidFromServer) // 🔹 구글 로그인도 UID 저장 완료!
                AuthResult.Success(true)
            } else {
                AuthResult.Fail("서버 구글 로그인 검증 실패 (Error: ${response.code()})")
            }
        } catch (e: Exception) {
            AuthResult.Fail("구글 로그인 중 서버 연결 오류", e)
        }
    }
}