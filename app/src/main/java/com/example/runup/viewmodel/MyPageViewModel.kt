package com.example.runup.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.RunFilter
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserData
import com.example.runup.domain.model.toSummary
import com.example.runup.domain.repository.UserRepository
import com.example.runup.ui.util.ImagePreloader
import com.example.runup.ui.util.UserStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject



@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userStateManager: UserStateManager,
    private val imagePreloader: ImagePreloader
) : ViewModel() {

    val userState: StateFlow<UserData?> = userStateManager.userData

    // 창고에 있는 비트맵을 UI가 관찰할 수 있게 노출
    val profileBitmaps = userStateManager.profileBitmaps

    // 필터링된 러닝 기록 리스트 (누적용)
    private val _pagedRuns = MutableStateFlow<List<RunRecord>>(emptyList())
    val pagedRuns: StateFlow<List<RunRecord>> = _pagedRuns

    // 🔹 2. 마지막으로 가져온 데이터의 참조 (다음 페이지 로드용)
    private var lastVisibleRunDate: Long? = null

    // 🔹 3. 더 가져올 데이터가 있는지 여부
    var hasMore by mutableStateOf(true)
        private set

    var isLoadingMore by mutableStateOf(false)
        private set

    // 필터 상태 (기본값 ALL)
    var selectedFilter by mutableStateOf(RunFilter.ALL)
        private set

    // 초기 데이터 로드 (유저 정보 + 첫 5개 기록)
    fun initData() {
        viewModelScope.launch {
            fetchMyUserData()
            loadMoreRuns(isRefresh = true)    // 러닝 기록 첫 페이지 로드
        }
    }

    // 1. 필터 업데이트 로직 수정
    fun updateFilter(filter: RunFilter) {
        if (selectedFilter == filter) return // 이미 선택된 필터면 무시
        selectedFilter = filter

        // 🔹 [수정] refreshRuns()의 emptyList() 로직을 지우고 바로 로드 시작
        // 대신 "새로고침(isRefresh = true)"이라는 신호를 보냅니다.
        loadMoreRuns(isRefresh = true)
    }

    // 2. [기존 함수 수정] loadMoreRuns에 파라미터 추가
    fun loadMoreRuns(isRefresh: Boolean = false) {
        // 🔹 [수정] 새로고침일 때는 상태를 초기화하지만 리스트를 비우지는 않음
        if (isRefresh) {
            lastVisibleRunDate = null
            hasMore = true
        }

        if (!hasMore || isLoadingMore) return

        viewModelScope.launch {
            isLoadingMore = true

            val result = userRepository.getRunsPaged(
                filter = selectedFilter,
                lastDate = lastVisibleRunDate,
                pageSize = 5
            )

            if (result is AuthResult.Success) {
                val newRuns = result.data

                // 🔹 [핵심 수정] 새로고침이면 덮어쓰고, 아니면 누적합니다.
                if (isRefresh) {
                    _pagedRuns.value = newRuns // 👈 여기서 기존 리스트가 한 번에 바뀜 (스크롤 유지)
                } else {
                    _pagedRuns.value += newRuns
                }

                if (newRuns.size < 5) {
                    hasMore = false
                } else {
                    lastVisibleRunDate = newRuns.last().recordDate
                }
            }
            isLoadingMore = false
        }
    }

    fun fetchMyUserData() {
        viewModelScope.launch {
            // 1. 내 데이터 가져오기
            val result = userRepository.getMyUserData()

            if (result is AuthResult.Success) {
                val myData = result.data
                userStateManager.updateUserData(myData)
                Log.d("MyPage", "데이터 로드 성공: ${myData.runs.size}개")

                // 3. [핵심 추가] 친구들의 프로필 이미지 프리로드
                if (myData.friends.isNotEmpty()) {
                    // 친구들의 요약 정보(이름, 프로필URL)를 서버에서 가져옴
                    val friendsResult = userRepository.getUsersSummary(myData.friends)
                    if (friendsResult is AuthResult.Success) {
                        val friends = friendsResult.data.map { it.toSummary() }

                        // 친구들 각각의 URL을 병렬로 프리로드
                        friends.forEach { friend ->
                            preloadProfileImage(friend.userProfileUrl)
                        }
                    }
                }
            }
        }
    }

    // 🔹 공통 프리로드 헬퍼 함수
    private fun preloadProfileImage(url: String) {
        if (url.isEmpty()) return

        // 이미 창고(Map)에 있다면 중복 로드 방지
        if (userStateManager.profileBitmaps.value.containsKey(url)) return

        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = imagePreloader.loadBitmap(url, 150) // 150px 정도로 로드
            bitmap?.let {
                userStateManager.updateProfileBitmap(url, it)
            }
        }
    }

    fun deleteRun(courseId: String) {
        viewModelScope.launch {
            val result = userRepository.deleteRunRecord(courseId)
            if (result is AuthResult.Success) {
                // 삭제 성공 시 최신 데이터를 다시 불러와 UI 갱신
                fetchMyUserData()
                Log.d("MyPage", "기록 삭제 성공: $courseId")
            } else {
                Log.e("MyPage", "기록 삭제 실패")
            }
        }
    }

    fun updateName(newName: String) {
        if (newName.isBlank()) return

        viewModelScope.launch {
            val result = userRepository.updateUserName(newName)
            if (result is AuthResult.Success) {
                fetchMyUserData() // UI 갱신을 위해 데이터 다시 로드
                Log.d("MyPage", "이름 수정 성공: $newName")
            }
        }
    }

    fun uploadProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            // 업로드 중 로딩 상태를 보여주고 싶다면 추가적인 로딩 상태값이 필요할 수 있습니다.
            val result = userRepository.uploadUserProfileImage(imageUri)
            if (result is AuthResult.Success) {
                // 업로드 성공 시 새로운 URL이 Firestore에 저장되었으므로 데이터를 다시 불러와 UI 갱신
                fetchMyUserData()
                Log.d("MyPage", "프로필 이미지 업로드 성공: ${result.data}")
            } else if (result is AuthResult.Fail) {
                Log.e("MyPage", "프로필 이미지 업로드 실패: ${result.message}")
            }
        }
    }
}