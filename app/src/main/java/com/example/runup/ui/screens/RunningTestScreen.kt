package com.example.runup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.viewmodel.RunningViewModel
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RunningTestScreen(
    viewModel: RunningViewModel = hiltViewModel()
) {
    // 1. ViewModel의 단일 UI 상태 구독
    val uiState by viewModel.uiState.collectAsState()

    // 2. 카메라 상태 설정
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(35.8888, 128.6103), 17f)
    }

    // [중요] 내 위치(uiState.currentLocation)가 업데이트될 때마다 카메라가 따라감
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { latLng ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 지도 영역 ---
        GoogleMap(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
        ) {
            // [실시간 경로 그리기] - uiState.latLngList 사용
            if (uiState.latLngList.size >= 2) {
                Polyline(
                    points = uiState.latLngList,
                    color = Color.Blue,
                    width = 12f,
                    jointType = JointType.ROUND
                )
            }
        }

        // --- 컨트롤 영역 (하단 테스트 패널) ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 상단 정보 표시 (uiState에서 데이터 추출)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("위치: ${String.format("%.7f", uiState.currentLocation?.latitude ?: 0.0)}, ${String.format("%.7f", uiState.currentLocation?.longitude ?: 0.0)}")
                    Text("거리: ${String.format("%.1f", uiState.totalDistance)}m", color = Color.Blue, fontWeight = FontWeight.Bold)
                }
                Text("기록된 노드: ${uiState.latLngList.size}개")
                Text("상태: ${if (uiState.isTracking) "러닝 기록 중 🏃" else "대기 중 🛑"}", fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(12.dp))

                // 1단계: 위치 추적 서비스 (LocationService)
                Text("1. 위치 서비스", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.startCurrentLocationTracking() }, modifier = Modifier.weight(1f)) {
                        Text("GPS 켜기")
                    }
                    Button(
                        onClick = { viewModel.stopCurrentLocationTracking() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("GPS 끄기")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2단계: 러닝 기록 (ViewModel Job 컨트롤)
                Text("2. 러닝 기록", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.startRunningTracking() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isTracking, // 이미 기록 중이면 비활성화
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("러닝 시작")
                    }
                    Button(
                        onClick = { viewModel.stopRunningTracking() },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isTracking, // 기록 중이 아닐 때 비활성화
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("저장 및 종료")
                    }
                }
            }
        }
    }
}
