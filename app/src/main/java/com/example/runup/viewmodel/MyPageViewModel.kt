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
import com.example.runup.domain.usecase.SaveCourseUseCase
import com.example.runup.ui.util.ImagePreloader
import com.example.runup.ui.util.UserStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyPageRunState(
    val pagedRuns: List<RunRecord> = emptyList(),
    val selectedFilter: RunFilter = RunFilter.ALL,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val lastDate: Long? = null
)

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val saveCourseUseCase: SaveCourseUseCase,
    private val userStateManager: UserStateManager,
    private val imagePreloader: ImagePreloader
) : ViewModel() {
    // 유저 데이터
    val userState: StateFlow<UserData?> = userStateManager.userData

    // 창고에 있는 비트맵을 UI가 관찰할 수 있게 노출
    val profileBitmaps = userStateManager.profileBitmaps

    // 2. [통합] 러닝 기록 관련 UI 상태 📍
    private val _runState = MutableStateFlow(MyPageRunState())
    val runState: StateFlow<MyPageRunState> = _runState.asStateFlow()

    private val _courseSaveSuccess = MutableSharedFlow<Unit>()
    val courseSaveSuccess = _courseSaveSuccess.asSharedFlow()

    // 초기 데이터 로드 (유저 정보 + 첫 5개 기록)
    fun initData() {
        viewModelScope.launch {
            fetchMyUserData()
            loadMoreRuns(isRefresh = true)    // 러닝 기록 첫 페이지 로드
        }
    }

    // 1. 필터 업데이트 로직 수정
    fun updateFilter(filter: RunFilter) {
        if ((_runState.value.selectedFilter == filter)) return // 이미 선택된 필터면 무시

        _runState.update { it.copy(selectedFilter = filter) }
        loadMoreRuns(isRefresh = true)
    }

    // loadMoreRuns에 파라미터 추가
    // MyPageViewModel.kt

    fun loadMoreRuns(isRefresh: Boolean = false) {
        val currentState = _runState.value

        if (!isRefresh && (!currentState.hasMore || currentState.isLoadingMore)) return

        viewModelScope.launch {
            _runState.update { it.copy(isLoadingMore = true) }

            val lastDate = if (isRefresh) null else currentState.lastDate

            // 화면엔 5개를 보여줄 거지만 서버엔 6개를 요청
            val PAGE_SIZE = 5
            val result = userRepository.getRunsPaged(
                filter = _runState.value.selectedFilter,
                lastDate = lastDate,
                pageSize = (PAGE_SIZE + 1).toLong() // 6개 요청
            )

            if (result is AuthResult.Success) {
                val fetchedRuns = result.data

                // 1. 가져온 데이터가 6개(PAGE_SIZE + 1)라면 더 가져올 데이터가 있는 것
                val hasMoreData = fetchedRuns.size > PAGE_SIZE

                // 2. 실제 UI에 보여줄 데이터는 최대 5개까지만 자름
                val displayRuns = if (hasMoreData) fetchedRuns.take(PAGE_SIZE) else fetchedRuns

                _runState.update { it.copy(
                    pagedRuns = if (isRefresh) displayRuns else it.pagedRuns + displayRuns,
                    hasMore = hasMoreData, // 👈 6개가 왔을 때만 true
                    lastDate = displayRuns.lastOrNull()?.recordDate ?: it.lastDate,
                    isLoadingMore = false
                ) }
            } else {
                _runState.update { it.copy(isLoadingMore = false) }
            }
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
                // 1. UI 리스트에서 즉시 제거 (pagedRuns)
                _runState.update { currentState ->
                    currentState.copy(
                        pagedRuns = currentState.pagedRuns.filterNot { it.course.id == courseId }
                    )
                }
                // 전체 통계 데이터 갱신 (총 거리, 횟수 등)
                fetchMyUserData()
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

    fun addCourseFromRecord(runRecord: RunRecord) {
        viewModelScope.launch {
            // 1. RunRecord 안의 Course 객체 추출
            val courseToSave = runRecord.course

            // 2. UseCase 실행
            val result = saveCourseUseCase(courseToSave)

            // 3. 결과 처리
            when (result) {
                is AuthResult.Success -> {
                    _courseSaveSuccess.emit(Unit)
                }
                is AuthResult.Fail -> {
                    Log.e("MyPageViewModel", "코스 추가 실패: ${result.message}")
                }
            }
        }
    }
}