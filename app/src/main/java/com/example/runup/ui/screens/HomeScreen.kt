package com.example.runup.ui.screens

import android.graphics.PointF
import android.os.Bundle
import com.example.runup.ui.components.ControlButton
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import com.example.runup.ui.components.DistanceGoalSettingDialog
import com.example.runup.ui.components.PaceGoalSettingDialog
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runup.domain.model.Scores
import com.example.runup.ui.theme.PointColor
import com.example.runup.viewmodel.GuideUiState
import com.example.runup.viewmodel.HomeTab
import com.example.runup.viewmodel.RunningUiState
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.overlay.PolylineOverlay
import com.example.runup.R
import com.example.runup.domain.model.SortDirection
import com.example.runup.domain.model.SortType
import com.example.runup.ui.components.AIStatusOverlay
import com.example.runup.ui.components.AiReasonBubble
import com.example.runup.ui.components.CourseInfoCard
import com.example.runup.ui.components.FailMessageBubble
import com.example.runup.ui.components.FakeMap
import com.example.runup.ui.components.LoadingStart
import com.example.runup.ui.components.LoopSelectionDialog
import com.example.runup.ui.components.MenuBtn
import com.example.runup.ui.components.RunningResultContent
import com.example.runup.ui.navigation.CourseProgress
import com.example.runup.ui.navigation.HomeUi
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.util.calculateCalories
import com.example.runup.viewmodel.AiPostureUiState
import com.example.runup.viewmodel.CourseRecommendationUiState
import com.naver.maps.map.overlay.CircleOverlay

@Preview
@Composable
private fun Preview_HomeContent() {
    HomeContent(
        homeUiState = HomeUiState(homeUi = HomeUi.HOME),
        runningUiState = RunningUiState(),
        guideUiState = GuideUiState(),
        aiPostureUiState = AiPostureUiState(),
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
    val aiPostureUiState by viewModel.AiPostureUiState.collectAsState()
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
                aiPostureUiState = aiPostureUiState,
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
    aiPostureUiState: AiPostureUiState,
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

    val navBarHeightPx = with(density) { 80.dp.toPx() } // 네비게이션 바 높이 (px)

    val hiddenHeightPx = with(density) { 60.dp.toPx() }
    val collapsedHeightPx = with(density) { 100.dp.toPx() }
    val selectedCourseTabHeightPx = with(density) { 135.dp.toPx() }
    val recommendTabHeightPx = with(density) { 200.dp.toPx() } // 추천 탭 기본 (조금 더 높게) 🚀
    val expandedHeightPx = with(density) { 550.dp.toPx() }


    // 시트 높이 상태
    var sheetHeightPx by remember { mutableFloatStateOf(hiddenHeightPx) }

    val animatedSheetHeightDp by animateDpAsState(
        targetValue = with(density) { sheetHeightPx.toDp() },
    )
    // 현재 주소 상태
    val addressUiState by viewModel.addressUiState.collectAsState()
    // 러닝 완료 후 결과창 뜬 상태
    var isResultLocked by remember { mutableStateOf(false) }

    // ── 🔹 [추가] 실시간 키보드 높이(px) 상태 📍 ──
    val imeHeightPx = WindowInsets.ime.getBottom(density)

    var isManualMode by remember { mutableStateOf(false) }

    LaunchedEffect(
        homeUiState.selectedTab,
        courseRecommendationUiState.isRecommendClick,
        homeUiState.selectedPath,
        imeHeightPx
    ) {
        val baseHeight = when (homeUiState.selectedTab) {
            // ── 🔹 1. 러닝 탭일 때: 코스 선택 여부 무시하고 무조건 기본 높이 📍 ──
            HomeTab.RUNNING -> collapsedHeightPx // 100dp

            // ── 🔹 2. 코스 추천 탭일 때: 상황별로 높이 결정 📍 ──
            HomeTab.RECOMMEND -> {
                when {
                    // 코스를 최종 선택했거나(상황 3), 브라우징 중일 때(상황 2) -> 135dp
                    homeUiState.selectedPath != null || courseRecommendationUiState.isRecommendClick -> selectedCourseTabHeightPx
                    // 아무것도 안 한 초기 설정 상태(상황 1) -> 200dp
                    else -> recommendTabHeightPx
                }
            }

            // 3. 기타 (MY 탭 등)
            else -> collapsedHeightPx
        }

        // 키보드 대응 로직
        val targetHeight = if (courseRecommendationUiState.isAiMode && imeHeightPx > 0) {
            val pureKeyboardHeight = (imeHeightPx - navBarHeightPx).coerceAtLeast(0f)
            baseHeight + pureKeyboardHeight
        } else {
            baseHeight
        }

        sheetHeightPx = targetHeight
    }


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
                        selectedCoursePath = homeUiState.selectedPath?.points?.map { LatLng(it.latitude, it.longitude) } ?: emptyList(),
                        destinationMarkerPos = guideUiState.destinationMarker,
                        guideDistance = guideUiState.guideDistance,
                        guideDuration = guideUiState.guideDuration,
                        isTrackingMode = guideUiState.isTrackingMode,
                        isManualMode = isManualMode, // 👈 전달
                        onManualModeChange = { isManualMode = it }, // 👈 전달
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
                    isAiEnabled = aiPostureUiState.isAiEnabled,
                    postureLabel = aiPostureUiState.currentPostureLabel,
                    leftBleState = aiPostureUiState.leftBleState,
                    rightBleState = aiPostureUiState.rightBleState,
                    onToggle = onToggleAi,
                    modifier = Modifier
                        .padding(top = 20.dp)
                )

                if (homeUiState.homeUi == HomeUi.RECOMMEND &&
                    !courseRecommendationUiState.isLoading &&      // 1. 로딩이 끝났을 때 📍
                    courseRecommendationUiState.isRecommendClick && // 2. 검색 버튼을 눌러 결과가 나왔을 때
                    courseRecommendationUiState.isFailSearchCourse.isBlank() // 3. 검색 실패가 아닐 때
                ) {
                    val currentCourse = courseRecommendationUiState.recommendedCourses
                        .getOrNull(courseRecommendationUiState.courseIndex)

                    currentCourse?.let { course ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, top = 12.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            CourseInfoCard(
                                uiState = courseRecommendationUiState,
                                course = course
                            )
                        }
                    }
                }
            }
            // ── 🔹 [하단 위젯 모음] 메시지 버블과 내 위치 버튼을 나란히 배치 📍 ──
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    // 바텀시트 높이 + 네비바(80dp) + 기본 여백(16dp)
                    .padding(bottom = with(density) { animatedSheetHeightDp } + 80.dp + 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.Bottom, // 👈 메시지와 버튼의 바닥 높이를 맞춤
                horizontalArrangement = Arrangement.End
            ) {
                // 1. 메시지 영역 (왼쪽 공간을 다 차지하면서 오른쪽 버튼을 밀어냄)
                val failMessage = courseRecommendationUiState.isFailSearchCourse
                val currentCourse = courseRecommendationUiState.recommendedCourses.getOrNull(courseRecommendationUiState.courseIndex)

                Box(
                    modifier = Modifier.weight(1f), // 👈 남은 가로 공간을 다 쓰되 버튼은 침범 안 함
                    contentAlignment = Alignment.BottomEnd // 메시지도 오른쪽 정렬
                ) {
                    if (failMessage.isNotBlank()) {
                        FailMessageBubble(
                            message = failMessage,
                            onClose = { viewModel.clearFailMessage() }
                        )
                    } else if (courseRecommendationUiState.isAiMode && courseRecommendationUiState.isRecommendClick && currentCourse != null && homeUiState.selectedPath == null) {
                        AiReasonBubble(reason = currentCourse.reason)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp)) // 메시지와 버튼 사이 최소 간격

                // 2. 내 위치 토글 버튼 (Target / Free)
                Surface(
                    onClick = { isManualMode = !isManualMode },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black, // 👈 배경은 검은색 유지 (테마에 맞춰 변경 가능)
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            // ── 🔹 요청하신 색상 조건: Target(노란색), Free(흰색) 📍 ──
                            tint = if (isManualMode) Color.White else PointColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxSize()
            ){
                // ── 🔹 [추가] AI 추천 사유 말풍선 배치 📍 ──
                val currentCourse = courseRecommendationUiState.recommendedCourses.getOrNull(courseRecommendationUiState.courseIndex)



                BottomSection(
                    selectedTab = homeUiState.selectedTab,
                    onTabSelect = { tab ->
                        val isCurrentlyRunning = homeUiState.homeUi == HomeUi.RUN
                        val isRunningTabClicked = tab == HomeTab.RUNNING
                        val isAlreadyOnRunningTab = homeUiState.selectedTab == HomeTab.RUNNING

                        if (isCurrentlyRunning && isRunningTabClicked && isAlreadyOnRunningTab) {
                            // 러닝 중인데 러닝 탭을 또 누른 경우: 아무것도 하지 않고 리턴!
                            return@BottomSection
                        }

                        viewModel.selectTab(tab)
                    },
                    homeUi = homeUiState.homeUi,
                    runningUiState = runningUiState,
                    sheetHeightPx = sheetHeightPx,
                    animatedHeightDp = animatedSheetHeightDp,
                    onHeightChange = { sheetHeightPx = it },
                    isResultLocked = isResultLocked,
                    imeHeightPx = imeHeightPx,
                    goalDistance = homeUiState.goalDistance,
                    goalPace = homeUiState.goalPace
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
                            val selectedCourse = homeUiState.selectedPath
                            val selectedCourseName = homeUiState.selectedCourseName
                            val isAiMode = courseRecommendationUiState.isAiMode
                            var aiChatInput by remember { mutableStateOf("") }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp)
                            ) {
                                // ── 🔹 상황 1: 아직 코스 추천을 받지 않은 상태 (설정 화면) ── 📍
                                if (selectedCourse == null && !courseRecommendationUiState.isRecommendClick) {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        // 1. 헤더 (제목 + AI 전환 아이콘)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // [왼쪽] 현재 타이틀
                                            Text(
                                                text = if (isAiMode) "AI 맞춤 코스 추천 ✨" else "나에게 맞는 코스 찾기",
                                                color = WhiteTextColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            // [오른쪽] 모드 전환 버튼 (캡슐형 디자인) 📍
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp)) // 둥근 캡슐 모양
                                                    .background(
                                                        // AI 모드일 때는 필터로 돌아가는 버튼(어두운 회색), 필터 모드일 때는 AI 추천 버튼(보라색)
                                                        if (isAiMode) Color.White.copy(alpha = 0.1f) else Color(0xFF311B92).copy(alpha = 0.8f)
                                                    )
                                                    .clickable { viewModel.toggleAiRecommendMode() }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isAiMode) Icons.Default.FilterList else Icons.Default.AutoAwesome,
                                                    tint = PointColor, // 우리 앱의 시그니처 노란색
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (isAiMode) "정렬 추천" else "AI 추천",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // 2. 공통 설정 영역 (AI 모드에서도 유지됨) 📍
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // [공통] 목표 거리
                                            Column(modifier = Modifier.clickable { viewModel.openRecommendDistanceDialog() }) {
                                                Text("목표 거리", color = Gray, fontSize = 11.sp)
                                                Text("${courseRecommendationUiState.goalDistance / 1000.0}km", color = PointColor, fontWeight = FontWeight.Bold)
                                            }

                                            Spacer(modifier = Modifier.width(24.dp)) // 기존보다 간격을 살짝 더 줌

                                            // [공통] 계산 방법 (왕복/편도)
                                            Column(modifier = Modifier.clickable { viewModel.openRecommendLoopDialog() }) {
                                                Text("계산 방법", color = Gray, fontSize = 11.sp)
                                                Text(if(courseRecommendationUiState.isLoop) "왕복" else "편도", color = PointColor, fontWeight = FontWeight.Bold)
                                            }

                                            // ── 🔹 1. 계산방법과 거리 사이를 확 띄우기 위해 가중치(weight) 사용 📍 ──
                                            Spacer(modifier = Modifier.weight(1f))

                                            // ── 🔹 2 & 3. 코스까지의 거리 레이아웃 수정 📍 ──
                                            Column(
                                                horizontalAlignment = Alignment.End, // 우측 정렬로 변경하여 끝에 붙임
                                                modifier = Modifier.clickable { viewModel.openMaxDistanceDialog() }
                                            ) {
                                                // 회색 라벨 변경
                                                Text("코스까지의 거리", color = Gray, fontSize = 11.sp)

                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    // '최대' 글자는 작게 (10.sp)
                                                    Text(
                                                        text = "최대 ",
                                                        color = PointColor,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    // 선택한 숫자는 기존 크기 유지
                                                    Text(
                                                        text = "${courseRecommendationUiState.maxSearchDistance / 1000.0}km",
                                                        color = PointColor,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        // 3. 가변 영역 (AI 입력창 VS 정렬+버튼) 📍
                                        if (isAiMode) {
                                            // ── 🔹 [AI 모드] 채팅 입력창 (정렬 기준 대신 등장) ──
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp) // 높이 고정
                                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = aiChatInput,
                                                    onValueChange = { aiChatInput = it },
                                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                                    modifier = Modifier.weight(1f),
                                                    decorationBox = { innerTextField ->
                                                        if (aiChatInput.isEmpty()) {
                                                            Text("예) 일청담 근처 밝은 코스 추천해줘!", color = Gray, fontSize = 14.sp)
                                                        }
                                                        innerTextField()
                                                    }
                                                )
                                                IconButton(
                                                    onClick = {
                                                        viewModel.onAiSearchClick(aiChatInput)
                                                        aiChatInput = ""
                                                    },
                                                    // 로딩 중이거나 입력값이 없으면 클릭 방지
                                                    enabled = aiChatInput.isNotBlank() && !courseRecommendationUiState.isLoading
                                                ) {
                                                    if (courseRecommendationUiState.isLoading) {
                                                        // ── 🔹 로딩 중일 때: 인디케이터 📍 ──
                                                        androidx.compose.material3.CircularProgressIndicator(
                                                            modifier = Modifier.size(20.dp), // 아이콘 크기와 동일하게 20dp
                                                            color = PointColor, // 우리 앱의 시그니처 컬러(노란색) 적용!
                                                            strokeWidth = 2.dp
                                                        )
                                                    } else {
                                                        // ── 🔹 평소 상태: 전송(Send) 아이콘 📍 ──
                                                        Icon(
                                                            imageVector = Icons.Default.Send,
                                                            contentDescription = "AI 검색",
                                                            // 입력값이 있을 때만 노란색, 없을 때는 회색
                                                            tint = if (aiChatInput.isNotBlank()) PointColor else Gray,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp), // 높이 살짝 확보
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 1. 가로 정렬 옵션 리스트 칩 (왼쪽 영역)
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    SortType.entries.forEach { sortType ->
                                                        val isSelected = courseRecommendationUiState.currentSort == sortType
                                                        val label = when(sortType) {
                                                            SortType.DISTANCE -> "가까운"
                                                            SortType.BRIGHT -> "밝기"
                                                            SortType.PEOPLE -> "유동인구"
                                                            SortType.DIFFICULTY -> "난이도"
                                                            else -> sortType.label
                                                        }

                                                        Row(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(20.dp))
                                                                .background(if (isSelected) PointColor else Color.White.copy(alpha = 0.05f))
                                                                .clickable { viewModel.confirmRecommendSort(sortType) }
                                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                color = if (isSelected) Color.Black else Gray,
                                                                fontSize = 11.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                            )

                                                            // 📍 거리순이 아닐 때만 방향 화살표 표시
                                                            if (isSelected && sortType != SortType.DISTANCE) {
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Icon(
                                                                    imageVector = if (courseRecommendationUiState.sortDirection == SortDirection.DESCENDING)
                                                                        Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                                                    contentDescription = null,
                                                                    tint = Color.Black,
                                                                    modifier = Modifier
                                                                        .size(14.dp)
                                                                        .clickable { viewModel.toggleSortDirection() } // 📍 클릭 시 오름/내림차순 전환
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(4.dp))

                                                // 2. 정사각형 모양의 코스 추천 버튼 📍
                                                ControlButton(
                                                    text = "코스 추천", // 👈 요청하신 대로 줄바꿈 적용!
                                                    color = PointColor,
                                                    contentColor = Color.Black,
                                                    isLoading = courseRecommendationUiState.isLoading,
                                                    modifier = Modifier
                                                        .size(72.dp) // 72dp 정사각형
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    viewModel.onSearchClick()
                                                }
                                            }
                                        }
                                    }
                                }

                                // ── 🔹 상황 2: 코스 추천 결과가 나온 상태 (브라우징/선택 대기) ── 📍
                                else if (selectedCourse == null && courseRecommendationUiState.isRecommendClick) {
                                    val currentCourse = courseRecommendationUiState.recommendedCourses.getOrNull(courseRecommendationUiState.courseIndex)
                                    val totalCourses = courseRecommendationUiState.recommendedCourses.size

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // 페이지 인디케이터
                                        Row(
                                            modifier = Modifier.padding(bottom = 12.dp), // 버튼들과의 수직 간격
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            repeat(totalCourses) { index ->
                                                val isSelected = index == courseRecommendationUiState.courseIndex
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = if (isSelected) 16.dp else 6.dp, height = 6.dp) // 현재 페이지는 길쭉하게!
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(
                                                            if (isSelected) PointColor else Gray.copy(alpha = 0.3f)
                                                        )
                                                )
                                            }
                                        }

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
                                            text = "선택된 코스: ${selectedCourseName}",
                                            color = PointColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween, // 공간을 더 넓게 활용
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                // 1. [안내 시작/종료] 버튼 (항상 노출)
                                                if (guideUiState.isGuiding) {
                                                    ControlButton(
                                                        text = "안내 종료",
                                                        color = Color.Red,
                                                        contentColor = Color.White
                                                    ) {
                                                        viewModel.clearNavigation()
                                                    }

                                                    // ── 🔹 [핵심 추가] 안내 중일 때만 나타나는 버튼 📍 ──
                                                    val trackingText = if (guideUiState.isTrackingMode) "추적 중" else "따라가기"
                                                    ControlButton(
                                                        text = trackingText,
                                                        color = Color.DarkGray,
                                                        contentColor = Color.White
                                                    ) {
                                                        viewModel.toggleTrackingMode()
                                                    }
                                                } else {
                                                    ControlButton(
                                                        text = "경로 안내",
                                                        color = PointColor,
                                                        contentColor = Color.Black // 노란 배경엔 검정 글씨 📍
                                                    ) {
                                                        val firstPoint = selectedCourse.points.first()
                                                        viewModel.startNavigation(LatLng(firstPoint.latitude, firstPoint.longitude))
                                                    }
                                                }
                                            }
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
                "목표 거리 설정",
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
                "목표 거리 설정",
                range = 0..100,
                startNumber = (courseRecommendationUiState.goalDistance / 100 + 1),
                onConfirm = { viewModel.confirmRecommendDistance(it) }, // 추천 전용 함수 호출
                onDismiss = { viewModel.closeRecommendDistanceDialog() }
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

        if (courseRecommendationUiState.showMaxDistanceDialog) {
            DistanceGoalSettingDialog(
                "최대 코스 추천 범위",
                range = 1..10, // 0.1km ~ 10.0km 범위
                // 500m인 경우 5가 선택되어 0.5km로 표시되도록 계산
                startNumber = (courseRecommendationUiState.maxSearchDistance / 100),
                onConfirm = { kmUnit ->
                    // 다이얼로그에서 선택한 숫자(예: 5)를 받아 500m로 변환하여 저장
                    viewModel.confirmMaxDistance(kmUnit)
                },
                onDismiss = { viewModel.closeMaxDistanceDialog() }
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
    animatedHeightDp: Dp,
    onHeightChange: (Float) -> Unit,
    isResultLocked: Boolean,
    imeHeightPx: Int = 0,
    goalDistance: Int = 0,
    goalPace: Int = 0,
    content: @Composable () -> Unit // 탭에 따른 내용
) {
    val density = LocalDensity.current
    // 네이버 지도 스타일 높이 설정
    val navBarHeight = 80.dp // 하단 메뉴바 높이
    val imeHeightPxFloat = imeHeightPx.toFloat()

    // 바텀 시트 높이
    val hiddenHeightPx = with(density) { 60.dp.toPx() } // 시트가 아예 내려가 있는 상태 (처음)
    val collapsedHeightPx = with(density) { 100.dp.toPx() } // 메뉴 클릭 시 올라오는 높이
    val recommendTabHeightPx = with(density) { 200.dp.toPx() } // 추천 탭 기본 (조금 더 높게) 🚀
    val expandedHeightPx = with(density) { 550.dp.toPx() }  // 최대로 올렸을 때 높이

    // 최대 바텀 시트 높이
    val maxAllowedHeight = when (selectedTab) {
        HomeTab.RECOMMEND -> recommendTabHeightPx + imeHeightPxFloat// 추천 탭은 설정/결과/안내 모두 이 높이 유지
        HomeTab.RUNNING -> if (homeUi == HomeUi.HOME && runningUiState.totalDistance > 0) expandedHeightPx else collapsedHeightPx
        else -> collapsedHeightPx
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if ((homeUi == HomeUi.RUN) && sheetHeightPx < expandedHeightPx - 10f) {
            val runningPace = if (runningUiState.totalDistance < 100.0) 0.0
            else (runningUiState.totalTime / runningUiState.totalDistance) * 1000

            // ── 🔹 색상 결정을 위한 조건 계산 📍 ──
            // 거리 목표: goalDistance(단위: 100m)를 미터 단위로 변환하여 비교
            val isDistanceGoalAchieved = runningUiState.totalDistance >= (goalDistance * 100)
            val distanceColor = if (isDistanceGoalAchieved) Color(0xFF4CAF50) else PointColor // 초록색 또는 기본 노란색

            // 페이스 목표: 페이스는 숫자가 작을수록 빠름 (단위: 초/km)
            // goalPace가 0(설정 안 함)이 아닐 때만 비교 로직 작동
            val paceColor = when {
                goalPace == 0 -> PointColor
                runningPace <= goalPace -> Color(0xFF4CAF50) // 목표보다 빠름 (초록)
                else -> Color(0xFFF44336) // 목표보다 느림 (빨강)
            }

            Column(
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), // 말풍선과 지표창 사이의 간격
                horizontalAlignment = Alignment.Start
            ) {
                // ── 🔹 [1] 말풍선 영역: 이제 지표창 배경(Box) 밖에 위치합니다 📍 ──
                if (runningUiState.courseProgress != CourseProgress.NONE) {
                    val message = when (runningUiState.courseProgress) {
                        CourseProgress.REACHED_END -> "선택한 코스를 완주했습니다! 🎉"
                        CourseProgress.RETURNING -> "선택한 코스를 완주했어요! 다시 돌아가볼까요? 🔄"
                        CourseProgress.BACK_AT_START -> "다시 돌아왔어요! 축하드려요! 🏆"
                        else -> ""
                    }

                    Surface(
                        color = PointColor, // 노란색 배경
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 4.dp // 밖으로 나왔으니 그림자를 살짝 주면 더 입체적입니다 🔹
                    ) {
                        Text(
                            text = message,
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // ── 🔹 [2] 실제 지표창 영역: 기존 배경 로직을 여기로 집중 📍 ──
                Column(
                    modifier = Modifier
                        .background(BackGroudColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. 거리 정보
                    Column {
                        Text(text = "거리", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text(
                            text = "${runningUiState.totalDistance.toInt()}m",
                            color = distanceColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
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

                    // 3. 현재 페이스
                    Column {
                        Text(text = "페이스", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text(
                            text = "${(runningPace / 60).toInt()}'${(runningPace % 60).toInt()}\"",
                            color = paceColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
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
        }
        // 드래그 가능한 바텀 시트
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeightDp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(BackGroudColor)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (!isResultLocked && selectedTab != HomeTab.RECOMMEND) {  // 🔹 lock이면 무시
                            onHeightChange((sheetHeightPx - delta).coerceIn(hiddenHeightPx, maxAllowedHeight))
                        }
                    },
                    enabled = !isResultLocked && selectedTab != HomeTab.RECOMMEND,
                    onDragStopped = {
                        // ── 🔹 추천 탭이 아닐 때만 스냅 로직 실행 📍 ──
                        if (!isResultLocked && selectedTab != HomeTab.RECOMMEND) {
                            val finalHeight = when {
                                // 러닝(RUNNING) 탭: 기록이 있고 절반 이상 올리면 전체 확장
                                selectedTab == HomeTab.RUNNING && homeUi == HomeUi.HOME && runningUiState.totalDistance > 0 &&
                                        sheetHeightPx > (collapsedHeightPx + expandedHeightPx) / 2 -> expandedHeightPx

                                // 통: 어느 정도 올라와 있으면 중간 높이(collapsed)로 고정
                                sheetHeightPx > (hiddenHeightPx + collapsedHeightPx) / 2 -> collapsedHeightPx

                                // 4. 그 외: 아예 아래로 숨김
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
private fun MapViewContainer(
    cameraPosition:LatLng,
    recommendCameraLocation: LatLng?,
    bearing: Float = 0.0f,
    homeUi: HomeUi = HomeUi.RUN,
    latLngList: List<LatLng> = emptyList(),   // 지금까지 나의 러닝 경로 (러닝 모드)
    guidePath: List<LatLng> = emptyList(),    // 코스 시작점까지의 안내 경로
    recommendPath: List<LatLng> = emptyList(), // 추천된 후보 코스들의 경로 (추천 모드)
    selectedCoursePath: List<LatLng> = emptyList(), // 최종 선택된 코스 경로
    destinationMarkerPos: LatLng? = null,     // 목적지 마커
    guideDistance: Int = 0, // 경로까지 걸리는 거리
    guideDuration: Long = 0L, // 경로까지 걸리는 시간
    isTrackingMode: Boolean = false,
    isManualMode: Boolean,
    onManualModeChange: (Boolean) -> Unit,
    modifier:Modifier = Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var isFirstLoad by remember { mutableStateOf(true) } // 처음 지도가 켜졌을 때만 순간이동을 하기 위한 플래그

    val locationSource = remember {
        com.naver.maps.map.util.FusedLocationSource(context as android.app.Activity, 1000)
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    val recommendPathOverlay = remember {// 추천 코스 전용 오버레이
        PathOverlay().apply {
            color = Color.Cyan.toArgb() // 추천 코스는 하늘색으로 구분
            outlineColor = Color.Black.toArgb()
            width = with(density) { 8.dp.toPx() }.toInt()
            outlineWidth = with(density) { 2.dp.toPx() }.toInt()
            patternImage = OverlayImage.fromResource(R.drawable.arrow_path)
            patternInterval = with(density) { 20.dp.toPx() }.toInt()
        }
    }

    val selectedPathOverlay = remember {// 최종 선택 코스 전용 오버레이
        PathOverlay().apply {
            color = Color.Yellow.toArgb()
            outlineColor = Color.Black.toArgb()
            width = with(density) { 15.dp.toPx() }.toInt()
            patternImage = OverlayImage.fromResource(R.drawable.arrow_path)
        }
    }

    val guidePathOverlay = remember {// 안내 경로용 오버레이
        PathOverlay().apply {
            color = PointColor.toArgb()
            outlineColor = Color.Black.toArgb()
            width = with(density) { 12.dp.toPx() }.toInt()
            outlineWidth = with(density) { 2.dp.toPx() }.toInt()
            patternImage = OverlayImage.fromResource(R.drawable.arrow_path)
            patternInterval = with(density) { 20.dp.toPx() }.toInt() // 화살표 간격
        }
    }

    val runningPathOverlay = remember {// 실제 러닝한 경로 오버레이
        PathOverlay().apply {
            color = PointColor.toArgb()
            outlineColor = Color.Black.toArgb()
            width = with(density) { 8.dp.toPx() }.toInt()
            outlineWidth = with(density) { 3.dp.toPx() }.toInt()
        }
    }

    val destMarker = remember {// 목적지 마커
        Marker().apply {
            icon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_marker_icon_blue)
            density.run { // 캡션 너비 등도 density 스코프 안에서 계산
                captionRequestedWidth = 100.dp.toPx().toInt()
            }
            captionTextSize = 14f
        }
    }

    val infoWindow = remember { // 경로 정보 말풍선용 InfoWindow
        com.naver.maps.map.overlay.InfoWindow().apply {
            adapter = object : com.naver.maps.map.overlay.InfoWindow.DefaultTextAdapter(context) {
                override fun getText(infoWindow: com.naver.maps.map.overlay.InfoWindow): CharSequence {
                    val km = String.format("%.1f", guideDistance / 1000f)
                    val min = guideDuration / 1000 / 60

                    return "${km}km (${min}분)"
                }
            }
            alpha = 0.9f
        }
    }

    val anchorMarker = remember {// 풍선을 고정할 투명 마커
        Marker().apply {
            icon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_location_overlay_sub_icon_arrow)
            alpha = 0f // 마커 자체는 투명하게
            width = 1
            height = 1
        }
    }

    val destinationGlow = remember {// 목적지 지점을 강조할 글로우 효과 (은은하게 퍼지는 원)
        CircleOverlay().apply {
            radius = 5.0
            color = PointColor.copy(alpha = 0.2f).toArgb() // 우리 앱 포인트 컬러의 반투명 버전
            outlineColor = PointColor.toArgb() // 테두리는 선명하게
            outlineWidth = with(density) { 2.dp.toPx() }.toInt()
        }
    }

    val destinationCenter = remember {// 중심부의 작은 점
        CircleOverlay().apply {
            radius = 3.0 // 아주 작은 원
            color = Color.White.toArgb() // 흰색으로 강조
            outlineColor = Color.Black.toArgb()
            outlineWidth = with(density) { 1.dp.toPx() }.toInt()
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
            guidePathOverlay.map = null
            destMarker.map = null
        }
    }

    var naverMapInstance by remember { mutableStateOf<NaverMap?>(null) }

    LaunchedEffect(Unit) {
        mapView.getMapAsync { naverMap ->
            naverMapInstance = naverMap
            naverMap.locationSource = locationSource
            naverMap.addOnCameraChangeListener { reason, _ ->
                if (reason == CameraUpdate.REASON_GESTURE) {
                    onManualModeChange(true)
                }
            }
        }
    }

    AndroidView(
        factory = {
            mapView
        },
        modifier = modifier,
        update = { _ ->
            val naverMap = naverMapInstance ?: return@AndroidView

            // --------------------------------------------- 카메라 조정 로직 ----------------------------------

            // 첫 로딩 -> 애니메이션 없이 바로 뜸
            if (isFirstLoad) {
                val initialCamera = CameraUpdate.toCameraPosition(CameraPosition(cameraPosition, 18.0))
                naverMap.moveCamera(initialCamera)
                isFirstLoad = false
                return@AndroidView
            }

            // 경로 안내 -> 따라가기 모드 설정 (현재 지도 모드와 위젯 상태가 다를 때만 업데이트)
            val targetMode = if (isTrackingMode) LocationTrackingMode.Face else LocationTrackingMode.None
            if (naverMap.locationTrackingMode != targetMode) {
                naverMap.locationTrackingMode = targetMode
                if (isTrackingMode) {
                    naverMap.moveCamera(
                        CameraUpdate.toCameraPosition(
                            CameraPosition(cameraPosition, 18.0, 0.0, bearing.toDouble())
                        ).animate(CameraAnimation.Easing)
                    )
                    return@AndroidView
                }
            }

            if (isTrackingMode) {// 따라가기 모드일 때는 시스템이 위치를 추적하므로 수동 이동 건너뜀
                naverMap.locationOverlay.isVisible = true
            } else {
                // 따라가기 모드 X -> 내 위치 오버레이 수동 설정
                naverMap.locationOverlay.apply {
                    isVisible = true
                    position = cameraPosition
                    setBearing(bearing)
                    subIcon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_location_overlay_sub_icon_arrow)
                }

                // 카메라 이동 우선순위
                when {
                    homeUi == HomeUi.RUN -> { // 1순위 : 러닝 중일 때
                        naverMap.moveCamera(
                            CameraUpdate.toCameraPosition(
                                CameraPosition(cameraPosition, 18.0, 0.0, bearing.toDouble())
                            ).pivot(PointF(0.5f, 0.65f)).animate(CameraAnimation.Easing, 1200)
                        )
                    }
                    isManualMode -> { // 2순위 : 자유 모드 활성화 시: 아래의 모든 카메라 이동 명령을 무시
                        // 사용자가 지도를 마음대로 움직이게 둠
                    }

                    guidePath.size >= 2 -> { // 3순위 : 경로 안내 중일 때
                        val bounds = LatLngBounds.Builder().apply {
                            guidePath.forEach { include(it) }
                        }.build()
                        naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 150).animate(CameraAnimation.Easing, 1500))
                    }

                    selectedCoursePath.size >= 2 -> { // 4순위 : homeUi가 HOME이더라도 selectedCoursePath 가 있으면 코스를 우선적으로 비춤
                        val boundsBuilder = LatLngBounds.Builder()
                        boundsBuilder.include(cameraPosition) // 현재 내 위치 포함

                        selectedCoursePath.forEach { latLng -> // selectedCoursePath 직접 순회
                            boundsBuilder.include(latLng)
                        }

                        try {
                            val bounds = boundsBuilder.build()
                            val cameraUpdate = CameraUpdate.fitBounds(bounds, 350) // 모든 지점이 포함되도록 카메라 업데이트 생성
                                .animate(CameraAnimation.Easing, 1000)
                            naverMap.moveCamera(cameraUpdate)
                        } catch (e: Exception) { // 혹시 모를 에러 발생 시 리스트의 첫 번째 좌표로 이동하는 방어 로직
                            val fallbackTarget = selectedCoursePath.first()
                            naverMap.moveCamera(
                                CameraUpdate.toCameraPosition(CameraPosition(fallbackTarget, 15.5))
                                    .animate(CameraAnimation.Easing, 1000)
                            )
                        }
                    }

                    homeUi == HomeUi.RECOMMEND -> { // 5순위 : 코스 추천 후보 보여주기
                        if (recommendCameraLocation != null && recommendCameraLocation != cameraPosition  // 👈 내 위치랑 같으면 무시
                        ) {
                            val bounds = LatLngBounds.Builder()
                                .include(cameraPosition)
                                .include(recommendCameraLocation)
                                .build()
                            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 350).animate(CameraAnimation.Easing, 1000))
                        }
                    }

                    homeUi == HomeUi.HOME -> { // 6순위: 일반 홈 화면 (찜한 코스도 없고, 구경 중도 아닐 때 -> 나를 비춤)
                        naverMap.moveCamera(
                            CameraUpdate.toCameraPosition(CameraPosition(cameraPosition, 18.0))
                                .animate(CameraAnimation.Easing, 1200)
                        )
                    }
                }
            }

            // 코스 그리는 부분 (내 러닝 코스, 경로 코스, 추천 코스)
            if (latLngList.size >= 2) { // 내 러닝 코스
                runningPathOverlay.coords = latLngList
                runningPathOverlay.map = naverMap // 지도에 부착
            } else {
                runningPathOverlay.map = null    // 좌표 부족 시 제거
            }

            if (selectedCoursePath.size >= 2) {
                selectedPathOverlay.coords = selectedCoursePath
                selectedPathOverlay.map = naverMap

                // ── 🔹 목적지 강조 로직 추가 📍 ──
                selectedCoursePath.lastOrNull()?.let { lastPoint ->
                    destinationGlow.center = lastPoint
                    destinationGlow.map = naverMap

                    destinationCenter.center = lastPoint
                    destinationCenter.map = naverMap
                }
            } else {
                selectedPathOverlay.map = null
                destinationGlow.map = null
                destinationCenter.map = null
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

        }
    )
}