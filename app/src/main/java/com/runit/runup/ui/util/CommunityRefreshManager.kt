package com.runit.runup.ui.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRefreshManager @Inject constructor() {
    private val _refreshEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshEvent = _refreshEvent.asSharedFlow()

    // 데이터가 변했을 때 호출할 함수
    fun notifyDataChanged() {
        _refreshEvent.tryEmit(Unit)
    }
}