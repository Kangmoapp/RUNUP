package com.runit.runup.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleSensorManager @Inject constructor() {
    private val _sensorDataFlow = MutableSharedFlow<FloatArray>(extraBufferCapacity = 60)
    val sensorDataFlow: SharedFlow<FloatArray> = _sensorDataFlow.asSharedFlow()

    private var leftRawData = FloatArray(10) { 0f }
    private var rightRawData = FloatArray(10) { 0f }
    private var continuousDataTimer: Timer? = null

    // 외부(블루투스 서비스 등)에서 데이터 갱신 시 호출
    fun updateLeftData(data: FloatArray) { leftRawData = data }
    fun updateRightData(data: FloatArray) { rightRawData = data }

    fun startDataProcessing() {
        continuousDataTimer = Timer()
        continuousDataTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val snapshot = FloatArray(20)
                System.arraycopy(leftRawData, 0, snapshot, 0, 10)
                System.arraycopy(rightRawData, 0, snapshot, 10, 10)

                if (snapshot.any { it != 0f }) {
                    _sensorDataFlow.tryEmit(snapshot)
                }
            }
        }, 0, 40) // 25Hz
    }

    fun stopDataProcessing() {
        continuousDataTimer?.cancel()
        continuousDataTimer = null
    }
}