package com.example.runup.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.runup.BuildConfig
import com.example.runup.domain.model.RunRecord
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.ui.util.mapper.TimeMapper
import com.example.runup.viewmodel.MyPageViewModel
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.runup.domain.model.RunFilter
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.util.calculatePace

@Composable
fun MyPageScreen(
    onBackClick: () -> Unit,
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val userData by viewModel.userState.collectAsState()

    val profileBitmap by viewModel.profileBitmap.collectAsState()

    val pagedRuns by viewModel.pagedRuns.collectAsState()

    // 갤러리 실행기 설정
    val profileGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent() // 변경됨
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfileImage(it) }
    }

    // 이름 수정을 위한 상태값
    var showEditDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // 화면 진입 시 데이터 호출
    LaunchedEffect(Unit) {
        viewModel.initData()
    }

    // 이름 수정 다이얼로그
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("이름 수정", color = Color.White) },
            text = {
                Column {
                    Text("새로운 이름을 입력해주세요.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        placeholder = { Text("이름 입력") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateName(newName)
                    showEditDialog = false
                    newName = "" // 초기화
                }) { Text("확인", color = Color(0xFF4A90E2)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("취소", color = Color.Gray) }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }

    Scaffold(
        containerColor = BackGroudColor,
        topBar = { TopBar(text = "마이페이지", isMenu = false, onBackClick = {onBackClick()}) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 프로필 영역
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C2C)) // 배경색을 좀 더 어둡게 변경
                            .clickable { profileGalleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            // [핵심] 메모리에 로드된 비트맵이 있으면 0초 만에 띄움
                            Log.d("bitmap", "dd")
                            Image(
                                bitmap = profileBitmap!!.asImageBitmap(),
                                contentDescription = "Profile Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (!userData?.userProfileUrl.isNullOrEmpty()) {
                            // 비트맵이 없을 때만 차선책으로 AsyncImage 작동
                            Log.d("bitmap", "async")
                            AsyncImage(
                                model = userData?.userProfileUrl, // String(URL)을 직접 넣음
                                contentDescription = "Profile Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("👤", fontSize = 40.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                newName = userData?.userName ?: ""
                                showEditDialog = true
                            }
                        ) {
                            Text(
                                text = userData?.userName ?: "Runner",
                                color = WhiteTextColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("✏️", fontSize = 14.sp)
                        }
                        Text(text = userData?.userEmail ?: "", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            // 2. 최근 목표 영역
            item {
                Column {
                    Text("최근 목표", color = WhiteTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            GoalItem("목표 거리", "${(userData?.goalDistance ?: 0) / 1000f}km")
                            GoalItem("목표 시간", formatSeconds(userData?.goalTime ?: 0))
                        }
                    }
                }
            }

            // 전체 통계 영역 (달린 횟수, 총 거리)
            item {
                Column {
                    Text("전체 통계", color = WhiteTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // 달린 횟수: runs 리스트의 크기 사용
                            GoalItem("달린 횟수", "${userData?.runs?.size ?: 0}회")

                            // 총 거리: 저장된 totalRunningDistance 사용 (m 단위를 km로 변환)
                            val totalDistanceMeters = userData?.runs?.sumOf { it.course.distance } ?: 0
                            val totalKm = totalDistanceMeters.toFloat() / 1000f
                            GoalItem("총 거리", String.format("%.2f km", totalKm))
                        }
                    }
                }
            }

            // 나의 러닝 섹션 헤더 + 필터 칩
            item {
                Column {
                    Text("나의 러닝", color = WhiteTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RunFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = viewModel.selectedFilter == filter,
                                onClick = { viewModel.updateFilter(filter) },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = Color.Gray,
                                    selectedLabelColor = Color.White,
                                    selectedContainerColor = PointColor
                                )
                            )
                        }
                    }
                }
            }

            // 4. 데이터 로드 상태 및 나의 러닝 리스트 처리 🔹
            when {
                // A. 초기 로딩 중 (유저 데이터 자체가 아직 없을 때)
                userData == null && viewModel.isLoadingMore -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }

                // B. 데이터는 불러왔는데 리스트가 비어있을 때
                pagedRuns.isEmpty() && !viewModel.isLoadingMore -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Text("해당 기간의 러닝 기록이 없습니다.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                // C. 리스트가 있을 때 (페이지네이션 적용)
                else -> {
                    items(pagedRuns, key = { it.recordDate }) { run ->
                        ExpandableRunItem(
                            run = run,
                            onDeleteConfirm = { courseId ->
                                viewModel.deleteRun(courseId)
                            }
                        )
                    }

                    // 🔹 5. '더 보기' 버튼 섹션 (리스트가 있을 때 그 아래에 표시)
                    if (viewModel.hasMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().offset(y = (-8).dp).padding(top = 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.isLoadingMore) {
                                    CircularProgressIndicator(color = PointColor, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = "더 보기 ▾",
                                        color = PointColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { viewModel.loadMoreRuns() }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableRunItem(
    run: RunRecord,
    onDeleteConfirm: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 1. 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("기록 삭제") },
            text = { Text("이 러닝 기록을 정말로 삭제하시겠습니까?\n총 거리 통계에서도 차감됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConfirm(run.course.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            },
            containerColor = Color(0xFF2C2C2C),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = { showDeleteDialog = true } // 꾹 누르면 다이얼로그 활성화
            )
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(TimeMapper.formatTimestamp(run.recordDate), color = Color.Gray, fontSize = 12.sp)
                    Text("코스: ${run.course.id}", color = WhiteTextColor, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${run.course.distance}m", color = PointColor, fontWeight = FontWeight.Bold)
                    Text(formatDuration(run.time), color = Color.LightGray, fontSize = 12.sp)
                }
            }

            // --- [수정] 펼쳐졌을 때 나타나는 상세 영역 ---
            if (expanded) {
                var isMapLoaded by remember { mutableStateOf(false) }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))

                // 1. 코스 지도 표시 영역
                val centerLat = (run.course.minLat + run.course.maxLat) / 2
                val centerLng = (run.course.minLng + run.course.maxLng) / 2

                // 코스 크기에 따른 동적 줌 (CommunityScreen 로직 재사용)
                val dynamicZoom = remember(run.course) {
                    val latDiff = run.course.maxLat - run.course.minLat
                    val lngDiff = run.course.maxLng - run.course.minLng
                    val maxDiff = maxOf(latDiff, lngDiff)
                    when {
                        maxDiff > 0.04 -> 13
                        maxDiff > 0.015 -> 14
                        maxDiff > 0.005 -> 15
                        else -> 16
                    }
                }

                val staticMapUrl = remember(run.recordDate) {
                    buildNaverStaticMapUrl(centerLat, centerLng, dynamicZoom)
                }


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)) // 지도 모서리도 둥글게 하면 세련돼 보입니다
                        .background(Color(0xFF2C2C2C))  // 지도 로드 전 배경색 (스켈레톤 느낌)
                ) {

                    // 1. Static Map
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(staticMapUrl)
                            .crossfade(true) // 부드러운 전환 효과
                            .addHeader("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_API_KEY)
                            .addHeader("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_API_SECRET_KEY)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { isMapLoaded = true }
                    )

                    if (isMapLoaded) {
                        // 2. 경로 그리기 (시작/종료 마커 포함)
                        Canvas(modifier = Modifier.fillMaxSize()) {

                            val points = run.course.locationPoints.map {
                                val (x, y) = latLngToPixel(
                                    it.locationPoint.latitude,
                                    it.locationPoint.longitude,
                                    centerLat,
                                    centerLng,
                                    dynamicZoom.toDouble(),
                                    size.width,
                                    size.height,
                                )
                                Offset(x, y)
                            }
                        }

                            // 🔹 2-1. 경로 데이터 생성 (Path 객체 사용)
                            val path = Path().apply {
                                points.forEachIndexed { index, point ->
                                    if (index == 0) {
                                        moveTo(point.x, point.y)
                                    } else {
                                        // 🔹 이전 노드가 '정지(stop)' 상태가 아닐 때만 선을 잇습니다.
                                        val prevNode = run.course.locationPoints[index - 1]
                                        if (!prevNode.stop) {
                                            lineTo(point.x, point.y)
                                        } else {
                                            // 정지 상태였다면 선을 긋지 않고 새로운 시작점으로 이동
                                            moveTo(point.x, point.y)
                                        }
                                    }
                                }
                            }

                            // 테두리
                            drawPath(
                                path = path,
                                color = Color.Black,
                                style = Stroke(
                                    width = 14f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round // 꺾이는 부분을 부드럽게
                                )
                            )

                            // 내부 선
                            drawPath(
                                path = path,
                                color = PointColor,
                                style = Stroke(
                                    width = 8f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // 2-2. 시작 및 종료 마커 추가
                            if (points.isNotEmpty()) {
                                val startPoint = points.first()
                                val endPoint = points.last()

                                // 시작 마커 (초록색)
                                drawMarker(
                                    center = startPoint,
                                    color = Color(0xFF4CAF50),
                                    "START"
                                )

                                // 종료 마커 (빨간색)
                                drawMarker(
                                    center = endPoint,
                                    color = Color(0xFFF44336),
                                    "END"
                                )
                            }
                        }
                        // 🔹 3. [추가] 지도 좌측 상단 점수 정보 패널
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart) // 좌측 상단 정렬
                                .padding(10.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)) // 반투명 검정 배경
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ScoreIndicator(label = "밝기", score = run.course.scores.brightScore)
                            ScoreIndicator(label = "붐빔", score = run.course.scores.crowdedScore)
                            ScoreIndicator(label = "난이도", score = run.course.scores.hardScore)
                        }
                    } else {
                        // 🔹 로딩 중일 때 보여줄 인디케이터 (선택 사항)
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).size(24.dp),
                            color = PointColor,
                            strokeWidth = 2.dp
                        )
                    }

                    // 🔹 3. [추가] 지도 좌측 상단 점수 정보 패널
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart) // 좌측 상단 정렬
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)) // 반투명 검정 배경
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ScoreIndicator(label = "밝기", score = run.course.scores.brightScore)
                        ScoreIndicator(label = "붐빔", score = run.course.scores.crowdedScore)
                        ScoreIndicator(label = "난이도", score = run.course.scores.hardScore)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. 상세 지표 영역 (평균 페이스 등)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DetailMetricItem("평균 페이스", calculatePace(run.time, run.course.distance.toDouble()))
                    DetailMetricItem("평균 속도", String.format("%.1f km/h", (run.course.distance / 1000.0) / (run.time / 3600000.0)))
                }
            }
        }
    }
}

fun buildNaverStaticMapUrl(
    centerLat: Double,
    centerLng: Double,
    zoom: Int
): String {
    return "https://maps.apigw.ntruss.com/map-static/v2/raster" +
            "?w=600&h=400" +
            "&center=$centerLng,$centerLat" + // ⚠️ 순서 중요 (경도,위도)
            "&level=$zoom" +
            "&maptype=basic" +
            "&format=png" +
            "&scale=2"
}

@Composable
fun GoalItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = WhiteTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = WhiteTextColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// 🔹 별점 표시용 소형 컴포넌트
@Composable
private fun ScoreIndicator(label: String, score: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label ",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp
        )
        // 별 아이콘 대신 텍스트와 주황색 수치로 깔끔하게 표시
        Text(
            text = "★ ${String.format("%.1f", score)}",
            color = Color(0xFFFF9800), // PointColor와 유사한 오렌지색
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// 시간 포맷팅 헬퍼 (ms -> 00:00:00)
fun formatDuration(ms: Int): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = ms / (1000 * 60 * 60)
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

// 목표 시간용 (seconds -> 00:00:00)
fun formatSeconds(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

