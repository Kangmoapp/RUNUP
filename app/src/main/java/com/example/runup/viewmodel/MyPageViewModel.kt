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
import com.example.runup.domain.repository.UserRepository
import com.example.runup.ui.util.UserStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val userStateManager: UserStateManager
) : ViewModel() {

    val userState: StateFlow<UserData?> = userStateManager.userData

    // 창고에 있는 비트맵을 UI가 관찰할 수 있게 노출
    val profileBitmap = userStateManager.profileBitmap

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
            refreshRuns()     // 러닝 기록 첫 페이지 로드
        }
    }

    // 필터가 바뀌면 리스트를 초기화하고 새로 가져옴
    fun updateFilter(filter: RunFilter) {
        selectedFilter = filter
        refreshRuns()
    }

    private fun refreshRuns() {
        _pagedRuns.value = emptyList() // 현재 채워져 있는 runsRecord 비움
        lastVisibleRunDate = null // 마지막 가져온 데이터 초기화
        hasMore = true // 더 가져올 데이터 초기화
        loadMoreRuns()
    }

    fun loadMoreRuns() {
        if (!hasMore || isLoadingMore) return // 가져올데이터 더 없거나, 더 불러오기 로딩 중이면 return

        viewModelScope.launch {
            isLoadingMore = true

            // repository에 필터, 마지막 날짜, 페이지 크기(5)를 넘김
            val result = userRepository.getRunsPaged(
                filter = selectedFilter,
                lastDate = lastVisibleRunDate,
                pageSize = 5
            )

            if (result is AuthResult.Success) {
                val newRuns = result.data
                _pagedRuns.value += newRuns // 리스트 누적

                if (newRuns.size < 5) {
                    hasMore = false // 5개 미만으로 오면 더 이상 데이터 없음
                } else {
                    lastVisibleRunDate = newRuns.last().recordDate // 마지막 위치 저장
                }
            }
            isLoadingMore = false
        }
    }

    fun fetchMyUserData() {
        viewModelScope.launch {
            val result = userRepository.getMyUserData()
            if (result is AuthResult.Success) {
                userStateManager.updateUserData(result.data)
                Log.d("MyPage", "데이터 로드 성공: ${result.data.runs.size}개")
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

    // 시간 비교 로직 (기존과 동일하지만 Calendar 체크를 꼼꼼히)
    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }

    private fun isSameWeek(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
}