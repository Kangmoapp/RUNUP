package com.example.runup.data.source.remote.user

import android.util.Log
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserDataSource {

    //1. 이메일 확인
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

    //2. 사용자 등록
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

    //3. 로그인
    override suspend fun loginUser(email: String, pw: String): AuthResult<Boolean> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pw).await()
            if (result.user != null) AuthResult.Success(true)
            else AuthResult.Fail("사용자 정보가 없습니다.")
        } catch (e: Exception) {
            AuthResult.Fail("로그인 실패: 이메일 또는 비밀번호를 확인하세요.", e)
        }
    }

    //4. 사용자 이름 등록
    override suspend fun updateUserName(name: String): AuthResult<Boolean> {
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")
            Log.d("UseCaseTest", "성공! 사용 가능 여부: ${userid}")
            firestore.collection("UserData").document(userid)
                .update("userName", name).await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("이름 업데이트 실패", e)
        }
    }

    //5. 사용자 목표 업데이트
    override suspend fun updateUserGoal(goalDistance: Int, goalTime: Int): AuthResult<Boolean> {
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")
            val updates = mapOf("goalDistance" to goalDistance, "goalTime" to goalTime)
            firestore.collection("UserData").document(userid).update(updates).await()
            Log.d("UseCaseTest", "성공! 사용 가능 여부: ${auth.currentUser?.uid}")
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("목표 설정 실패", e)
        }
    }

    //6. 사용자 러닝 기록 저장
    override suspend fun saveRunRecord(record: RunRecord): AuthResult<Boolean> {
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")
            firestore.collection("UserData").document(userid)
                .collection("runs").document().set(record).await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("러닝 기록 저장 실패", e)
        }
    }

    //7. 사용자 데이터 불러오기
    override suspend fun getMyUserData(): AuthResult<UserData> {
        return try {
            val userid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인이 필요합니다.")
            val document = firestore.collection("UserData").document(userid).get().await()
            val userData = document.toObject(UserData::class.java) ?: UserData()
            AuthResult.Success(userData)
        } catch (e: Exception) {
            AuthResult.Fail("사용자 정보 로드 실패", e)
        }
    }
}