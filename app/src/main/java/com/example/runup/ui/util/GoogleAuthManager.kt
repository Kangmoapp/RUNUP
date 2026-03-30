package com.example.runup.ui.util

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor() {

    private val webClientId = "119763044034-7fmna97qp33hgoke2a7f0ogpm2i8ssj3.apps.googleusercontent.com"

    fun signIn(
        context: Context,
        scope: CoroutineScope,
        onTokenReceived: (String) -> Unit
    ) {
        val credentialManager = CredentialManager.create(context) //받아온 context 건네줌,(loginScreen)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) //기기에 등록된 모든 구글계정 전부 표시
            .setServerClientId(webClientId) //RUN UP 웹 클라이언트 아이디
            .setAutoSelectEnabled(false) //사용자가 이 앱에서 로그인한 적이 있는 계정이 딱 하나라면, 계정 선택 창을 띄우지 않고 자동으로 그 계정으로 로그인을 시도
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption) //구글 로그인 방식 이번 요청에 선언
            .build()

        scope.launch {
            try {
                Log.d("GoogleLogin", "2. getCredential 호출 직전")
                val result = credentialManager.getCredential(context = context, request = request)
                Log.d("GoogleLogin", "3. 결과 받음: ${result.credential.type}")
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                    Log.d("GoogleLogin", "4. 조건 일치! 토큰 추출 시작")
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    Log.d("GoogleLogin", "5. 토큰 추출 성공: ${idToken.take(10)}...") // 보안상 앞부분만 출력
                    onTokenReceived(idToken)
                } else {
                    // 만약 이 로그가 찍힌다면 조건문 설정이 잘못된 것입니다.
                    Log.e("GoogleLogin", "4. 실패: 타입이 일치하지 않음. 실제 타입: ${credential.type}")
                }
            } catch (e: GetCredentialException) {
                Log.e("GoogleLogin", "Credential 에러: ${e.message}")
            } catch (e: Exception) { // 모든 에러를 다 잡도록 추가
                Log.e("GoogleLogin", "일반 에러 발생: ${e::class.java.simpleName} - ${e.message}")
            }
        }
    }

    // GoogleAuthManager.kt 내부에 추가

    suspend fun signOut(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context)
            // 기존에 저장된(선택된) 자격 증명 상태를 초기화합니다.
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Log.d("GoogleLogin", "로그아웃 성공: 자격 증명 상태 초기화됨")
        } catch (e: Exception) {
            Log.e("GoogleLogin", "로그아웃 실패: ${e.message}")
        }
    }
}