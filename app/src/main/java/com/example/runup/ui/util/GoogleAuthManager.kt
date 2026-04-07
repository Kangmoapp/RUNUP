package com.example.runup.ui.util

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                onTokenReceived(idToken)
            } catch (e: GetCredentialException) {
                Log.e("GoogleLogin", "Credential 에러: ${e.message}")
            } catch (e: Exception) { // 모든 에러를 다 잡도록 추가
                Log.e("GoogleLogin", "일반 에러 발생: ${e::class.java.simpleName} - ${e.message}")
            }
        }
    }

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