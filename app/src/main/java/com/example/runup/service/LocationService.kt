package com.example.runup.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.ui.screens.MainActivity
import com.example.runup.ui.util.calculateCalories
import com.example.runup.ui.util.calculatePace
import com.example.runup.ui.util.mapper.TimeMapper.formatDurationMmSs
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : LifecycleService() {

    @Inject lateinit var repository: LocationRepository
    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "running_service_channel"

    private var isObserving = false

    // 콜백을 변수로 빼서 나중에 중단할 수 있게 함
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.lastLocation?.let { location ->
                repository.updateCurrentLocation(GeoPoint(location.latitude, location.longitude))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // ── 🔹 [핵심] 채널은 여기서 딱 한 번만 만듭니다! ── 📍
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_TRACKING") {
            stopLocationUpdates()
            stopSelf()
            return super.onStartCommand(intent, flags, startId)
        }

        // 초기 알림 띄우기
        val notification = buildNotification("0.00km", "00:00", "0'00\"", "0")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        requestLocationUpdates()
        if (!isObserving) {  // ← 중복 방지
            isObserving = true
            observeRunningData()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun observeRunningData() {
        lifecycleScope.launch {
            combine(
                repository.totalDistance,
                repository.totalTime
            ) { distance, time ->
                // 데이터 가공 🔹
                val pace = calculatePace(time, distance)
                val kcal = calculateCalories(distance)
                Log.d("NotiDebug", "알림 업데이트: time=${time}, distance=$distance") // ← 이거 찍히나요?

                // 알림 객체 빌드 (여기선 빌드만 함) 📍
                buildNotification(
                    distance = String.format("%.2fkm", distance / 1000.0),
                    time = formatDurationMmSs(time.toLong()),
                    pace = pace,
                    kcal = kcal
                )
            }.collect { updatedNotification ->
                Log.d("NotiDebug", "notify 호출됨") // ← 이게 찍히나요?
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, updatedNotification)
        }
        }
    }

    // ── 🔹 채널 생성 로직 분리 (onCreate에서 호출) ── 📍
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Running Tracking",
                NotificationManager.IMPORTANCE_LOW // 소리 안 나게 LOW 유지
            ).apply {
                description = "러닝 기록을 실시간으로 표시합니다."
                setShowBadge(false) // 뱃지 미표시 (선택)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    // ── 🔹 알림 빌더 로직 분리 (빌드만 담당) ── 📍
    private fun buildNotification(distance: String, time: String, pace: String, kcal: String): Notification {
        Log.d("NotiDebug", "buildNotification: distance=$distance, time=$time")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RUNUP 기록 중")
            .setContentText("$distance  |  $time  |  $pace  |  ${kcal}")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true) // 📍 업데이트 시 진동/소리 완전 차단 (안정성)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }
}

