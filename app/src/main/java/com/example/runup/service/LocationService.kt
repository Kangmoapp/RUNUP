package com.example.runup.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.ui.screens.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : LifecycleService() {

    @Inject lateinit var repository: LocationRepository
    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient

    // 콜백을 변수로 빼서 나중에 중단할 수 있게 함
    private val locationCallback = object : LocationCallback() { // 위치가 잡힐때마다 실행하는 행동 지침
        override fun onLocationResult(result: LocationResult) { // 위치 정보가 도착했을 때 실행되는 함수
            super.onLocationResult(result)
            result.lastLocation?.let { location ->
                repository.updateCurrentLocation(GeoPoint(location.latitude, location.longitude)) //이제 실행되면 계속 위치 갱신만 한다
            }
        }
    }

    override fun onCreate() { //RunningViewModel 에서 LocationService 호출하면 실행
        super.onCreate()

        // 구글 서비스로부터 위치 제공 클라이언트를 빌려옴 (장비 챙김)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 중단 액션이 들어오면 수집 멈추고 서비스 종료
        if (intent?.action == "STOP_TRACKING") {
            stopLocationUpdates()
            stopSelf()
            return START_NOT_STICKY
        }
        // 진짜 데이터 수집 시작
        startForeground(1, createNotification()) //상단바에 알림 띄어 시스템이 못 죽이게 함
        requestLocationUpdates() //위치 업데이트
        return super.onStartCommand(intent, flags, startId)
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

    private fun createNotification(): Notification {
        val channelId = "running_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Running Tracking", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("RUNUP 기록 중")
            .setContentText("현재 러닝 경로를 실시간으로 기록하고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}

