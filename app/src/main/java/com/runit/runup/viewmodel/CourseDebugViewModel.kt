package com.runit.runup.viewmodel

import androidx.lifecycle.ViewModel
import com.runit.runup.data.source.local.objectbox.entity.CourseEntity
import com.runit.runup.service.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.objectbox.Box
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CourseDebugViewModel @Inject constructor(
    private val courseBox: Box<CourseEntity>,
    private val syncManager: SyncManager // 기존에 만든 SyncManager 주입
) : ViewModel() {

    private val _courseList = MutableStateFlow<List<CourseEntity>>(emptyList())
    val courseList: StateFlow<List<CourseEntity>> = _courseList.asStateFlow()

    init {
        loadLocalData()
    }

    // 로컬 DB에서 데이터 읽기
    fun loadLocalData() {
        _courseList.value = courseBox.all
    }

    // 새로고침 버튼 클릭 시: 동기화 작업 실행 + 로컬 데이터 다시 읽기
    fun refreshData() {
        syncManager.setupDataSync() // WorkManager 실행
        // 실제로는 WorkManager가 끝나는 시점을 관찰해야 하지만,
        // 우선은 클릭 시 로컬 DB를 다시 긁어오도록 처리합니다.
        loadLocalData()
    }
}