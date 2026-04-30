package com.example.runup.ui.screens

import android.graphics.PointF
import android.os.Bundle
import com.example.runup.ui.components.ControlButton
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.White
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.viewmodel.HomeUiState
import com.example.runup.viewmodel.HomeViewModel
import com.naver.maps.map.MapView
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.geometry.LatLng
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.runup.ui.components.DistanceGoalSettingDialog
import com.example.runup.ui.components.PaceGoalSettingDialog
import com.example.runup.ui.theme.TextWhite
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runup.domain.model.Scores
import com.example.runup.ui.theme.PointColor
import com.example.runup.viewmodel.GuideUiState
import com.example.runup.viewmodel.HomeTab
import com.example.runup.viewmodel.RunningUiState
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.overlay.LocationOverlay
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.overlay.PolylineOverlay
import com.example.runup.R
import com.example.runup.ui.components.CategoryDialog
import com.example.runup.ui.components.CourseInfoCard
import com.example.runup.ui.components.LoopSelectionDialog
import com.example.runup.ui.navigation.HomeUi
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.util.calculateCalories
import com.example.runup.ui.util.mapper.DistanceMapper
import com.example.runup.viewmodel.CourseRecommendationUiState

@Preview
@Composable
private fun Preview_HomeContent() {
    HomeContent(
        homeUiState = HomeUiState(homeUi = HomeUi.HOME),
        runningUiState = RunningUiState(),
        guideUiState = GuideUiState(),
        courseRecommendationUiState = CourseRecommendationUiState(),
        onMenuClick = {},
        onRunClick = {},
        stopRunningTracking = {},
        recordRunningCourse = { _ -> }, // (Scores) -> Unit
        onDistanceClick = {},
        onDistanceClose = {},
        onDistanceConfirm = { _ -> },   // (Int) -> Unit
        onPaceClick = {},
        onPaceClose = {},
        onPaceConfirm = { _, _ -> },    // (Int, Int) -> Unit 🔹 여기가 인자 2개 자리!
        onToggleAi = {}                 // 🔹 누락되었던 마지막 인자 추가
    )
}


@Composable
fun HomeScreen(
    onMenuClick:()->Unit,
    viewModel: HomeViewModel = hiltViewModel()
){
    val homeUiState by viewModel.homeUiState.collectAsState()
    val runningUiState by viewModel.runningUiState.collectAsStateWithLifecycle()
    val guideUiState by viewModel.GuideUiState.collectAsState()
    val courseRecommendationUiState by viewModel.courseRecommendationUiState.collectAsState()
    val timer by viewModel.loadingTimer.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ){
        // 초기 위치를 잡는 중
        if (homeUiState.isInitialLoading) {
            LoadingScreen()
        }
        // 위치를 잡았을 때
        else {
            HomeContent(
                homeUiState = homeUiState,
                runningUiState = runningUiState,
                guideUiState = guideUiState,
                courseRecommendationUiState = courseRecommendationUiState,
                onMenuClick = onMenuClick,
                onRunClick = {viewModel.onRunClick()},
                stopRunningTracking = {viewModel.stopRunningTracking()},
                recordRunningCourse = { scores ->
                    viewModel.recordRunningCourse(scores)},
                onDistanceClick = {viewModel.openDistanceDialog()},
                onDistanceClose = {viewModel.closeDistanceDialog()},
                onDistanceConfirm = {viewModel.confirmDistance(it)},
                onPaceClick = {viewModel.openPaceDialog()},
                onPaceClose = {viewModel.closePaceDialog()},
                onPaceConfirm = { minute, second ->
                    viewModel.confirmPace(minute, second)
                },
                onToggleAi = {
                    viewModel.toggleAi()               // 기존 AI 상태 변경
                    viewModel.startBluetoothScan()     // 🌟 스캔 같이 시작!
                }
            )
        }
        if(homeUiState.isLoading){
            LoadingStart(timer)
        }
    }
}

@Composable
private fun HomeContent(
    homeUiState: HomeUiState,
    runningUiState: RunningUiState,
    guideUiState: GuideUiState,
    courseRecommendationUiState: CourseRecommendationUiState,
    onMenuClick:()->Unit,
    onRunClick:()->Unit,
    stopRunningTracking:()->Unit,
    recordRunningCourse:(Scores)->Unit,
    onDistanceClick:()->Unit,
    onDistanceClose:()->Unit,
    onDistanceConfirm:(Int)->Unit,
    onPaceClick:()->Unit,
    onPaceClose:()->Unit,
    onPaceConfirm:(Int,Int)->Unit,

    onToggleAi: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
){
    val isPreview = LocalInspectionMode.current

    val density = LocalDensity.current
    val hiddenHeightPx = with(density) { 60.dp.toPx() }
    val collapsedHeightPx = with(density) { 100.dp.toPx() }
    val expandedHeightPx = with(density) { 550.dp.toPx() }

    // 시트 높이 상태
    var sheetHeightPx by remember { mutableFloatStateOf(hiddenHeightPx) }
    // 현재 주소 상태
    val addressUiState by viewModel.addressUiState.collectAsState()
    // 러닝 완료 후 결과창 뜬 상태
    var isResultLocked by remember { mutableStateOf(false) }


    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ){
        Box(
            modifier = Modifier.fillMaxSize()
        ){
            if(isPreview){
                FakeMap(modifier = Modifier.fillMaxSize())
            }
            else{
                homeUiState.currentLocation?.let { geoPoint ->
                    MapViewContainer(
                        cameraPosition = LatLng(geoPoint.latitude, geoPoint.longitude),
                        recommendCameraLocation = courseRecommendationUiState.cameraLocation,
                        bearing = homeUiState.currentBearing,
                        homeUi = homeUiState.homeUi,
                        latLngList = runningUiState.latLngList,

                        guidePath = guideUiState.guidePath,
                        recommendPath = courseRecommendationUiState.recommendedCourses
                            .getOrNull(courseRecommendationUiState.courseIndex)
                            ?.path?.points?.map { LatLng(it.latitude, it.longitude) } ?: emptyList(),
                        selectedCoursePath = homeUiState.selectedRecommendCourse
                            ?.path?.points?.map { LatLng(it.latitude, it.longitude) } ?: emptyList(),
                        destinationMarkerPos = guideUiState.destinationMarker,
                        guideDistance = guideUiState.guideDistance,
                        guideDuration = guideUiState.guideDuration,
                        isTrackingMode = guideUiState.isTrackingMode,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ){
                Box(
                    modifier = Modifier.fillMaxWidth()
                ){
                    // 🔹 [왼쪽 상단] 현재 주소 표시
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 35.dp, start = 18.dp) // 👈 start = 18.dp 추가하여 MenuBtn과 대칭
                            .clip(RoundedCornerShape(8.dp)) // 클릭 이펙트가 사각형으로 딱딱하지 않게 추가
                            .padding(4.dp) // 클릭 가능한 영역을 시각적 요소보다 살짝 더 넓게 확보
                    ) {
                        Text(
                            text = "현재 위치",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.5f), // 그림자 색상
                                    offset = Offset(2f, 2f),               // 그림자 위치
                                    blurRadius = 4f                        // 퍼짐 정도
                                )
                            )
                        )
                        Text(
                            text = addressUiState?.fullAddress ?: "위치 확인 중...",
                            color = PointColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.5f), // 그림자 색상
                                    offset = Offset(2f, 2f),               // 그림자 위치
                                    blurRadius = 4f                        // 퍼짐 정도
                                )
                            ),
                            maxLines = 1, // 한 줄로 제한
                        )
                    }
                    MenuBtn(
                        modifier = Modifier.align(Alignment.TopEnd),
                        onMenuClick = onMenuClick
                    )

                }
                AIStatusOverlay(
                    isAiEnabled = homeUiState.isAiEnabled,
                    postureLabel = homeUiState.currentPostureLabel,
                    leftBleState = homeUiState.leftBleState,
                    rightBleState = homeUiState.rightBleState,
                    onToggle = onToggleAi,
                    modifier = Modifier
                        .padding(top = 20.dp)
                )

                if (homeUiState.homeUi == HomeUi.RECOMMEND) {
                    val currentCourse = courseRecommendationUiState.recommendedCourses
                        .getOrNull(courseRecommendationUiState.courseIndex)

                    currentCourse?.let { course ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, top = 12.dp), // 👈 왼쪽 여백 18dp로 주소창과 라인 맞춤
                            contentAlignment = Alignment.TopStart // 👈 왼쪽 정렬!
                        ) {
                            CourseInfoCard(
                                uiState = courseRecommendationUiState,
                                course = course
                            )
                        }
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
            ){
                BottomSection(
                    selectedTab = homeUiState.selectedTab,
                    onTabSelect = { tab ->
                        viewModel.selectTab(tab)

                        // 🔹 탭에 따른 초기 시트 높이 결정
                        val targetHeight = when(tab) {
                            HomeTab.RECOMMEND -> with(density) { 140.dp.toPx() } // 추천 탭 전용 높이
                            else -> collapsedHeightPx // 나머지는 기본 높이
                        }
                        sheetHeightPx = targetHeight
                    },
                    homeUi = homeUiState.homeUi,
                    runningUiState = runningUiState,
                    sheetHeightPx = sheetHeightPx,
                    onHeightChange = { sheetHeightPx = it },
                    isResultLocked = isResultLocked
                ) {
                    when (homeUiState.selectedTab) {

                        HomeTab.RUNNING -> {
                            // 러닝 완료 버튼 누른 후 (바텀 시트 최대 확장)
                            if (sheetHeightPx >= expandedHeightPx - 10f) {
                                RunningResultContent(
                                    runningUiState = runningUiState,
                                    onSave = { b, c, d ->
                                        recordRunningCourse(Scores(b.toDouble(), c.toDouble(), d.toDouble())) // 실제 데이터 저장 로직
                                        sheetHeightPx = hiddenHeightPx // 시트 닫기
                                        isResultLocked = false  // 🔹 저장하면 잠금 해제
                                    },
                                    onSkip = {
                                        viewModel.cancelRunningCourse()
                                        sheetHeightPx = hiddenHeightPx // 저장 없이 닫기
                                        isResultLocked = false
                                    }
                                )
                            }
                            else{
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // 목표 영역
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // [왼쪽] 목표 정보 영역
                                        Row(
                                            modifier = Modifier.wrapContentWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 목표 거리 클릭 영역
                                            Column(
                                                horizontalAlignment = Alignment.Start, // 왼쪽 정렬로 변경
                                                modifier = Modifier.clickable { onDistanceClick() }
                                            ) {
                                                Text(text = "목표 거리", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                                                Text(
                                                    text = "${homeUiState.goalDistance.toDouble() / 1000}km",
                                                    color = PointColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // 목표 페이스 클릭 영역
                                            Column(
                                                horizontalAlignment = Alignment.Start, // 왼쪽 정렬로 변경
                                                modifier = Modifier.clickable { onPaceClick() }
                                            ) {
                                                Text(text = "목표 페이스", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                                                Text(
                                                    text = "${homeUiState.goalPace / 60}'${homeUiState.goalPace % 60}\"",
                                                    color = PointColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // [오른쪽] 제어 버튼 영역
                                        Row(
                                            modifier = Modifier.wrapContentWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (homeUiState.homeUi == HomeUi.HOME) {
                                                ControlButton(text = "Run", color = PointColor) { onRunClick() }
                                            } else {
                                                val pauseOrResumeText = if (runningUiState.isTracking) "일시 정지" else "재개"
                                                ControlButton(text = pauseOrResumeText, color = Color.Gray) { stopRunningTracking() }
                                                ControlButton(text = "완료", color = Color.Red) {
                                                    stopRunningTracking()
                                                    sheetHeightPx = expandedHeightPx
                                                    isResultLocked = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HomeTab.RECOMMEND -> {
                            val selectedCourse = homeUiState.selectedRecommendCourse

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp)
                            ) {
                                // ── 🔹 상황 1: 아직 코스 추천을 받지 않은 상태 (설정 화면) ── 📍
                                if (selectedCourse == null && !courseRecommendationUiState.isRecommendClick) {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(
                                            text = "나에게 맞는 코스 찾기",
                                            color = WhiteTextColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Column(modifier = Modifier.clickable { viewModel.openRecommendDistanceDialog() }) {
                                                Text("목표 거리", color = Gray, fontSize = 11.sp)
                                                Text("${courseRecommendationUiState.goalDistance / 1000.0}km", color = PointColor, fontWeight = FontWeight.Bold)
                                            }
                                            Column(modifier = Modifier.clickable { viewModel.openRecommendLoopDialog() }) {
                                                Text("계산 방법", color = Gray, fontSize = 11.sp)
                                                Text(if(courseRecommendationUiState.isLoop) "왕복" else "편도", color = PointColor, fontWeight = FontWeight.Bold)
                                            }
                                            Column(modifier = Modifier.clickable { viewModel.openRecommendSortDialog() }) {
                                                Text("정렬 기준", color = Gray, fontSize = 11.sp)
                                                Text(courseRecommendationUiState.currentSort.label, color = PointColor, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            ControlButton(
                                                text = if(courseRecommendationUiState.isLoading) "찾는 중.." else "코스 추천",
                                                color = PointColor
                                            ) { viewModel.onSearchClick() }
                                        }
                                    }
                                }

                                // ── 🔹 상황 2: 코스 추천 결과가 나온 상태 (브라우징/선택 대기) ── 📍
                                else if (selectedCourse == null && courseRecommendationUiState.isRecommendClick) {
                                    val currentCourse = courseRecommendationUiState.recommendedCourses.getOrNull(courseRecommendationUiState.courseIndex)

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "다시 설정",
                                                color = Gray,
                                                modifier = Modifier.clickable { viewModel.clearRecommendation() }
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                ControlButton("이전", Color.DarkGray) { viewModel.subtractRecommendCourseIndex() }
                                                ControlButton("다음", Color.DarkGray) { viewModel.addRecommendCourseIndex() }
                                                ControlButton("선택", PointColor) {
                                                    currentCourse?.let { viewModel.selectRecommendCourse(it) }
                                                }
                                            }
                                        }
                                    }
                                }

                                // ── 🔹 상황 3: 코스를 최종 선택한 후 (제어 모드) ── 📍
                                // 윤석님이 말씀하신 대로 가장 마지막(밑)에 배치했습니다!
                                else if (selectedCourse != null) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "준비된 코스: ${selectedCourse.originCourse.id}",
                                            color = PointColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween, // 공간을 더 넓게 활용
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                // ── 🔹 [핵심] 길 안내 중인지에 따라 버튼 이름과 기능 변경 ── 📍
                                                if (guideUiState.isGuiding) {
                                                    ControlButton(text = "안내 종료", color = Color.Red) {
                                                        // 파란색 안내선만 제거하고, 코스(노란색)는 그대로 유지!
                                                        viewModel.clearNavigation()
                                                    }
                                                } else {
                                                    ControlButton(text = "경로 안내", color = PointColor) {
                                                        // 시작점까지의 파란색 안내선 생성
                                                        val firstPoint = selectedCourse.path.points.first()
                                                        viewModel.startNavigation(LatLng(firstPoint.latitude, firstPoint.longitude))
                                                    }
                                                }

                                                // 따라가기 버튼
                                                val trackingText = if (guideUiState.isTrackingMode) "추적 중" else "따라가기"
                                                ControlButton(text = trackingText, color = Color.DarkGray) {
                                                    viewModel.toggleTrackingMode()
                                                }
                                            }

                                            // [코스 취소]: 코스(노란색) 자체를 아예 지도에서 지우고 초기화
                                            Text(
                                                text = "코스 취소",
                                                color = Gray,
                                                fontSize = 13.sp,
                                                modifier = Modifier
                                                    .clickable {
                                                        viewModel.clearSelectedCourse()
                                                        sheetHeightPx = collapsedHeightPx
                                                    }
                                                    .padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        HomeTab.COURSE -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // [왼쪽] 경로 정보 영역
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (!guideUiState.isGuiding) {
                                        Text(text = "목적지", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                                        Text(
                                            text = "course 14", // 예시 명칭
                                            color = PointColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        // 경로 안내 중일 때 정보 표시 (거리, 시간)
                                        val km = String.format("%.1f", guideUiState.guideDistance / 1000f)
                                        val min = guideUiState.guideDuration / 1000 / 60

                                        Text(text = "예상 경로 정보", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                                        Text(
                                            text = "${km}km (${min}분 소요)",
                                            color = PointColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // [오른쪽 제어 버튼 영역]
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!guideUiState.isGuiding) {
                                        ControlButton(text = "경로 안내", color = PointColor) {
                                            viewModel.startNavigation(LatLng(35.88790968249425, 128.61171841999786))
                                        }
                                    } else {
                                        // 🔹 따라가기 버튼 추가
                                        val trackingText = if (guideUiState.isTrackingMode) "추적 중" else "따라가기"
                                        val trackingColor = if (guideUiState.isTrackingMode) PointColor else Color.Gray

                                        ControlButton(text = trackingText, color = trackingColor) {
                                            viewModel.toggleTrackingMode()
                                        }

                                        ControlButton(text = "안내 종료", color = Color.Red) {
                                            viewModel.clearNavigation()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (homeUiState.showDistanceDialog) {
            DistanceGoalSettingDialog(
                range = 0..100,
                startNumber = (homeUiState.goalDistance/100 + 1),
                onConfirm = onDistanceConfirm,
                onDismiss = onDistanceClose
            )
        }
        else if (homeUiState.showPaceDialog) {
            PaceGoalSettingDialog(
                rangeMinutes = 0..20,
                rangeSeconds = 0..59,
                startMinute = (homeUiState.goalPace/60 + 1),
                startSecond = (homeUiState.goalPace%60 + 1),
                onConfirm = onPaceConfirm,
                onDismiss = onPaceClose
            )
        }

        // 2. [추가] 코스 추천 전용 설정 다이얼로그들 🚀
        if (courseRecommendationUiState.showDistanceDialog) {
            DistanceGoalSettingDialog(
                range = 0..100,
                startNumber = (courseRecommendationUiState.goalDistance / 100 + 1),
                onConfirm = { viewModel.confirmRecommendDistance(it) }, // 추천 전용 함수 호출
                onDismiss = { viewModel.closeRecommendDistanceDialog() }
            )
        }

        if (courseRecommendationUiState.showSortDialog) {
            CategoryDialog(
                currentSort = courseRecommendationUiState.currentSort,
                onConfirm = { viewModel.confirmRecommendSort(it) },
                onDismiss = { viewModel.closeRecommendSortDialog() }
            )
        }

        if (courseRecommendationUiState.showLoop) {
            // 왕복/편도 선택 다이얼로그 (간단하게 AlertDialog 등으로 구현 가능)
            LoopSelectionDialog(
                isLoop = courseRecommendationUiState.isLoop,
                onSelect = { viewModel.selectRecommendLoop(it) },
                onDismiss = { viewModel.closeRecommendLoopDialog() }
            )
        }
    }
}


@Composable
private fun BottomSection(
    modifier: Modifier = Modifier,
    selectedTab: HomeTab,
    onTabSelect: (HomeTab) -> Unit,
    homeUi: HomeUi,            // 추가
    runningUiState: RunningUiState, // 추가
    sheetHeightPx: Float,
    onHeightChange: (Float) -> Unit,
    isResultLocked: Boolean,
    content: @Composable () -> Unit // 탭에 따른 내용
) {
    val density = LocalDensity.current
    // 네이버 지도 스타일 높이 설정
    val navBarHeight = 80.dp // 하단 메뉴바 높이

    // 바텀 시트 높이
    val hiddenHeightPx = with(density) { 60.dp.toPx() } // 시트가 아예 내려가 있는 상태 (처음)
    val collapsedHeightPx = with(density) { 100.dp.toPx() } // 메뉴 클릭 시 올라오는 높이
    val recommendTabHeightPx = with(density) { 140.dp.toPx() } // 추천 탭 기본 (조금 더 높게) 🚀
    val expandedHeightPx = with(density) { 550.dp.toPx() }  // 최대로 올렸을 때 높이

    val animatedHeight by animateDpAsState(targetValue = with(density) { sheetHeightPx.toDp() })
    // 최대 바텀 시트 높이
    val maxAllowedHeight = when (selectedTab) {
        HomeTab.RECOMMEND -> recommendTabHeightPx // 추천 탭은 설정/결과/안내 모두 이 높이 유지
        HomeTab.COURSE -> expandedHeightPx
        HomeTab.RUNNING -> if (homeUi == HomeUi.HOME && runningUiState.totalDistance > 0) expandedHeightPx else collapsedHeightPx
        else -> collapsedHeightPx
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 정보창 영역 (시트 바로 위에 부착되어 함께 이동) ---
        if ((homeUi == HomeUi.RUN) &&  sheetHeightPx < expandedHeightPx - 10f) {
            val runningPace = if (runningUiState.totalDistance < 100.0) 0.0
            else (runningUiState.totalTime / runningUiState.totalDistance) * 1000

            Column(
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 10.dp) // 시트와의 간격
                    .background(BackGroudColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // 각 항목 간격
            ) {
                // 1. 거리 정보
                Column {
                    Text(text = "거리", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(
                        text = "${runningUiState.totalDistance.toInt()}m",
                        color = PointColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }

                // 2. 시간 정보
                Column {
                    Text(text = "시간", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(
                        text = "${runningUiState.totalTime / 60}분 ${runningUiState.totalTime % 60}초",
                        color = PointColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }

                //3. 현재 페이스
                Column {
                    Text(text = "페이스", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(
                        text = "${(runningPace / 60).toInt()}'${(runningPace % 60).toInt()}\"",
                        color = PointColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }

                // 4. 칼로리 정보
                Column {
                    val caloriesValue = calculateCalories(runningUiState.totalDistance)
                    Text(text = "칼로리", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(
                        text = caloriesValue,
                        color = PointColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        // 드래그 가능한 바텀 시트
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(BackGroudColor)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (!isResultLocked) {  // 🔹 lock이면 무시
                            onHeightChange((sheetHeightPx - delta).coerceIn(hiddenHeightPx, maxAllowedHeight))
                        }
                    },
                    enabled = true,
                    onDragStopped = {
                        if (!isResultLocked) {  // 🔹 lock이면 snap도 무시
                            val finalHeight = when {
                                (selectedTab == HomeTab.COURSE || selectedTab == HomeTab.RECOMMEND) &&
                                        sheetHeightPx > (collapsedHeightPx + expandedHeightPx) / 2 -> expandedHeightPx

                                // 2. 추천 탭에서 기본 높이 유지 (170dp 근처일 때) 🚀
                                selectedTab == HomeTab.RECOMMEND &&
                                        sheetHeightPx > (collapsedHeightPx + recommendTabHeightPx) / 2 -> recommendTabHeightPx

                                selectedTab == HomeTab.RUNNING && (homeUi == HomeUi.HOME) && runningUiState.totalDistance > 0 &&
                                        sheetHeightPx > (collapsedHeightPx + expandedHeightPx) / 2 -> expandedHeightPx

                                // 일반 접힘 상태
                                sheetHeightPx > (hiddenHeightPx + collapsedHeightPx) / 2 -> collapsedHeightPx

                                // 아예 숨김 상태
                                else -> hiddenHeightPx
                            }
                            onHeightChange(finalHeight)
                        }
                    }
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 핸들러
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(40.dp, 4.dp)
                        .background(Color.Gray.copy(0.5f), RoundedCornerShape(2.dp))
                )

                // 실제 컨텐츠
                if (sheetHeightPx > hiddenHeightPx + 10f) {
                    content()
                }
            }
        }

        // 2. 하단 네비게이션 바 (구분선 포함)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackGroudColor)
        ) {
            // 주황색 상단 구분선
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PointColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarHeight)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val menuItems = listOf(
                    HomeTab.RUNNING to "러닝",
                    HomeTab.RECOMMEND to "코스 추천",
                    HomeTab.COURSE to "경로 표시"
                )

                menuItems.forEach { (tab, title) ->
                    // weight(1f)를 주어 모든 칸이 동일한 너비를 가짐
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                if (!isResultLocked) { // 👈 평가 중에는 탭 클릭 무시
                                    onTabSelect(tab)
                                    //onHeightChange(collapsedHeightPx)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (selectedTab == tab) PointColor else WhiteTextColor,
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeComponent(
    textTop: String,
    textTopValue:String,
    textBottom: String,
    textBottomValue:String,
    onPaceClick:()->Unit,
    onDistanceClick:()->Unit,
    modifier: Modifier = Modifier
        .padding(top = 35.dp, start = 18.dp, end = 18.dp)
){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .wrapContentSize()
    ){
        InfoBtn(
            texttop = textTop,
            textbottom = textTopValue,
            onClick = onPaceClick,
            modifier = Modifier
                .height(130.dp)
                .weight(1f)
        )
        Box(
            modifier = Modifier
                .background(color = White, shape = RoundedCornerShape(20.dp))
                .width(1.dp)
                .height(120.dp)
        )
        InfoBtn(
            texttop = textBottom,
            textbottom = textBottomValue,
            onClick = onDistanceClick,
            modifier = Modifier
                .height(130.dp)
                .weight(1f)
        )
    }
}

@Composable
private fun HomeExpandedContent(
    text: String,
    onClick:()->Unit
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(top = 30.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(60.dp)
                .width(300.dp)
                .background(color = White, shape = RoundedCornerShape(5.dp))
                .clickable { onClick() }
        ){
            Text(
                text = text,
                color = TextBlack,
                fontSize = 30.sp
            )
        }
    }
}

@Composable
private fun InfoBtn(
    texttop: String,
    textbottom:String,
    onClick:()->Unit,
    fontsize: TextUnit = 25.sp,
    modifier:Modifier = Modifier
        .fillMaxSize()
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onClick)
    ){
        Text(
            text = texttop,
            fontSize = fontsize,
            color = TextWhite,
            modifier = Modifier
        )
        Text(
            text = textbottom,
            fontSize = fontsize,
            color = TextWhite,
            modifier = Modifier
        )
    }
}

@Composable
private fun MapViewContainer(
    cameraPosition:LatLng,
    recommendCameraLocation: LatLng?,
    bearing: Float = 0.0f, // 추가
    homeUi: HomeUi = HomeUi.RUN,
    latLngList: List<LatLng> = emptyList(),   // 지금까지 나의 러닝 경로 (러닝 모드)
    guidePath: List<LatLng> = emptyList(),    // 코스 시작점까지의 안내 경로
    recommendPath: List<LatLng> = emptyList(), // 추천된 코스 경로 (추천 모드)
    selectedCoursePath: List<LatLng> = emptyList(),
    destinationMarkerPos: LatLng? = null,     // 🔹 추가: 목적지 마커

    guideDistance: Int = 0,    // 🔹 추가: 미터 단위 거리
    guideDuration: Long = 0L,  // 🔹 추가: 밀리초 단위 시간
    isTrackingMode: Boolean = false,
    modifier:Modifier = Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val density = LocalDensity.current

    // 🔹 [핵심] 처음 지도가 켜졌을 때만 순간이동을 하기 위한 플래그
    var isFirstLoad by remember { mutableStateOf(true) }

    val locationSource = remember {
        com.naver.maps.map.util.FusedLocationSource(context as android.app.Activity, 1000)
    }


    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    // 방법 1: 일반 선
    val polyline = remember { PolylineOverlay() }

    // 방법 2: 좀 더 "경로"처럼 보이는 선
    // val pathOverlay = remember { PathOverlay() }

    val marker = remember { Marker() }

    // ── 🔹 추천 코스 전용 오버레이 설정 ── 📍
    val recommendPathOverlay = remember {
        PathOverlay().apply {
            color = Color.Cyan.toArgb() // 추천 코스는 하늘색으로 구분
            outlineColor = Color.Black.toArgb()
            width = with(density) { 8.dp.toPx() }.toInt()
            outlineWidth = with(density) { 2.dp.toPx() }.toInt()

            // 화살표 패턴 추가
            patternImage = OverlayImage.fromResource(R.drawable.arrow_path)
            patternInterval = with(density) { 20.dp.toPx() }.toInt()
        }
    }

    // ── 🔹 최종 선택된 코스 전용 오버레이 추가 ── 📍
    val selectedPathOverlay = remember {
        PathOverlay().apply {
            color = Color.Yellow.toArgb() // 선택된 코스는 우리 앱의 포인트 컬러인 노란색으로! 💛
            outlineColor = Color.Black.toArgb()
            width = with(density) { 10.dp.toPx() }.toInt()
            patternImage = OverlayImage.fromResource(R.drawable.arrow_path)
        }
    }

    // 🔹 안내 경로용 오버레이 (새로 추가)
    val guidePathOverlay = remember {
        PathOverlay().apply {
            // 🔹 네이버 스타일: 진한 하늘색 테두리 + 밝은 하늘색 내부
            color = PointColor.toArgb() // 내부 색상 (노란색 )
            outlineColor = Color.Black.toArgb() // 테두리 색상 (검은색)
            width = with(density) { 12.dp.toPx() }.toInt() // 전체 너비
            outlineWidth = with(density) { 2.dp.toPx() }.toInt() // 테두리 너비

            // 🔹 핵심: 실제 네이버 지도 같은 화살표 패턴 추가
            patternImage = OverlayImage.fromResource(
                R.drawable.arrow_path
            )
            patternInterval = with(density) { 20.dp.toPx() }.toInt() // 화살표 간격
        }
    }

    // 🔹 목적지 마커
    val destMarker = remember {
        Marker().apply {
            icon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_marker_icon_blue)
            // 🔹 3. 캡션 너비 등도 density 스코프 안에서 계산
            density.run {
                captionRequestedWidth = 100.dp.toPx().toInt()
            }
            captionTextSize = 14f
        }
    }

    // 🔹 경로 정보 말풍선용 InfoWindow
    val infoWindow = remember {
        com.naver.maps.map.overlay.InfoWindow().apply {
            adapter = object : com.naver.maps.map.overlay.InfoWindow.DefaultTextAdapter(context) {
                override fun getText(infoWindow: com.naver.maps.map.overlay.InfoWindow): CharSequence {
                    val km = String.format("%.1f", guideDistance / 1000f)
                    val min = guideDuration / 1000 / 60

                    return "${km}km (${min}분)"
                }
            }
            // 말풍선 디자인 살짝 조정
            alpha = 0.9f
        }
    }

    // 🔹 말풍선을 고정할 투명 마커
    val anchorMarker = remember {
        Marker().apply {
            icon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_location_overlay_sub_icon_arrow)
            alpha = 0f // 마커 자체는 투명하게
            width = 1
            height = 1
        }
    }

    DisposableEffect(lifecycleOwner) {

        val observer = object : DefaultLifecycleObserver {

            override fun onStart(owner: LifecycleOwner) {
                mapView.onStart()
            }

            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
            }

            override fun onStop(owner: LifecycleOwner) {
                mapView.onStop()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                mapView.onDestroy()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // 오버레이 제거
            marker.map = null
            polyline.map = null
            guidePathOverlay.map = null
            destMarker.map = null
            // pathOverlay.map = null
        }
    }

    AndroidView(
        factory = {
            mapView
        },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { naverMap: NaverMap ->
                // 지도에 위치 소스 연결
                if (naverMap.locationSource == null) {
                    naverMap.locationSource = locationSource
                }

                // 🔹 [핵심 추가] 첫 로딩 시 애니메이션 없이 순간이동
                if (isFirstLoad) {
                    // CameraUpdate.scrollTo()는 애니메이션 없이 즉시 좌표로 이동
                    val initialCamera = CameraUpdate.toCameraPosition(CameraPosition(cameraPosition, 18.0))
                    naverMap.moveCamera(initialCamera)

                    isFirstLoad = false // 이동 후 플래그를 꺼서 다음부터는 애니메이션이 작동하게 함
                    return@getMapAsync // 첫 프레임에서는 여기서 종료하여 아래 중복 이동을 방지
                }

                // 트래킹 모드 설정 (현재 지도 모드와 위젯 상태가 다를 때만 업데이트)
                val targetMode = if (isTrackingMode) LocationTrackingMode.Face else LocationTrackingMode.None
                if (naverMap.locationTrackingMode != targetMode) {
                    naverMap.locationTrackingMode = targetMode

                    if (isTrackingMode) {
                        // 추적 모드를 켜는 순간에 카메라를 내 위치/방향으로 강제 세팅
                        naverMap.moveCamera(
                            CameraUpdate.toCameraPosition(
                                CameraPosition(cameraPosition, 18.0, 0.0, bearing.toDouble())
                            ).animate(CameraAnimation.Easing)
                        )
                        return@getMapAsync
                    }
                }

                if (isTrackingMode) {
                    // 🔹 따라가기 모드일 때는 시스템이 위치를 추적하므로 수동 이동 건너뜀
                    naverMap.locationOverlay.isVisible = true
                } else {
                    // 🔹 따라가기 모드가 아닐 때: 내 위치 오버레이 수동 설정
                    naverMap.locationOverlay.apply {
                        isVisible = true
                        position = cameraPosition
                        setBearing(bearing)
                        subIcon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_location_overlay_sub_icon_arrow)
                    }

                    // ── 🔹 카메라 이동 우선순위 결정 ── 📍
                    when {
                        // 1순위: 경로 안내(내비게이션) 중일 때 (시작점까지 찾아가는 중)
                        guidePath.size >= 2 -> {
                            val bounds = LatLngBounds.Builder().apply {
                                guidePath.forEach { include(it) }
                            }.build()
                            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 150).animate(CameraAnimation.Easing, 1500))
                        }

                        //
                        // 2순위: homeUi가 HOME이더라도 selectedRecommendCourse가 있으면 코스를 우선적으로 비춤
                        selectedCoursePath.size >= 2 -> { // ── 🔹 리스트가 비어있지 않은지 확인 📍
                            val boundsBuilder = LatLngBounds.Builder()

                            // 1. 현재 내 위치 포함
                            boundsBuilder.include(cameraPosition)

                            // 2. ── 🔹 넘겨받은 리스트(selectedCoursePath)를 직접 순회 ── 📍
                            selectedCoursePath.forEach { latLng ->
                                boundsBuilder.include(latLng)
                            }

                            try {
                                val bounds = boundsBuilder.build()
                                // 3. 모든 지점이 포함되도록 카메라 업데이트 생성
                                // Padding 200: 왼쪽 상단 카드에 가려지지 않게 넉넉히 여백 부여
                                val cameraUpdate = CameraUpdate.fitBounds(bounds, 200)
                                    .animate(CameraAnimation.Easing, 1000)

                                naverMap.moveCamera(cameraUpdate)
                            } catch (e: Exception) {
                                // 혹시 모를 에러 발생 시 리스트의 첫 번째 좌표로 이동하는 방어 로직
                                val fallbackTarget = selectedCoursePath.first()
                                naverMap.moveCamera(
                                    CameraUpdate.toCameraPosition(CameraPosition(fallbackTarget, 15.5))
                                        .animate(CameraAnimation.Easing, 1000)
                                )
                            }
                        }

                        // 3순위: 코스 추천 브라우징 모드일 때 (이전/다음 버튼 누르며 구경 중)
                        homeUi == HomeUi.RECOMMEND && recommendCameraLocation != null -> {
                            naverMap.moveCamera(
                                CameraUpdate.toCameraPosition(CameraPosition(recommendCameraLocation, 16.0))
                                    .animate(CameraAnimation.Easing, 1000)
                            )
                        }

                        // 4순위: 일반 홈 화면 (찜한 코스도 없고, 구경 중도 아닐 때 -> 나를 비춤)
                        homeUi == HomeUi.HOME -> {
                            naverMap.moveCamera(
                                CameraUpdate.toCameraPosition(CameraPosition(cameraPosition, 18.0))
                                    .animate(CameraAnimation.Easing, 1200)
                            )
                        }

                        homeUi == HomeUi.RUN -> {
                            naverMap.moveCamera(
                                CameraUpdate.toCameraPosition(
                                    CameraPosition(cameraPosition, 18.0, 0.0, bearing.toDouble())
                                )
                                    .pivot(PointF(0.5f, 0.65f))
                                    .animate(CameraAnimation.Easing, 1200)
                            )
                        }
                    }
                }


                // 코스 그리는 부분 (내 러닝 코스, 경로 코스, 추천 코스)

                if (latLngList.size >= 2) {
                    polyline.coords = latLngList
                    polyline.width = 10
                    polyline.color = PointColor.toArgb()
                    polyline.map = naverMap
                } else {
                    polyline.map = null
                }

                // 경로 가이드 모드
                if (guidePath.size >= 2) {
                    guidePathOverlay.coords = guidePath
                    guidePathOverlay.map = naverMap

                    // 말풍선 어댑터 갱신
                    infoWindow.adapter = object : com.naver.maps.map.overlay.InfoWindow.DefaultTextAdapter(context) {
                        override fun getText(infoWindow: com.naver.maps.map.overlay.InfoWindow): CharSequence {
                            val km = String.format("%.1f", guideDistance / 1000f)
                            val min = guideDuration / 1000 / 60
                            return "${km}km (${min}분)"
                        }
                    }

                    // 말풍선 위치 업데이트 및 유지
                    val middleIndex = guidePath.size / 2
                    anchorMarker.position = guidePath[middleIndex]
                    anchorMarker.map = naverMap
                    infoWindow.open(anchorMarker)
                } else {
                    // 경로 데이터가 아예 없을 때만 지웁니다.
                    guidePathOverlay.map = null
                    anchorMarker.map = null
                    infoWindow.close()
                }

                // 목적지 마커 표시
                destinationMarkerPos?.let {
                    destMarker.position = it
                    destMarker.map = naverMap
                    destMarker.captionText = "목적지"
                } ?: run {
                    destMarker.map = null
                }

                // ── 🔹 추천 경로(Path) 그리기 로직 ── 📍
                if (homeUi == HomeUi.RECOMMEND && recommendPath.size >= 2) {
                    recommendPathOverlay.coords = recommendPath
                    recommendPathOverlay.map = naverMap
                } else {
                    recommendPathOverlay.map = null
                }

                if (selectedCoursePath.size >= 2) {
                    selectedPathOverlay.coords = selectedCoursePath
                    selectedPathOverlay.map = naverMap
                } else {
                    selectedPathOverlay.map = null
                }
            }
        }
    )
}


@Composable
private fun MenuBtn(    // 우측 상단 메튜 버튼
    modifier:Modifier,
    onMenuClick:()->Unit
){
    Box(
        modifier = modifier
            .wrapContentSize()
            .padding(top = 35.dp, end = 18.dp)
    ){
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(color = BackGroudColor, shape = RoundedCornerShape(5.dp))
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ){
            Icon(
                Icons.Default.Menu,
                "메뉴",
                tint = WhiteTextColor,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun FakeMap(
    modifier:Modifier = Modifier
){

    Box(
        modifier = modifier.background(White),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "Map Preview Placeholder",
            color = TextBlack
        )
    }
}

@Composable
private fun LoadingStart(
    timeNumber: Int,
) {
    // 🔹 Surface 대신 Box를 사용하고 배경색을 투명하게 설정
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f)), // 👈 지도가 살짝 어두워지면 숫자가 더 잘 보여요
        contentAlignment = Alignment.Center
    ) {
        Box {
            // 1. 외곽선 (주황색)
            Text(
                text = timeNumber.toString(),
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    fontSize = 160.sp,
                    color = PointColor,
                    drawStyle = Stroke(width = 15f), // 외곽선 두께 조절
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(4f, 4f),
                        blurRadius = 8f
                    )
                )
            )

            // 2. 내부 채우기 (흰색)
            Text(
                text = timeNumber.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 160.sp,
                color = PointColor,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(4f, 4f),
                        blurRadius = 8f
                    )
                )
            )
        }
    }
}


@Composable
fun AIStatusOverlay(
    isAiEnabled: Boolean,
    postureLabel: String,
    leftBleState: String,
    rightBleState: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            // 텍스트가 추가되었으니 폭을 살짝 넓혀줍니다. (0.85f -> 0.95f)
            .fillMaxWidth(0.95f)
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = if (isAiEnabled) Color(0xCC311B92) else Color(0xCC757575),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 왼쪽: 현재 AI 분석 결과 (팔자 걸음 등)
            Text(
                text = postureLabel,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f) // 텍스트가 길어져도 우측 UI를 밀어내지 않게 방어
            )

            // 🌟 2. 오른쪽: 블루투스 상태 2줄 + 스피커 버튼 묶음
            Row(verticalAlignment = Alignment.CenterVertically) {

                // 블루투스 상태 텍스트 (위: 왼쪽, 아래: 오른쪽)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(text = leftBleState, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = rightBleState, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 스피커 버튼
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isAiEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "AI Voice Toggle",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun RunningResultContent(
    runningUiState: RunningUiState,
    onSave: (Float, Float, Float) -> Unit,
    onSkip: () -> Unit
) {
    var brightnessScore by remember { mutableFloatStateOf(0f) }
    var crowdedScore by remember { mutableFloatStateOf(0f) }
    var difficultyScore by remember { mutableFloatStateOf(0f) }

    val runningPace = if (runningUiState.totalDistance < 100.0) 0.0
    else (runningUiState.totalTime / runningUiState.totalDistance) * 1000

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp), // 🔹 상하 패딩 축소 (24dp -> 12dp)
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 타이틀 섹션 (간격 축소)
        Text(
            "RUN COMPLETE!",
            fontSize = 12.sp, // 🔹 폰트 살짝 축소
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Black,
            color = PointColor
        )
        Text(
            "오늘의 러닝 결과",
            fontSize = 20.sp, // 🔹 24sp -> 20sp
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp)) // 🔹 32dp -> 16dp

        // 🔹 1. 주요 수치 카드 (패딩 및 간격 최적화)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) { // 🔹 20dp -> 16dp
                Row(modifier = Modifier.fillMaxWidth()) {
                    ResultItem("거리", DistanceMapper.formatDistance(runningUiState.totalDistance), Modifier.weight(1f))
                    ResultItem("시간", "${runningUiState.totalTime / 60}:${String.format("%02d", runningUiState.totalTime % 60)}", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp)) // 🔹 24dp -> 16dp
                Row(modifier = Modifier.fillMaxWidth()) {
                    ResultItem("페이스", "${(runningPace / 60).toInt()}'${(runningPace % 60).toInt()}\"", Modifier.weight(1f))
                    ResultItem("칼로리", calculateCalories(runningUiState.totalDistance), Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp)) // 🔹 32dp -> 20dp

        // 🔹 2. 평가 섹션 (이모지 크기 및 간격 축소)
        Text(
            "코스는 어떠셨나요?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(12.dp), // 🔹 16dp -> 12dp
            verticalArrangement = Arrangement.spacedBy(10.dp) // 🔹 16dp -> 10dp
        ) {
            RatingSection("💡 밝기", brightnessScore) { brightnessScore = it }
            RatingSection("👥 붐빔", crowdedScore) { crowdedScore = it }
            RatingSection("⛰️ 난이도", difficultyScore) { difficultyScore = it }
        }

        // 🔹 중요: 버튼이 씹히지 않도록 Spacer를 고정값이 아닌 weight로 조절
        Spacer(modifier = Modifier.height(24.dp))

        // 🔹 3. 하단 버튼 영역 (바텀 시트 하단 여백 확보)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp), // 🔹 하단 기기 네비바 영역 고려
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Skip",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable { onSkip() }
                    .padding(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onSave(brightnessScore, crowdedScore, difficultyScore) },
                colors = ButtonDefaults.buttonColors(containerColor = PointColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp).width(140.dp) // 🔹 버튼 크기 살짝 축소
            ) {
                Text("기록 저장", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
            }
        }
    }
}

@Composable
private fun ResultItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun RatingSection(label: String, score: Float, onScoreChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = WhiteTextColor.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 1..5) {
                val starValue = i * 0.2f
                val isSelected = score >= starValue - 0.01f
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (isSelected) PointColor else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onScoreChange(starValue) }
                )
            }
        }
    }
}
