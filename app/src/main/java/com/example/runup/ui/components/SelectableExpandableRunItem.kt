package com.example.runup.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.runup.BuildConfig
import com.example.runup.ui.screens.buildNaverStaticMapUrl
import com.example.runup.ui.screens.drawMarker
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.ui.util.calculatePace
import com.example.runup.ui.util.latLngToPixel
import com.example.runup.ui.util.mapper.TimeMapper.formatDuration

@Composable
fun SelectableExpandableRunItem(
    run: com.example.runup.domain.model.RunRecord,
    onSelect: () -> Unit // 본체 클릭 시 선택
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252525)),
        // 테두리를 살짝 주어 본체와 구별되게 함
        border = BorderStroke(1.dp, if (expanded) PointColor.copy(alpha = 0.5f) else Color.Transparent)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp, start = 18.dp, end = 10.dp)) {
            // [A] 상단 헤더 영역 (MyPage 디자인과 동일)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 왼쪽 정보 (날짜, 코스 ID)
                Column(modifier = Modifier.weight(1f).clickable { onSelect() }) {
                    Text(
                        text = com.example.runup.ui.util.mapper.TimeMapper.formatTimestamp(run.recordDate),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = run.course.id,
                        color = WhiteTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // 2. 오른쪽 정보 (거리, 시간) + 펼치기 버튼 📍
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.clickable { onSelect() }) {
                        Text(
                            text = "${run.course.distance}m",
                            color = PointColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = formatDuration(run.time),
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // ── 🔹 노란색 펼치기 버튼 (하단 버튼 삭제됨) 📍 ──
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "펼치기",
                        tint = PointColor,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { expanded = !expanded } // 👈 얘만 확장 담당
                            .graphicsLayer { rotationZ = rotationState }
                            .padding(4.dp)
                    )
                }
            }

            // [B] 상세 내용 (지도 및 점수) - Expanded 상태일 때만
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
                            .addHeader("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_API_KEY_MW)
                            .addHeader("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_API_SECRET_KEY_MW)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { isMapLoaded = true }
                    )

                    if (isMapLoaded) {
                        // 2. 경로 그리기 (시작/종료 마커 포함)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // 🔹 핵심: 모든 드로잉 로직은 이 Canvas { ... } 블록 안에 있어야 합니다! 📍
                            val points = run.course.locationPoints.map {
                                val (x, y) = latLngToPixel(
                                    it.locationPoint.latitude,
                                    it.locationPoint.longitude,
                                    centerLat,
                                    centerLng,
                                    dynamicZoom.toDouble(),
                                    size.width,
                                    size.height
                                )
                                Offset(x, y)
                            }

                            if (points.isNotEmpty()) {
                                val path = Path().apply {
                                    points.forEachIndexed { i, p ->
                                        if (i == 0) moveTo(p.x, p.y)
                                        else {
                                            if (!run.course.locationPoints[i - 1].stop) lineTo(p.x, p.y)
                                            else moveTo(p.x, p.y)
                                        }
                                    }
                                }

                                // 경로 선 그리기
                                drawPath(path, Color.Black, style = Stroke(14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                drawPath(path, PointColor, style = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                                // 시작/종료 마커
                                drawMarker(points.first(), Color(0xFF4CAF50), "START")
                                drawMarker(points.last(), Color(0xFFF44336), "END")
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

@Composable
fun MiniScoreIndicator(label: String, score: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(score.toString(), color = PointColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}