package com.example.runup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.data.source.local.SessionManager
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

data class FriendUiState(
    val isLoading: Boolean = false,
    val friends: List<FriendSummary> = emptyList(),
    val searchResult: FriendSummary? = null,
    val isSearching: Boolean = false,
    val searchErrorMessage: String? = null,
    val sentRequests: List<FriendSummary> = emptyList(),
    val receivedRequests: List<FriendSummary> = emptyList(),
    val showManagementDialog: Boolean = false,
    val deletingFriendId: String? = null
)

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userStateManager: UserStateManager,
    private val imagePreloader: ImagePreloader,
    private val sessionManager: SessionManager
) : ViewModel() {
    val myUid: String = sessionManager.getUid()
    private val _uiState = MutableStateFlow(FriendUiState())
    val uiState: StateFlow<FriendUiState> = _uiState.asStateFlow()

    private val _targetUserProfile = MutableStateFlow<UserData?>(null)
    val targetUserProfile = _targetUserProfile.asStateFlow()

    val profileBitmaps = userStateManager.profileBitmaps

    init {
        refreshAllFriendData()
    }

    fun resetTransientStates() {
        _uiState.update { it.copy(
            searchResult = null,
            deletingFriendId = null,
            searchErrorMessage = null,
            isSearching = false
        ) }
    }

    // ── [1] 모든 친구 데이터 동기화 (Spring API 사용) ──
    fun refreshAllFriendData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 🌟 Firebase가 아니라 Spring API에서 목록을 한 방에 가져옵니다.
            val friendsRes = userRepository.getMyFriends()
            val pendingRes = userRepository.getPendingRequests()

            _uiState.update { it.copy(
                friends = if (friendsRes is AuthResult.Success) friendsRes.data else emptyList(),
                receivedRequests = if (pendingRes is AuthResult.Success) pendingRes.data else emptyList(),
                sentRequests = emptyList(), // 서버 구조상 보낸 요청은 생략
                isLoading = false
            ) }
        }
    }

    // ── [2] 이메일로 친구 검색 ──
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
                    _uiState.update { it.copy(searchErrorMessage = "해당 이메일의 사용자가 없습니다.", isSearching = false) }
                }
            }
        }
    }

    // ── [3] 친구 액션 처리 (신청/수락/거절/삭제) ──
    fun sendRequest(targetUid: String) {
        // 🌟 UID가 아니라 이메일로 보내야 합니다!
        val targetEmail = _uiState.value.searchResult?.userEmail ?: return
        viewModelScope.launch {
            if (userRepository.sendFriendRequest(targetEmail) is AuthResult.Success) {
                refreshAllFriendData()
                resetTransientStates() // 신청 성공 시 팝업 닫기
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

    fun deleteFriend(targetUid: String) {
        viewModelScope.launch {
            if (userRepository.deleteFriend(targetUid) is AuthResult.Success) {
                setDeletingFriend(null)
                refreshAllFriendData()
            }
        }
    }

    // ── UI 제어 함수 ──
    fun toggleManagementDialog(show: Boolean) {
        _uiState.update { it.copy(showManagementDialog = show) }
        if (show) refreshAllFriendData() // 열 때마다 최신화
    }

    fun setDeletingFriend(uid: String?) {
        _uiState.update { it.copy(deletingFriendId = uid) }
    }

    fun fetchTargetUserProfile(targetUid: String) {
        viewModelScope.launch {
            val result = userRepository.getUsersSummary(listOf(targetUid))
            if (result is AuthResult.Success) {
                _targetUserProfile.value = result.data.firstOrNull()
            }
        }
    }

    fun clearTargetUserProfile() {
        _targetUserProfile.value = null
    }
}