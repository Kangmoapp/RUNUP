package com.example.runup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.FriendSummary
import com.example.runup.domain.model.UserData
import com.example.runup.domain.model.toSummary
import com.example.runup.domain.repository.UserRepository
import com.example.runup.ui.util.ImagePreloader
import com.example.runup.ui.util.UserStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.map


data class FriendUiState(
    val isLoading: Boolean = false,

    // 친구 목록 관련
    val friends: List<FriendSummary> = emptyList(),

    // 검색 관련
    val searchResult: FriendSummary? = null,
    val isSearching: Boolean = false,
    val searchErrorMessage: String? = null,

    // 친구 관리(신청) 관련
    val sentRequests: List<FriendSummary> = emptyList(),
    val receivedRequests: List<FriendSummary> = emptyList(),

    // UI 제어 플래그
    val showManagementDialog: Boolean = false,
    val deletingFriendId: String? = null // 현재 삭제 버튼이 활성화된 친구의 ID
)

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userStateManager: UserStateManager,
    private val imagePreloader: ImagePreloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendUiState())
    val uiState: StateFlow<FriendUiState> = _uiState.asStateFlow()

    private val _targetUserProfile = MutableStateFlow<UserData?>(null)
    val targetUserProfile = _targetUserProfile.asStateFlow()

    val profileBitmaps = userStateManager.profileBitmaps

    init {
        // 화면 진입 시 즉시 데이터 로드
        refreshAllFriendData()
    }

    // ── 🔹 탭 전환 및 창 닫기 시 일시적 상태 초기화 ──
    fun resetTransientStates() {
        _uiState.update { it.copy(
            searchResult = null,
            deletingFriendId = null,
            searchErrorMessage = null,
            isSearching = false
        ) }
    }

    // ── [핵심 로직 1] 모든 친구 데이터 동기화 ──
    fun refreshAllFriendData() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true
            ) }

            // 1. 내 최신 정보 가져오기 (UID 리스트를 얻기 위함)
            val myDataResult = userRepository.getMyUserData()

            if (myDataResult is AuthResult.Success) {
                val myData = myDataResult.data

                // 2. 친구, 보낸 신청, 받은 신청 요약 정보를 병렬로 가져오기
                // (성능을 위해 async-awaitAll 사용 권장하지만, 이해를 돕기 위해 순차로 작성합니다)
                val friends = userRepository.getUsersSummary(myData.friends).getOrDefault(emptyList()).map { it.toSummary() }
                val sent = userRepository.getUsersSummary(myData.sentRequests).getOrDefault(emptyList()).map { it.toSummary() }
                val received = userRepository.getUsersSummary(myData.receivedRequests).getOrDefault(emptyList()).map { it.toSummary() }

                _uiState.update { it.copy(
                    friends = friends,
                    sentRequests = sent,
                    receivedRequests = received,
                    isLoading = false
                ) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── [핵심 로직 2] ID로 친구 검색 ──
    fun searchUser(searchId: String) {
        if (searchId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchResult = null, searchErrorMessage = null) }

            val result = userRepository.searchUserByEmail(searchId)
            when (result) {
                is AuthResult.Success -> {
                    val summary = result.data.toSummary()
                    _uiState.update { it.copy(searchResult = summary, isSearching = false) }
                }
                is AuthResult.Fail -> {
                    _uiState.update { it.copy(searchErrorMessage = "해당 ID의 사용자가 없습니다.", isSearching = false) }
                }
            }
        }
    }

    // ── [핵심 로직 3] 친구 액션 처리 (신청/수락/거절) ──
    fun sendRequest(targetUid: String) {
        viewModelScope.launch {
            if (userRepository.sendFriendRequest(targetUid) is AuthResult.Success) {
                refreshAllFriendData() // 성공 시 목록 갱신
            }
        }
    }

    fun acceptRequest(targetUid: String) {
        viewModelScope.launch {
            if (userRepository.acceptFriendRequest(targetUid) is AuthResult.Success) {
                refreshAllFriendData()
            }
        }
    }

    fun declineRequest(targetUid: String) {
        viewModelScope.launch {
            if (userRepository.declineFriendRequest(targetUid) is AuthResult.Success) {
                refreshAllFriendData()
            }
        }
    }

    // ── UI 제어 함수 ──
    fun toggleManagementDialog(show: Boolean) {
        _uiState.update { it.copy(showManagementDialog = show) }
    }

    fun setDeletingFriend(uid: String?) {
        _uiState.update { it.copy(deletingFriendId = uid) }
    }

    fun deleteFriend(targetUid: String) {
        viewModelScope.launch {
            val result = userRepository.deleteFriend(targetUid)
            if (result is AuthResult.Success) {
                setDeletingFriend(null) // 삭제 버튼 숨기기
                refreshAllFriendData()   // 목록 갱신
            }
        }
    }

    fun fetchTargetUserProfile(targetUid: String) {
        viewModelScope.launch {
            // 이미 구현된 getUsersSummary를 활용 (리스트에 UID 하나만 담아서 보냄)
            val result = userRepository.getUsersSummary(listOf(targetUid))
            if (result is AuthResult.Success) {
                _targetUserProfile.value = result.data.firstOrNull()
            }
        }
    }

    // 팝업이 닫힐 때 데이터를 비워주기 위한 함수
    fun clearTargetUserProfile() {
        _targetUserProfile.value = null
    }
}

// AuthResult 확장 함수 (편의용)
fun <T> AuthResult<T>.getOrDefault(default: T): T {
    return if (this is AuthResult.Success) this.data else default
}