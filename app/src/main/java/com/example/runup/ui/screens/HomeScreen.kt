package com.example.runup.ui.screens

import androidx.compose.animation.core.Spring
import com.example.runup.ui.components.ControlButton
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.viewmodel.HomeUiState
import com.example.runup.viewmodel.HomeViewModel
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runup.domain.model.Scores
import com.example.runup.ui.theme.PointColor
import com.example.runup.viewmodel.GuideUiState
import com.example.runup.viewmodel.HomeTab
import com.example.runup.viewmodel.RunningUiState
import com.example.runup.domain.model.AddressModel
import com.example.runup.domain.model.CourseRecommendation
import com.example.runup.domain.model.SortDirection
import com.example.runup.domain.model.SortType
import com.example.runup.ui.components.AIStatusOverlay
import com.example.runup.ui.components.AiCharacterIcon
import com.example.runup.ui.components.AiGuideBubble
import com.example.runup.ui.components.AiReasonBubble
import com.example.runup.ui.components.BottomSection
import com.example.runup.ui.components.CourseInfoCard
import com.example.runup.ui.components.FailMessageBubble
import com.example.runup.ui.components.FakeMap
import com.example.runup.ui.components.HelpCircleButton
import com.example.runup.ui.components.LoadingStart
import com.example.runup.ui.components.LoopSelectionDialog
import com.example.runup.ui.components.MapViewContainer
import com.example.runup.ui.components.MenuBtn
import com.example.runup.ui.components.RunningResultContent
import com.example.runup.ui.navigation.HomeUi
import com.example.runup.ui.theme.Gray
import com.example.runup.viewmodel.AiPostureUiState
import com.example.runup.viewmodel.CourseRecommendationUiState

@Preview
@Composable
private fun Preview_HomeContent() {
    HomeContent(
        homeUiState = HomeUiState(homeUi = HomeUi.HOME, selectedTab = HomeTab.RUNNING),
        runningUiState = RunningUiState(),
        guideUiState = GuideUiState(),
        aiPostureUiState = AiPostureUiState(),
        courseRecommendationUiState = CourseRecommendationUiState(),
        addressUiState = null,
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
        onToggleAi = {},
        onToggleAiRecommendMode = {},
        onOpenRecommendDistanceDialog = {},
        onOpenRecommendLoopDialog = {},
        onOpenMaxDistanceDialog = {},
        onConfirmRecommendSort = {},
        onToggleSortDirection = {},
        onSearchClick = {},
        onAiSearchClick = {},
        onClearRecommendation = {},
        onSubtractCourseIndex = {},
        onAddCourseIndex = {},
        onSelectRecommendCourse = {},
        onClearSelectedCourse = {},
        onStartNavigation = {},
        onClearNavigation = {},
        onToggleTrackingMode = {},
        onClearFailMessage = {},
        onSelectTab = {},
        onCancelRunningCourse = {},
        onConfirmRecommendDistance = {},
        onCloseRecommendDistanceDialog = {},
        onSelectRecommendLoop = {},
        onCloseRecommendLoopDialog = {},
        onConfirmMaxDistance = {},
        onCloseMaxDistanceDialog = {},

        helpStep = HelpStep.HOME_2
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
    val addressUiState by viewModel.addressUiState.collectAsState()
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
                addressUiState = addressUiState,
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
                },

                onToggleAiRecommendMode = { viewModel.toggleAiRecommendMode() },
                onOpenRecommendDistanceDialog = { viewModel.openRecommendDistanceDialog() },
                onOpenRecommendLoopDialog = { viewModel.openRecommendLoopDialog() },
                onOpenMaxDistanceDialog = { viewModel.openMaxDistanceDialog() },
                onConfirmRecommendSort = { viewModel.confirmRecommendSort(it) },
                onToggleSortDirection = { viewModel.toggleSortDirection() },
                onSearchClick = { viewModel.onSearchClick() },
                onAiSearchClick = { viewModel.onAiSearchClick(it) },
                onClearRecommendation = { viewModel.clearRecommendation() },
                onSubtractCourseIndex = { viewModel.subtractRecommendCourseIndex() },
                onAddCourseIndex = { viewModel.addRecommendCourseIndex() },
                onSelectRecommendCourse = { viewModel.selectRecommendCourse(it) },
                onClearSelectedCourse = { viewModel.clearSelectedCourse() },
                onStartNavigation = { viewModel.startNavigation(it) },
                onClearNavigation = { viewModel.clearNavigation() },
                onToggleTrackingMode = { viewModel.toggleTrackingMode() },
                onClearFailMessage = { viewModel.clearFailMessage() },

                onSelectTab = { viewModel.selectTab(it) },
                onCancelRunningCourse = { viewModel.cancelRunningCourse() },
                onConfirmRecommendDistance = { viewModel.confirmRecommendDistance(it) },
                onCloseRecommendDistanceDialog = { viewModel.closeRecommendDistanceDialog() },
                onSelectRecommendLoop = { viewModel.selectRecommendLoop(it) },
                onCloseRecommendLoopDialog = { viewModel.closeRecommendLoopDialog() },
                onConfirmMaxDistance = { viewModel.confirmMaxDistance(it) },
                onCloseMaxDistanceDialog = { viewModel.closeMaxDistanceDialog() },
            )
        }
        if(homeUiState.isLoading){
            LoadingStart(timer)
        }
    }
}

private enum class HelpStep {
    NONE,
    HOME_1,
    HOME_2,
    HOME_3,
    HOME_4,
    RECOMMEND_1,
    RECOMMEND_2,
    RECOMMEND_3,
    RECOMMEND_4,
    RECOMMEND_5,
    RECOMMEND_6,
}

@Composable
private fun HomeContent(
    homeUiState: HomeUiState,
    runningUiState: RunningUiState,
    guideUiState: GuideUiState,
    courseRecommendationUiState: CourseRecommendationUiState,
    aiPostureUiState: AiPostureUiState,
    addressUiState: AddressModel?,
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
    // 추천 관련
    onToggleAiRecommendMode: () -> Unit,     // 👈 추가
    onOpenRecommendDistanceDialog: () -> Unit,
    onOpenRecommendLoopDialog: () -> Unit,
    onOpenMaxDistanceDialog: () -> Unit,
    onConfirmRecommendSort: (SortType) -> Unit,
    onToggleSortDirection: () -> Unit,
    onSearchClick: () -> Unit,
    onAiSearchClick: (String) -> Unit,
    onClearRecommendation: () -> Unit,
    onSubtractCourseIndex: () -> Unit,
    onAddCourseIndex: () -> Unit,
    onSelectRecommendCourse: (CourseRecommendation) -> Unit,
    onClearSelectedCourse: () -> Unit,
    onStartNavigation: (LatLng) -> Unit,
    onClearNavigation: () -> Unit,
    onToggleTrackingMode: () -> Unit,
    onClearFailMessage: () -> Unit,

    onSelectTab: (HomeTab) -> Unit,
    onCancelRunningCourse: () -> Unit,
    onConfirmRecommendDistance: (Int) -> Unit,
    onCloseRecommendDistanceDialog: () -> Unit,
    onSelectRecommendLoop: (Boolean) -> Unit,
    onCloseRecommendLoopDialog: () -> Unit,
    onConfirmMaxDistance: (Int) -> Unit,
    onCloseMaxDistanceDialog: () -> Unit,

    helpStep: HelpStep = HelpStep.NONE
){
    val isPreview = LocalInspectionMode.current
    val density = LocalDensity.current
    val screenHeightPx = LocalConfiguration.current.screenHeightDp.let {
        with(density) { it.dp.toPx() }
    }


    val navBarHeightPx       = screenHeightPx * 0.095f  // 80 / 844
    val hiddenHeightPx       = screenHeightPx * 0.071f  // 60 / 844
    val collapsedHeightPx    = screenHeightPx * 0.130f  // 110 / 844
    val selectedCourseTabHeightPx = screenHeightPx * 0.160f  // 135 / 844
    val recommendTabHeightPx = screenHeightPx * 0.237f  // 200 / 844
    val expandedHeightPx     = screenHeightPx * 0.652f  // 550 / 844

    var sheetHeightPx by remember { mutableFloatStateOf(hiddenHeightPx) } // 시트 높이 상태

    val animatedSheetHeightDp by animateDpAsState(
        targetValue = with(density) { sheetHeightPx.toDp() },
    )

    var isResultLocked by remember { mutableStateOf(false) } // 러닝 완료 후 결과창 뜬 상태

    val imeHeightPx = WindowInsets.ime.getBottom(density) // 실시간 키보드 높이(px) 상태

    var isManualMode by remember { mutableStateOf(false) }

    var showGuideBubble by remember { mutableStateOf(false) }

    var helpStep by remember { mutableStateOf(helpStep) }

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
                    val address = addressUiState?.fullAddress ?: "위치 확인 중..."

                    Column(
                        modifier = Modifier
                            .padding(top = 35.dp, start = 18.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A0E21).copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = PointColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "현재 위치",
                                color = WhiteTextColor.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = address,
                            color = PointColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    if(homeUiState.homeUi == HomeUi.HOME){
                        when (homeUiState.selectedTab) {
                            HomeTab.RUNNING -> {
                                HelpCircleButton(
                                    onClick = { helpStep = HelpStep.HOME_1 },
                                    modifier = Modifier
                                        .padding(top = 50.dp, start = 8.dp)
                                )
                            }

                            HomeTab.RECOMMEND -> {
                                HelpCircleButton(
                                    onClick = { helpStep = HelpStep.RECOMMEND_1 },
                                    modifier = Modifier
                                        .padding(top = 50.dp, start = 8.dp)
                                )
                            }
                            else -> { }
                        }
                    }
                    MenuBtn(
                        modifier = Modifier.align(Alignment.TopEnd)
                            .padding(top = 35.dp, end = 18.dp),
                        onMenuClick = onMenuClick
                    )
                }
                if (aiPostureUiState.isAiStatusOverlayVisible) { // 설정값이 true일 때만 렌더링
                    AIStatusOverlay(
                        isAiEnabled = aiPostureUiState.isAiEnabled,
                        postureLabel = aiPostureUiState.currentPostureLabel,
                        leftBleState = aiPostureUiState.leftBleState,
                        rightBleState = aiPostureUiState.rightBleState,
                        onToggle = onToggleAi,
                        modifier = Modifier
                            .padding(top = 20.dp)
                    )
                }


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
            // [하단 위젯 모음] 메시지 버블과 내 위치 돌아가기 버튼을 나란히 배치
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    // 바텀시트 높이 + 네비바(80dp) + 기본 여백(16dp)
                    .padding(bottom = with(density) { animatedSheetHeightDp } + 80.dp + 16.dp,
                        start = 16.dp,
                        end = 16.dp),
                verticalAlignment = Alignment.Bottom, // 메시지와 버튼의 바닥 높이를 맞춤
                horizontalArrangement = Arrangement.End
            ) {
                // 1. 메시지 영역 (왼쪽 공간을 다 차지하면서 오른쪽 버튼을 밀어냄)
                val currentCourse = courseRecommendationUiState.recommendedCourses.getOrNull(courseRecommendationUiState.courseIndex)

                // ── 🔹 캐릭터 및 메시지 영역 📍 ──
                val isRecommendTab = homeUiState.selectedTab == HomeTab.RECOMMEND
                val isAiMode = courseRecommendationUiState.isAiMode
                val hasSelectedCourse = homeUiState.selectedPath != null
                val shouldShowCharacter = isRecommendTab && isAiMode && !hasSelectedCourse // 표시 조건: 추천 탭 + AI 모드 + 코스 최종 선택 전

                Box(
                    modifier = Modifier.weight(1f), // 남은 가로 공간을 다 쓰되 버튼은 침범 안 함
                    contentAlignment = Alignment.BottomStart // 메시지도 오른쪽 정렬
                ) {
                    // ── 🔹 캐릭터가 왼쪽, 버블이 오른쪽 📍 ──
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp) // 캐릭터와 버블 사이 간격
                    ) {
                        // 1. 이미 만들어두신 캐릭터 아이콘 사용
                        if (shouldShowCharacter) {
                            Column(horizontalAlignment = Alignment.Start) {
                                // ── 🔹 캐릭터 터치 시 나오는 안내 가이드 📍 ──
                                if (showGuideBubble && homeUiState.homeUi != HomeUi.RECOMMEND) {
                                    AiGuideBubble()
                                }

                                // 클릭 상태 감지를 위한 상태값
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val rotation by animateFloatAsState(
                                    targetValue = if (isPressed) -30f else 0f, // 눌리면 왼쪽으로 8도 기울어짐
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                )
                                val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f)

                                AiCharacterIcon(
                                    modifier = Modifier
                                        .offset(y = 8.dp)
                                        .graphicsLayer(
                                            rotationZ = rotation, // 좌우 회전
                                            scaleX = scale,       // 크기 축소
                                            scaleY = scale,
                                            transformOrigin = TransformOrigin(0.5f, 1f) // 발바닥(하단 중앙)을 기준으로 흔들리게 설정 📍
                                        )
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null // 거슬리는 배경 회색 제거
                                        ) {
                                            showGuideBubble = !showGuideBubble
                                        }
                                )
                            }
                        }

                        // 2. AI 말풍선
                        val failMessage = courseRecommendationUiState.isFailSearchCourse

                        if (failMessage.isNotBlank()) {
                            FailMessageBubble(
                                message = failMessage,
                                onClose = { onClearFailMessage() }
                            )
                        } else if (isAiMode && courseRecommendationUiState.isRecommendClick && currentCourse != null) {
                            AiReasonBubble(reason = currentCourse.reason)
                        }
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
                            tint = if (!isManualMode or (homeUiState.homeUi == HomeUi.RUN)) PointColor else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxSize()
            ){
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
                        showGuideBubble = false
                        onSelectTab(tab)
                    },
                    homeUi = homeUiState.homeUi,
                    runningUiState = runningUiState,
                    sheetHeightPx = sheetHeightPx,
                    animatedHeightDp = animatedSheetHeightDp,
                    onHeightChange = { sheetHeightPx = it },
                    isResultLocked = isResultLocked,
                    imeHeightPx = imeHeightPx,
                    goalDistance = homeUiState.goalDistance,
                    goalPace = homeUiState.goalPace,
                ) {
                    when (homeUiState.selectedTab) {

                        HomeTab.RUNNING -> {
                            // 러닝 완료 버튼 누른 후 (바텀 시트 최대 확장)
                            if (sheetHeightPx >= expandedHeightPx - 10f) {
                                RunningResultContent(
                                    runningUiState = runningUiState,
                                    onSave = { b, c, d ->
                                        recordRunningCourse(Scores(b.toDouble(), c.toDouble(), d.toDouble())) // 실제 데이터 저장 로직
                                        sheetHeightPx = collapsedHeightPx
                                        isResultLocked = false
                                    },
                                    onSkip = {
                                        onCancelRunningCourse()
                                        sheetHeightPx = collapsedHeightPx
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
                                                ControlButton(text = pauseOrResumeText, color = Color.Gray) {
                                                    if (!homeUiState.isLoading) {
                                                        stopRunningTracking()
                                                    }
                                                }
                                                ControlButton(text = "완료", color = Color.Red) {
                                                    if (!homeUiState.isLoading) {
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
                                        // 1. 헤더 (전체를 감싸는 부모 Row)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween, // 👈 [왼쪽 그룹]과 [오른쪽 버튼]을 양끝으로 찢음
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            // ── 🔹 [왼쪽 그룹] 타이틀 + 카운트를 하나로 묶음 📍 ──
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp) // 타이틀과 숫자 사이 간격
                                            ) {
                                                // 메인 타이틀
                                                Text(
                                                    text = if (isAiMode) "AI 맞춤 코스 추천" else "나에게 맞는 코스 찾기",
                                                    color = WhiteTextColor,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                // 카운트 (AI 모드일 때만)
                                                if (isAiMode) {
                                                    Text(
                                                        text = "(${courseRecommendationUiState.aiSearchCount}/4)",
                                                        color = if (courseRecommendationUiState.aiSearchCount >= 4) Color(0xFFE57373) else PointColor, // 👈 요청하신 PointColor 반영
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                            }

                                            // ── 🔹 [오른쪽 그룹] 모드 전환 버튼 (기존 코드 그대로) 📍 ──
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(
                                                        if (isAiMode) Color.White.copy(alpha = 0.1f) else Color(
                                                            0xFF311B92
                                                        ).copy(alpha = 0.8f)
                                                    )
                                                    .clickable { onToggleAiRecommendMode() }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isAiMode) Icons.Default.FilterList else Icons.Default.AutoAwesome,
                                                    tint = PointColor,
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
                                            Column(modifier = Modifier.clickable { onOpenRecommendDistanceDialog() }) {
                                                Text("목표 거리", color = Gray, fontSize = 11.sp)
                                                Text("${courseRecommendationUiState.goalDistance / 1000.0}km", color = PointColor, fontWeight = FontWeight.Bold)
                                            }

                                            Spacer(modifier = Modifier.width(24.dp)) // 기존보다 간격을 살짝 더 줌

                                            // [공통] 계산 방법 (왕복/편도)
                                            Column(modifier = Modifier.clickable { onOpenRecommendLoopDialog() }) {
                                                Text("러닝 방법", color = Gray, fontSize = 11.sp)
                                                Text(if(courseRecommendationUiState.isLoop) "왕복" else "편도", color = PointColor, fontWeight = FontWeight.Bold)
                                            }

                                            // ── 🔹 1. 계산방법과 거리 사이를 확 띄우기 위해 가중치(weight) 사용 📍 ──
                                            Spacer(modifier = Modifier.weight(1f))

                                            // ── 🔹 2 & 3. 코스까지의 거리 레이아웃 수정 📍 ──
                                            Column(
                                                horizontalAlignment = Alignment.End, // 우측 정렬로 변경하여 끝에 붙임
                                                modifier = Modifier.clickable { onOpenMaxDistanceDialog() }
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
                                                    .background(
                                                        Color.White.copy(alpha = 0.05f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                BasicTextField(
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
                                                        onAiSearchClick(aiChatInput)
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
                                                                .background(
                                                                    if (isSelected) PointColor else Color.White.copy(
                                                                        alpha = 0.05f
                                                                    )
                                                                )
                                                                .clickable {
                                                                    onConfirmRecommendSort(sortType)
                                                                }
                                                                .padding(
                                                                    horizontal = 10.dp,
                                                                    vertical = 6.dp
                                                                ),
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
                                                                        .clickable { onToggleSortDirection() } // 📍 클릭 시 오름/내림차순 전환
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(4.dp))

                                                val canSearch = homeUiState.homeUi != HomeUi.RUN && !homeUiState.isLoading

                                                ControlButton(
                                                    text = "코스 추천", // 👈 요청하신 대로 줄바꿈 적용!
                                                    color = PointColor,
                                                    contentColor = Color.Black,
                                                    isLoading = courseRecommendationUiState.isLoading,
                                                    modifier = Modifier
                                                        .size(72.dp) // 72dp 정사각형
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    if (canSearch) {
                                                        isManualMode = false
                                                        onSearchClick()
                                                    }
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
                                                        .size(
                                                            width = if (isSelected) 16.dp else 6.dp,
                                                            height = 6.dp
                                                        ) // 현재 페이지는 길쭉하게!
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(
                                                            if (isSelected) PointColor else Gray.copy(
                                                                alpha = 0.3f
                                                            )
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
                                                modifier = Modifier.clickable { onClearRecommendation() }
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                ControlButton("이전", Color.DarkGray) { onSubtractCourseIndex() }
                                                ControlButton("다음", Color.DarkGray) { onAddCourseIndex() }
                                                ControlButton("선택", PointColor) {
                                                    currentCourse?.let { onSelectRecommendCourse(it) }
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
                                                        onClearSelectedCourse()
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
                                                        onClearNavigation()
                                                    }

                                                    // ── 🔹 [핵심 추가] 안내 중일 때만 나타나는 버튼 📍 ──
                                                    val trackingText = if (guideUiState.isTrackingMode) "추적 중" else "따라가기"
                                                    ControlButton(
                                                        text = trackingText,
                                                        color = Color.DarkGray,
                                                        contentColor = Color.White
                                                    ) {
                                                        onToggleTrackingMode()
                                                    }
                                                } else {
                                                    ControlButton(
                                                        text = "경로 안내",
                                                        color = PointColor,
                                                        contentColor = Color.Black // 노란 배경엔 검정 글씨 📍
                                                    ) {
                                                        val firstPoint = selectedCourse.points.first()
                                                        onStartNavigation(LatLng(firstPoint.latitude, firstPoint.longitude))
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

        if (homeUiState.showDistanceDialog) { // 목표 거리 설정 다이얼로그
            DistanceGoalSettingDialog(
                "목표 거리 설정",
                range = 0..100,
                startNumber = (homeUiState.goalDistance/100 + 1),
                onConfirm = onDistanceConfirm,
                onDismiss = onDistanceClose
            )
        }
        else if (homeUiState.showPaceDialog) { // 목표 페이스 설정 다이얼로그
            PaceGoalSettingDialog(
                rangeMinutes = 0..20,
                rangeSeconds = 0..59,
                startMinute = (homeUiState.goalPace/60 + 1),
                startSecond = (homeUiState.goalPace%60 + 1),
                onConfirm = onPaceConfirm,
                onDismiss = onPaceClose
            )
        }
        if (helpStep != HelpStep.NONE) {
            HelpOverlay(
                step = helpStep,
                onNext = {
                    helpStep = when (helpStep) {
                        HelpStep.HOME_1 -> HelpStep.HOME_2
                        HelpStep.HOME_2 -> HelpStep.HOME_3
                        HelpStep.HOME_3 -> HelpStep.HOME_4
                        HelpStep.HOME_4 -> HelpStep.NONE

                        HelpStep.RECOMMEND_1 -> HelpStep.RECOMMEND_2
                        HelpStep.RECOMMEND_2 -> HelpStep.RECOMMEND_3
                        HelpStep.RECOMMEND_3 -> HelpStep.RECOMMEND_4
                        HelpStep.RECOMMEND_4 -> HelpStep.RECOMMEND_5
                        HelpStep.RECOMMEND_5 -> HelpStep.RECOMMEND_6
                        HelpStep.RECOMMEND_6 -> HelpStep.NONE

                        HelpStep.NONE -> HelpStep.NONE
                    }
                }
            )
        }

        if (courseRecommendationUiState.showDistanceDialog) { // 코스 추천 목표 거리 설정 다이얼로그
            DistanceGoalSettingDialog(
                "목표 거리 설정",
                range = 0..100,
                startNumber = (courseRecommendationUiState.goalDistance / 100 + 1),
                onConfirm = { onConfirmRecommendDistance(it) },
                onDismiss = { onCloseRecommendDistanceDialog() }
            )
        }

        if (courseRecommendationUiState.showLoop) { // 왕복/편도 선택 다이얼로그 (간단하게 AlertDialog 등으로 구현 가능)
            LoopSelectionDialog(
                isLoop = courseRecommendationUiState.isLoop,
                onSelect = { onSelectRecommendLoop(it) },
                onDismiss = { onCloseRecommendLoopDialog() }
            )
        }

        if (courseRecommendationUiState.showMaxDistanceDialog) { // 러닝 방법 선택 다이얼로그 (왕복/편도)
            DistanceGoalSettingDialog(
                "최대 코스 추천 범위",
                range = 1..20, // 0.1km ~ 10.0km 범위
                // 500m인 경우 5가 선택되어 0.5km로 표시되도록 계산
                startNumber = (courseRecommendationUiState.maxSearchDistance / 100 + 1),
                onConfirm = { kmUnit ->
                    // 다이얼로그에서 선택한 숫자(예: 5)를 받아 500m로 변환하여 저장
                    onConfirmMaxDistance(kmUnit)
                },
                onDismiss = { onCloseMaxDistanceDialog() }
            )
        }
    }
}


@Composable
private fun HelpOverlay(
    step: HelpStep,
    onNext: () -> Unit
) {
    val text = when (step) {
        HelpStep.HOME_1 -> "여기는 러닝 탭입니다"
        HelpStep.HOME_2 -> "목표 거리와 목표 페이스를 설정해 주세요"
        HelpStep.HOME_3 -> "이 버튼을 눌러 내 위치로\n화면을 고정해 주세요"
        HelpStep.HOME_4 -> "Run 버튼을 눌러 달리기를 시작합니다"

        HelpStep.RECOMMEND_1 -> "여기는 코스 추천 탭입니다"
        HelpStep.RECOMMEND_2 -> "추천 받을 방법을 선택해 주세요"
        HelpStep.RECOMMEND_3 -> "원하는 코스 특징을 입력해\n추천 받을 수 있어요"
        HelpStep.RECOMMEND_4 -> "코스 거리, 방법, 코스까지의 거리를\n선택할 수 있어요"
        HelpStep.RECOMMEND_5 -> "이 버튼을 눌러 내 위치로\n화면을 고정해 주세요"
        HelpStep.RECOMMEND_6 -> "버튼을 눌러 코스를 추천 받아요"

        HelpStep.NONE -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onNext()
            },
        contentAlignment = Alignment.Center
    ) {
        when (step) {
            HelpStep.HOME_1 -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                ){
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(80.dp)
                            .border(
                                width = 3.dp,
                                color = Color.Red
                            )
                    )
                }
            }
            HelpStep.HOME_2 -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                ){
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(65.dp)
                            .border(
                                width = 3.dp,
                                color = Color.Red
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(80.dp)
                    )
                }
            }
            HelpStep.HOME_3 -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 190.dp, end = 10.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ){
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .border(
                                width = 3.dp,
                                color = Color.Red
                            )
                    )
                }
            }
            HelpStep.HOME_4 -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 94 .dp, end = 10.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ){
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 46.dp)
                            .border(
                                width = 3.dp,
                                color = Color.Red
                            )
                    )
                }
            }

            HelpStep.RECOMMEND_1 -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ){
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(80.dp)
                            .border(
                                width = 3.dp,
                                color = Color.Red
                            )
                    )
                }
            }
            HelpStep.RECOMMEND_2 -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 97.dp, start = 10.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                ){
                    Box(
                        modifier = Modifier
                            .width(235.dp)
                            .height(43.dp)
                            .border(
                                width = 3.dp,
                                color = Color.Red
                            )
                    )
                }
            }
            HelpStep.RECOMMEND_3 -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 200.dp, end = 10.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ){
                    Box(
                        modifier = Modifier
                            .size(width = 88.dp, height = 38.dp)
                            .border(
                                width = 3.dp,
                                color = Color.Red
                            )
                    )
                }
            }
            HelpStep.RECOMMEND_4 ->{
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 148.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ){
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(47.dp)
                            .border(
                                width = 3.dp,
                                color = Color.Red
                            )
                    )
                }
            }
            HelpStep.RECOMMEND_5 ->{
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 290.dp, end = 10.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ){
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .border(
                                width = 3.dp,
                                color = Color.Red
                            )
                    )
                }

            }
            HelpStep.RECOMMEND_6 ->{
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 95.dp, end = 10.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ){
                    Box(
                        modifier = Modifier
                            .size(width = 84.dp, height = 50.dp)
                            .border(
                                width = 3.dp,
                                color = Color.Red
                            )
                    )
                }
            }
            HelpStep.NONE -> {}
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = text,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

    }
}

