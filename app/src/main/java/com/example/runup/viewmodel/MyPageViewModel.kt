package com.example.runup.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
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

enum class RunFilter(val label: String) {
    TODAY("오늘"), WEEK("이번 주"), MONTH("이번 달"), ALL("전체")
}

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userStateManager: UserStateManager
) : ViewModel() {

    private val _userState = MutableStateFlow<UserData?>(null)
    val userState: StateFlow<UserData?> = _userState

    // 창고에 있는 비트맵을 UI가 관찰할 수 있게 노출
    val profileBitmap = userStateManager.profileBitmap

    // 필터 상태 (기본값 ALL)
    var selectedFilter by mutableStateOf(RunFilter.ALL)
        private set

    fun fetchMyUserData() {
        viewModelScope.launch {
            val result = userRepository.getMyUserData()
            if (result is AuthResult.Success) {
                _userState.value = result.data
                Log.d("MyPage", "데이터 로드 성공: ${result.data.runs.size}개")
            }
        }
    }

    // [수정] 게터 대신 직접 계산 로직을 분리하여 UI에서 호출하기 쉽게 만듦
    fun getFilteredRuns(): List<RunRecord> {
        val allRuns = _userState.value?.runs ?: return emptyList()
        val now = System.currentTimeMillis()

        val filtered = when (selectedFilter) {
            RunFilter.TODAY -> allRuns.filter { isSameDay(it.recordDate, now) }
            RunFilter.WEEK -> allRuns.filter { isSameWeek(it.recordDate, now) }
            RunFilter.MONTH -> allRuns.filter { isSameMonth(it.recordDate, now) }
            RunFilter.ALL -> allRuns
        }

        // 정렬 추가 (최신순)
        return filtered.sortedByDescending { it.recordDate }
    }

    fun updateFilter(filter: RunFilter) {
        selectedFilter = filter
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