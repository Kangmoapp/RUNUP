package com.example.runup.data.source.remote.user

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Post
import com.example.runup.domain.model.RunFilter
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserActivityStats
import com.example.runup.domain.model.UserData
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.Calendar
import javax.inject.Inject

class UserDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : UserDataSource {

    // 이메일 확인
    override suspend fun isEmailAlreadyRegistered(email: String): AuthResult<Boolean> {
        return try {
            val querySnapshot = firestore.collection("UserData")
                .whereEqualTo("userEmail", email)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                // 1. 중복이 없음 -> 사용 가능한 이메일 (Success)
                AuthResult.Success(true)
            } else {
                // 2. 중복이 있음 -> 이미 가입된 이메일 (Fail로 던짐)
                AuthResult.Fail("이미 등록된 이메일입니다.")
            }
        } catch (e: Exception) {
            // 3. 네트워크 오류 등 물리적 에러
            AuthResult.Fail("이메일 확인 중 네트워크 오류가 발생했습니다.", e)
        }
    }

    // 사용자 회원가입
    override suspend fun registerUser(email: String, pw: String): AuthResult<Boolean> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pw).await()
            val uid = authResult.user?.uid ?: return AuthResult.Fail("UID 생성 실패")

            val userMap = mapOf(
                "userId" to uid,
                "userEmail" to email,
                "userPassword" to pw,
                "userName" to "Runner",
                "goalDistance" to 0,
                "goalTime" to 0
            )

            firestore.collection("UserData").document(uid).set(userMap).await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("회원가입 실패: ${e.localizedMessage}", e)
        }
    }

    // 구글로 사용자 로그인
    override suspend fun signInWithGoogle(idToken: String): AuthResult<Boolean> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: return AuthResult.Fail("User null")

            val userDoc = firestore.collection("UserData").document(user.uid).get().await()

            if (!userDoc.exists()) {
                val userMap = mutableMapOf(
                    "userId" to user.uid,
                    "userEmail" to (user.email ?: ""),
                    "userName" to (user.displayName ?: "Runner"),
                    "goalDistance" to 0,
                    "goalTime" to 0,
                    "authType" to "google" // 이메일 가입자와 구분용
                )
                firestore.collection("UserData").document(user.uid).set(userMap).await()
            }
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("Google Login Error: ${e.localizedMessage}")
        }
    }

    // 로그인
    override suspend fun loginUser(email: String, pw: String): AuthResult<Boolean> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pw).await()
            if (result.user != null) {
                AuthResult.Success(true)
            } else {
                AuthResult.Fail("사용자 정보가 없습니다.")
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            // 1. 이메일 자체가 등록되지 않은 경우
            AuthResult.Fail("등록되지 않은 이메일입니다.", e)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // 2. 이메일은 맞지만 비밀번호가 틀렸거나, 형식이 잘못된 경우
            AuthResult.Fail("비밀번호가 틀렸습니다.", e)
        } catch (e: Exception) {
            // 3. 그 외 (네트워크 오류 등)
            AuthResult.Fail("로그인 실패: ${e.localizedMessage}", e)
        }
    }

    // 사용자 이름 등록
    override suspend fun updateUserName(name: String): AuthResult<Boolean> {
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")
            firestore.collection("UserData").document(userid)
                .update("userName", name).await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("이름 업데이트 실패", e)
        }
    }

    override suspend fun uploadUserProfileImage(imageUri: Uri): AuthResult<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")

            // 1. Storage 참조 (메인용과 미니용 두 개 설정)
            val profileMainRef = storage.reference.child("userProfileImages/$uid/profile_main.jpg")
            val profileMiniRef = storage.reference.child("userProfileImages/$uid/profile_mini.jpg")

            // 2. 이미지 압축 및 리사이징
            // 메인 프로필 (마이페이지용)
            val mainData = resizeAndCompressImage(imageUri, width = 200, height = 200)
                ?: return AuthResult.Fail("메인 이미지 압축 실패")

            // 미니 프로필 (댓글/커뮤니티 목록용)
            val miniData = resizeAndCompressImage(imageUri, width = 100, height = 100)
                ?: return AuthResult.Fail("미니 이미지 압축 실패")

            // 3. Storage 업로드
            profileMainRef.putBytes(mainData).await()
            profileMiniRef.putBytes(miniData).await()

            // 4. 각각의 다운로드 URL 가져오기
            val mainUrl = profileMainRef.downloadUrl.await().toString()
            val miniUrl = profileMiniRef.downloadUrl.await().toString()

            // 5. Firestore 업데이트 (두 필드를 동시에 업데이트)
            firestore.collection("UserData").document(uid)
                .update(
                    mapOf(
                        "userProfileUrl" to mainUrl,
                        "userProfileUrlMini" to miniUrl
                    )
                ).await()

            // 성공 시 메인 URL 반환 (필요에 따라 miniUrl을 반환해도 됨)
            AuthResult.Success(mainUrl)
        } catch (e: Exception) {
            AuthResult.Fail("프로필 사진 업로드 실패: ${e.localizedMessage}", e)
        }
    }

    private fun resizeAndCompressImage(uri: Uri, width: Int, height: Int): ByteArray? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        // [추가] 썸네일 생성 전에도 회전 각도 보정 실행
        val rotatedBitmap = rotateImageIfRequired(originalBitmap, uri)

        // 지정된 크기로 리사이징
        val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width, height, true)

        val outputStream = ByteArrayOutputStream()
        // 퀄리티를 50~60 정도로 낮춰도 150px에서는 충분히 깨끗합니다.
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)

        // 사용한 비트맵들 메모리 해제 (선택 사항이지만 권장)
        if (rotatedBitmap != originalBitmap) {
            rotatedBitmap.recycle()
        }

        return outputStream.toByteArray()
    }

    // [필수 보조] 사진 회전 방지 로직
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
        img.recycle() // 메모리 해제
        return rotatedImg
    }

    // 사용자 목표 업데이트
    override suspend fun updateUserGoal(goalDistance: Int, goalTime: Int): AuthResult<Boolean> {
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")
            val updates = mapOf("goalDistance" to goalDistance, "goalTime" to goalTime)
            firestore.collection("UserData").document(userid).update(updates).await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("목표 설정 실패", e)
        }
    }

    // 사용자 러닝 기록 저장
    override suspend fun saveRunRecord(record: RunRecord): AuthResult<Boolean> {
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")

            val userDocRef = firestore.collection("UserData").document(userid)
            // metadata 서브 컬렉션 내의 runRecordInfo 문서 참조
            val metaDocRef = userDocRef.collection("metadata").document("runRecordInfo")

            firestore.runTransaction { transaction ->
                // 1. 현재 마지막 번호 가져오기 (문서가 없으면 0으로 시작)
                val snapshot = transaction.get(metaDocRef)
                val lastNum = snapshot.getLong("lastRunRecordNum") ?: 0L
                val nextNum = lastNum + 1

                // 새 ID 생성 (예: runrecord_1)
                val newRecordId = "runningRecord_$nextNum"

                val updatedRecord = record.copy(
                    course = record.course.copy(id = newRecordId)
                )

                val runRecordRef = userDocRef.collection("runs").document(newRecordId)
                transaction.set(runRecordRef, updatedRecord)

                // 4. metadata의 번호 업데이트
                transaction.set(metaDocRef, mapOf("lastRunRecordNum" to nextNum))

                // 5. UserData 문서의 totalRunningDistance 누적 업데이트
                transaction.update(userDocRef,
                    "totalRunningDistance", FieldValue.increment(record.course.distance.toLong()),
                    "totalRunningCount", FieldValue.increment(1))

                null // Transaction은 결과값을 반환해야 하므로 null 반환
            }.await()

            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("러닝 기록 저장 실패: ${e.message}", e)
        }
    }

    override suspend fun deleteRunRecord(courseId: String): AuthResult<Boolean> {
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")
            val userDocRef = firestore.collection("UserData").document(userid)
            val runsCollectionRef = userDocRef.collection("runs")

            // 1. 해당 courseId를 가진 문서를 쿼리로 찾기
            val querySnapshot = runsCollectionRef
                .whereEqualTo("course.id", courseId)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return AuthResult.Fail("삭제할 기록을 찾을 수 없습니다.")
            }

            // 2. 일괄 처리를 위해 Batch 생성
            firestore.runBatch { batch ->
                for (document in querySnapshot.documents) {
                    // 삭제할 문서에서 거리 정보 가져오기 (전체 거리 차감을 위해)
                    val distance = document.getLong("course.distance") ?: 0L

                    // 해당 기록 삭제
                    batch.delete(document.reference)

                    // 🔹 통계 데이터 차감 업데이트 (거리 -n, 횟수 -1)
                    batch.update(userDocRef,
                        "totalRunningDistance", FieldValue.increment(-distance),
                        "totalRunningCount", FieldValue.increment(-1) // 횟수 -1
                    )
                }
            }.await()

            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("러닝 기록 삭제 실패", e)
        }
    }

    // 유저 기본 정보만 가져오기 (runs 제외)
    override suspend fun getMyUserData(): AuthResult<UserData> {
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")
            val userSnapshot = firestore.collection("UserData").document(userid).get().await()
            val userData = userSnapshot.toObject(UserData::class.java) ?: UserData()

            AuthResult.Success(userData)
        } catch (e: Exception) {
            AuthResult.Fail("사용자 정보 로드 실패", e)
        }
    }

    // 2. [신규] 러닝 기록만 페이지 단위로 가져오기
    override suspend fun getRunsPaged(
        filter: RunFilter,
        lastDate: Long?,
        pageSize: Long
    ): AuthResult<List<RunRecord>> {
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")

            // 기본 쿼리 설정: 날짜 내림차순(최신순)
            var query = firestore.collection("UserData")
                .document(userid)
                .collection("runs")
                .orderBy("recordDate", Query.Direction.DESCENDING)

            // 🔹 필터링 로직 추가 (날짜 범위 제한)
            val startTime = getFilterStartTime(filter)
            if (startTime > 0) {
                query = query.whereGreaterThanOrEqualTo("recordDate", startTime)
            }

            // 🔹 페이지네이션 핵심: 마지막 데이터 다음부터 가져오기
            if (lastDate != null) {
                query = query.startAfter(lastDate)
            }

            // 🔹 개수 제한
            val snapshot = query.limit(pageSize).get().await()
            val runRecords = snapshot.toObjects(RunRecord::class.java)

            AuthResult.Success(runRecords)
        } catch (e: Exception) {
            AuthResult.Fail("기록 로드 실패", e)
        }
    }

    // 사용자 목표 가져오기
    override suspend fun getUserGoal(): AuthResult<Pair<Int,Int>>{
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")

            // Firestore에서 사용자 문서 가져오기
            val document = firestore.collection("UserData").document(userid).get().await()

            if (document.exists()) {
                // 필드 값을 읽어와서 UserGoal 객체로 변환
                val goalDistance = document.getLong("goalDistance")?.toInt() ?: 0
                val goalTime = document.getLong("goalTime")?.toInt() ?: 0

                AuthResult.Success(Pair(goalDistance, goalTime))
            } else {
                // 문서가 아예 없는 경우 기본값 반환 혹은 실패 처리
                AuthResult.Fail("사용자 목표 정보가 존재하지 않습니다.")
            }
        } catch (e: Exception) {
            AuthResult.Fail("목표 정보를 가져오는 중 오류 발생", e)
        }
    }

    // 🔹 필터별 시작 시간 계산 헬퍼 함수
    private fun getFilterStartTime(filter: RunFilter): Long {
        val cal = Calendar.getInstance()

        // 🔹 공통: 시, 분, 초, 밀리초를 0으로 완벽 초기화 (오늘 0시 0분 0초)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when (filter) {
            RunFilter.TODAY -> {
                cal.timeInMillis
            }
            RunFilter.WEEK -> {
                // 🔹 [수정] 이번 주 월요일을 시작점으로 강제 설정
                // 오늘이 일요일(1)이면 지난주 월요일로 가는 것을 방지하기 위한 로직
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 일(1), 월(2) ... 토(7)

                if (dayOfWeek == Calendar.SUNDAY) {
                    // 오늘이 일요일이면 6일 전인 지난 월요일로 이동
                    cal.add(Calendar.DAY_OF_YEAR, -6)
                } else {
                    // 오늘이 월~토라면 이번 주 월요일로 이동
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                }
                cal.timeInMillis
            }
            RunFilter.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            RunFilter.ALL -> 0L
        }
    }

    // 유저 커뮤니티 활동 정보 및 이름 불러오기
    override suspend fun getUserActivityStats(uid: String): AuthResult<UserActivityStats> {
        return try {
            // 1. 참조 생성
            val userDocRef = firestore.collection("UserData").document(uid)
            val statsDocRef = userDocRef.collection("PostStats").document("info")

            // 2. 두 문서를 동시에 호출 (병렬 처리로 속도 최적화)
            // Note: Firestore 문서는 각각 별개의 호출이 필요합니다.
            val userSnapshot = userDocRef.get().await()
            val statsSnapshot = statsDocRef.get().await()

            // 3. 유저 이름 추출
            val userName = userSnapshot.getString("userName") ?: "Runner"

            // 4. 통계 데이터와 이름 결합
            if (statsSnapshot.exists()) {
                AuthResult.Success(UserActivityStats(
                    userName = userName, // 🔹 가져온 이름 꽂아주기
                    uploadPostIds = statsSnapshot.get("uploadPostIds") as? List<String> ?: emptyList(),
                    likePostIds = statsSnapshot.get("likePostIds") as? List<String> ?: emptyList(),
                    commentPostIds = statsSnapshot.get("commentPostIds") as? List<String> ?: emptyList(),
                    followPostIds = statsSnapshot.get("followPostIds") as? List<String> ?: emptyList(),
                ))
            } else {
                // 통계 문서가 아직 생성되지 않았더라도 이름은 전달해야 함
                AuthResult.Success(UserActivityStats(userName = userName))
            }
        } catch (e: Exception) {
            Log.e("UserRepo", "데이터 통합 로드 실패: ${e.localizedMessage}")
            AuthResult.Fail(e.localizedMessage ?: "정보 로드 실패")
        }
    }

    //---------------------------------------------------------------------------------------------//
    //친구 기능

    // 1. ID로 사용자 검색 (정확히 일치하는 ID)
    override suspend fun searchUserByEmail(searchId: String): AuthResult<UserData> {
        return try {
            val query = firestore.collection("UserData")
                .whereEqualTo("userEmail", searchId)
                .get()
                .await()

            val user = query.documents.firstOrNull()?.toObject(UserData::class.java)

            if (user != null) AuthResult.Success(user)
            else AuthResult.Fail("해당 이메일의 사용자를 찾을 수 없습니다.")
        } catch (e: Exception) {
            AuthResult.Fail("사용자 검색 실패", e)
        }
    }

    // 2. 친구 신청 보내기 (나의 sent, 상대의 received 업데이트)
    override suspend fun sendFriendRequest(targetUid: String): AuthResult<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
            if (myUid == targetUid) return AuthResult.Fail("자신에게는 신청할 수 없습니다.")

            val myDocRef = firestore.collection("UserData").document(myUid)
            val targetDocRef = firestore.collection("UserData").document(targetUid)

            firestore.runTransaction { transaction ->
                // 내 '보낸 신청' 리스트에 추가
                transaction.update(myDocRef, "sentRequests", FieldValue.arrayUnion(targetUid))
                // 상대방 '받은 신청' 리스트에 추가
                transaction.update(targetDocRef, "receivedRequests", FieldValue.arrayUnion(myUid))
                null
            }.await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("친구 신청 실패", e)
        }
    }

    // 3. 친구 신청 수락 (핵심 로직)
    override suspend fun acceptFriendRequest(targetUid: String): AuthResult<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
            val myDocRef = firestore.collection("UserData").document(myUid)
            val targetDocRef = firestore.collection("UserData").document(targetUid)

            firestore.runTransaction { transaction ->
                // 1. 요청 목록에서 서로 제거
                transaction.update(myDocRef, "receivedRequests", FieldValue.arrayRemove(targetUid))
                transaction.update(targetDocRef, "sentRequests", FieldValue.arrayRemove(myUid))

                // 2. 친구 목록에 서로 추가
                transaction.update(myDocRef, "friends", FieldValue.arrayUnion(targetUid))
                transaction.update(targetDocRef, "friends", FieldValue.arrayUnion(myUid))
                null
            }.await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("친구 수락 실패", e)
        }
    }

    // 4. 친구 신청 거절 또는 보낸 신청 취소
    override suspend fun declineFriendRequest(targetUid: String): AuthResult<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
            val myDocRef = firestore.collection("UserData").document(myUid)
            val targetDocRef = firestore.collection("UserData").document(targetUid)

            firestore.runTransaction { transaction ->
                // 1. 내 문서에서 상대방 UID 제거 (받은 신청/보낸 신청 양쪽 다 체크해서 제거)
                transaction.update(myDocRef, "receivedRequests", FieldValue.arrayRemove(targetUid))
                transaction.update(myDocRef, "sentRequests", FieldValue.arrayRemove(targetUid))

                // 2. 상대방 문서에서 내 UID 제거 (보낸 신청/받은 신청 양쪽 다 체크해서 제거)
                transaction.update(targetDocRef, "sentRequests", FieldValue.arrayRemove(myUid))
                transaction.update(targetDocRef, "receivedRequests", FieldValue.arrayRemove(myUid))

                null
            }.await()

            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("요청 처리 실패: ${e.localizedMessage}")
        }
    }

    // 5. UID 리스트로 요약 정보 가져오기 (친구 목록 띄울 때 사용)
    override suspend fun getUsersSummary(uidList: List<String>): AuthResult<List<UserData>> {
        if (uidList.isEmpty()) return AuthResult.Success(emptyList())
        return try {
            // Firestore whereIn은 한 번에 최대 30명까지 지원
            val snapshot = firestore.collection("UserData")
                .whereIn("userId", uidList)
                .get()
                .await()

            val users = snapshot.toObjects(UserData::class.java)
            AuthResult.Success(users)
        } catch (e: Exception) {
            AuthResult.Fail("목록 로드 실패", e)
        }
    }

    // 친구 목록 가져오기
    override suspend fun getFriendUids(): AuthResult<List<String>> {
        return try {
            val uid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
            val snapshot = firestore.collection("UserData").document(uid).get().await()
            val friends = snapshot.get("friends") as? List<String> ?: emptyList()
            AuthResult.Success(friends)
        } catch (e: Exception) {
            AuthResult.Fail("친구 목록 로드 실패", e)
        }
    }

    override suspend fun deleteFriend(targetUid: String): AuthResult<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
            val myDocRef = firestore.collection("UserData").document(myUid)
            val targetDocRef = firestore.collection("UserData").document(targetUid)

            firestore.runTransaction { transaction ->
                // 양측의 friends 리스트에서 서로의 UID를 제거
                transaction.update(myDocRef, "friends", FieldValue.arrayRemove(targetUid))
                transaction.update(targetDocRef, "friends", FieldValue.arrayRemove(myUid))
                null
            }.await()

            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("친구 삭제 실패: ${e.localizedMessage}")
        }
    }

    override suspend fun deletePersonalUserData(): AuthResult<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")

            // 1. Storage 프로필 이미지 삭제
            try { storage.reference.child("userProfileImages/$uid").delete().await() } catch (e: Exception) { }

            // 2. Firestore 유저 데이터 삭제 (하위 컬렉션 포함)
            val userDocRef = firestore.collection("UserData").document(uid)

            // ── 🔹 [사전 작업] 상대방 데이터 정리를 위해 내 리스트 정보 가져오기 📍 ──
            val userSnapshot = userDocRef.get().await()
            val friendUids = userSnapshot.get("friends") as? List<String> ?: emptyList()
            val receivedUids = userSnapshot.get("receivedRequests") as? List<String> ?: emptyList()
            val sentUids = userSnapshot.get("sentRequests") as? List<String> ?: emptyList()

            val runs = userDocRef.collection("runs").get().await()
            val stats = userDocRef.collection("PostStats").get().await()
            val metadata = userDocRef.collection("metadata").get().await()

            firestore.runBatch { batch ->
                // (1) 상대방의 친구 목록에서 나를 삭제
                friendUids.forEach { friendId ->
                    val ref = firestore.collection("UserData").document(friendId)
                    batch.update(ref, "friends", FieldValue.arrayRemove(uid))
                }

                // (2) 나에게 신청했던 사람들(received)의 '보낸 요청'에서 나를 삭제
                receivedUids.forEach { requesterId ->
                    val ref = firestore.collection("UserData").document(requesterId)
                    batch.update(ref, "sentRequests", FieldValue.arrayRemove(uid))
                }

                // (3) 내가 신청했던 사람들(sent)의 '받은 요청'에서 나를 삭제
                sentUids.forEach { receiverId ->
                    val ref = firestore.collection("UserData").document(receiverId)
                    batch.update(ref, "receivedRequests", FieldValue.arrayRemove(uid))
                }

                // (4) 내 하위 데이터 삭제 (기존 로직)
                runs.forEach { batch.delete(it.reference) }
                stats.forEach { batch.delete(it.reference) }
                metadata.forEach { batch.delete(it.reference) }

                // (5) 내 메인 문서 삭제
                batch.delete(userDocRef)
            }.await()

            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("개인 데이터 삭제 실패: ${e.localizedMessage}")
        }
    }

    /**
     * [추가] 마지막 Auth 계정 삭제
     */
    override suspend fun deleteAuthAccount(): AuthResult<Boolean> {
        return try {
            auth.currentUser?.delete()?.await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("인증 계정 삭제 실패 (재로그인 필요)")
        }
    }
}