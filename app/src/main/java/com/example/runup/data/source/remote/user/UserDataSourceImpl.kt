package com.example.runup.data.source.remote.user

import com.example.runup.domain.model.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserDataSourceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserDataSource {
    //email, pw 로 계정생성 후 데이터베이스에 등록
    override suspend fun registerUser(email: String, pw: String): Boolean {
        return try {
            //파이어베이스 인증(Auth)에 계정 생성
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pw).await()
            val uid = authResult.user?.uid ?: return false

            //생성된 고유 UID를 문서 ID로 사용하여 Firestore에 저장
            val userMap = mapOf(
                "userId" to uid,
                "userEmail" to email,
                "userName" to "name",
                "goalDistance" to 0, // 초기값
                "goalTime" to 0      // 초기값
            )

            firestore.collection("users")
                .document(uid)
                .set(userMap)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    //현재 로그인된 사용자의 상세 정보 가져오기
    override suspend fun getMyUserData(): UserData? {
        return try {
            // 현재 로그인된 사용자의 UID 가져오기
            val uid = firebaseAuth.currentUser?.uid ?: return null

            // Firestore의 'users' 컬렉션에서 해당 UID 문서 가져오기
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            // 가져온 문서를 UserData 클래스 형태로 변환 (기본 생성자 필요)
            document.toObject(UserData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun isEmailAlreadyRegistered(email: String): Boolean {
        return try {
            // 'users' 컬렉션에서 'userEmail' 필드가 입력받은 email과 일치하는 문서가 있는지 검색
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("userEmail", email)
                .get()
                .await()

            // 결과가 비어있지 않다면 이미 등록된 이메일임
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            // 에러 발생 시 안전하게 true(이미 있다고 가정) 혹은 false를 반환하도록 처리
            e.printStackTrace()
            false
        }
    }
}