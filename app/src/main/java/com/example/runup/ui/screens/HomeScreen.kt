package com.example.runup.ui.screens

import android.os.Bundle
import com.example.runup.ui.components.ControlButton
import android.util.Log
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runup.ui.theme.PointColor
import com.example.runup.viewmodel.HomeTab
import com.example.runup.viewmodel.RunningUiState
import com.naver.maps.map.overlay.LocationOverlay
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PolylineOverlay

@Preview
@Composable
private fun Preview_HomeContent(){
    HomeContent(homeUiState = HomeUiState(isRunning = true), runningUiState = RunningUiState(),
        {},{},{}, {},
        {},{},{},{},{},{ _, _ -> },
        {}
    )
}


@Composable
fun HomeScreen(
    onMenuClick:()->Unit,
    viewModel: HomeViewModel = hiltViewModel()
){
    val homeUiState by viewModel.homeUiState.collectAsState()
    val runningUiState by viewModel.runningUiState.collectAsStateWithLifecycle()
    val timer by viewModel.loadingTimer.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ){
        HomeContent(
            homeUiState = homeUiState,
            runningUiState = runningUiState,
            onMenuClick = onMenuClick,
            onRunClick = {viewModel.onRunClick()},
            stopRunningTracking = {viewModel.stopRunningTracking()},
            recordRunningCourse = {viewModel.recordRunningCourse()},

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
        if(homeUiState.isLoading){
            LoadingStart(timer)
        }
    }
}

@Composable
private fun HomeContent(
    homeUiState: HomeUiState,
    runningUiState: RunningUiState,
    onMenuClick:()->Unit,
    onRunClick:()->Unit,
    stopRunningTracking:()->Unit,
    recordRunningCourse:()->Unit,
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

    val runningPace = if (runningUiState.totalDistance < 100.0) 0.0
        else (runningUiState.totalTime / runningUiState.totalDistance) * 1000
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
                        bearing = homeUiState.currentBearing,
                        isRunning = homeUiState.isRunning,
                        latLngList = runningUiState.latLngList,
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
            }
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
            ){
                BottomSection(
                    selectedTab = homeUiState.selectedTab,
                    onTabSelect = { viewModel.selectTab(it) },
                    isRunning = homeUiState.isRunning,         // 🔹 추가
                    runningUiState = runningUiState            // 🔹 추가
                ) {
                    when (homeUiState.selectedTab) {

                        HomeTab.RUNNING -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // --- 1. 상단 제어 및 목표 영역 (단일 Row) ---
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // [왼쪽] 목표 정보 영역 (순서를 위로 올림)
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

                                    // [오른쪽] 제어 버튼 영역 (순서를 아래로 내림)
                                    Row(
                                        modifier = Modifier.wrapContentWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (!homeUiState.isRunning) {
                                            ControlButton(text = "Run", color = PointColor) { onRunClick() }
                                        } else {
                                            val pauseOrResumeText = if (runningUiState.isTracking) "일시 정지" else "재개"
                                            ControlButton(text = pauseOrResumeText, color = Color.Gray) { stopRunningTracking() }
                                            ControlButton(text = "완료", color = Color.Red) { recordRunningCourse() }
                                        }
                                    }
                                }
                            }
                        }
                        HomeTab.RECOMMEND -> {
                            Text("추천 코스 리스트가 여기에 나타납니다.", color = Color.White, modifier = Modifier.padding(20.dp))
                        }
                        HomeTab.COURSE -> {
                            Text("나의 저장된 경로가 여기에 나타납니다.", color = Color.White, modifier = Modifier.padding(20.dp))
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
                rangeSeconds = 0..60,
                startMinute = (homeUiState.goalPace/60 + 1),
                startSecond = (homeUiState.goalPace%60 + 1),
                onConfirm = onPaceConfirm,
                onDismiss = onPaceClose
            )
        }
    }
}




@Composable
private fun BottomSection(
    modifier: Modifier = Modifier,
    selectedTab: HomeTab,
    onTabSelect: (HomeTab) -> Unit,
    isRunning: Boolean,            // 추가
    runningUiState: RunningUiState, // 추가
    content: @Composable () -> Unit // 탭에 따른 내용
) {
    val density = LocalDensity.current
    // 네이버 지도 스타일 높이 설정
    val navBarHeight = 80.dp // 하단 메뉴바 높이

    // 1. 높이 정의
    val hiddenHeightPx = with(density) { 60.dp.toPx() } // 시트가 아예 내려가 있는 상태 (처음)
    val collapsedHeightPx = with(density) { 100.dp.toPx() } // 메뉴 클릭 시 올라오는 높이
    val expandedHeightPx = with(density) { 550.dp.toPx() }  // 최대로 올렸을 때 높이


    var sheetHeightPx by remember { mutableFloatStateOf(hiddenHeightPx) }
    val animatedHeight by animateDpAsState(targetValue = with(density) { sheetHeightPx.toDp() })

    Column(modifier = modifier.fillMaxWidth()) {
        // 정보창 영역 (시트 바로 위에 부착되어 함께 이동) ---
        if (isRunning && sheetHeightPx > hiddenHeightPx + 10f) {
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

                // 3. 페이스 정보
                val runningPace = if (runningUiState.totalDistance < 100.0) 0.0
                else (runningUiState.totalTime / runningUiState.totalDistance) * 1000

                Column {
                    Text(text = "페이스", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(
                        text = "${(runningPace / 60).toInt()}'${(runningPace % 60).toInt()}\"",
                        color = PointColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }

                // 4. 칼로리 정보
                Column {
                    Text(text = "칼로리", color = WhiteTextColor.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(
                        text = "119kcal",
                        color = PointColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        // 1. 드래그 가능한 바텀 시트
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(BackGroudColor)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        sheetHeightPx = (sheetHeightPx - delta).coerceIn(hiddenHeightPx, expandedHeightPx)
                    },
                    onDragStopped = {
                        // 🔹 수정: 각 구간 사이의 중간값을 기준으로 스냅핑 위치 결정
                        sheetHeightPx = when {
                            // 중간보다 높으면 최대로 확장
                            sheetHeightPx > (collapsedHeightPx + expandedHeightPx) / 2 -> expandedHeightPx

                            // 중간과 숨김 사이에서 판단 (숨김~중간지점)
                            sheetHeightPx > (hiddenHeightPx + collapsedHeightPx) / 2 -> collapsedHeightPx

                            // 그 외에는 숨김 상태로
                            else -> hiddenHeightPx
                        }
                    }
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1) 핸들러: 항상 중앙 상단에 노출
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(40.dp, 4.dp)
                        .background(Color.Gray.copy(0.5f), RoundedCornerShape(2.dp))
                )

                // 2) 실제 컨텐츠: 높이가 어느 정도 확보되었을 때만 노출
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
                                onTabSelect(tab)
                                sheetHeightPx = collapsedHeightPx
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
    bearing: Float = 0.0f, // 추가
    isRunning: Boolean = false,
    latLngList: List<LatLng> = emptyList(),   // 지금까지 이동 경로
    modifier:Modifier = Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val density = context.resources.displayMetrics.density


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
                naverMap.moveCamera(
                    CameraUpdate.toCameraPosition(
                        CameraPosition(cameraPosition, 18.0)
                    ).animate(CameraAnimation.Easing, 1200)
                )

                // 네이버 지도 자체 위치 오버레이 설정
                val locationOverlay = naverMap.locationOverlay
                locationOverlay.isVisible = true
                locationOverlay.position = cameraPosition
                locationOverlay.bearing = bearing // 화살표가 가리키는 방향
                //Log.d("MAP", "bearing: $bearing")

                locationOverlay.subIcon = OverlayImage.fromResource(
                    com.naver.maps.map.R.drawable.navermap_default_location_overlay_sub_icon_arrow
                )
                locationOverlay.iconWidth = LocationOverlay.SIZE_AUTO
                locationOverlay.iconHeight = LocationOverlay.SIZE_AUTO
                locationOverlay.subIconWidth = LocationOverlay.SIZE_AUTO
                locationOverlay.subIconHeight = LocationOverlay.SIZE_AUTO

                if (latLngList.size >= 2) {
                    polyline.coords = latLngList
                    polyline.width = 10
                    polyline.color = PointColor.toArgb()
                    polyline.map = naverMap
                } else {
                    polyline.map = null
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
    timeNumber:Int,
){
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ){
            Box {
                // 외곽선
                androidx.tv.material3.Text(
                    text = timeNumber.toString(),
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        fontSize = 150.sp,
                        color = PointColor,
                        drawStyle = Stroke(width = 20f)
                    )
                )

                // 내부 채우기
                androidx.tv.material3.Text(
                    text = timeNumber.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 150.sp,
                    color = TextWhite
                )
            }
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