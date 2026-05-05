package com.example.runup.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.navigation.CourseProgress
import com.example.runup.ui.navigation.HomeUi
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.ui.util.calculateCalories
import com.example.runup.viewmodel.HomeTab
import com.example.runup.viewmodel.RunningUiState

@Composable
fun BottomSection(
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

    val imeHeightPxFloat = imeHeightPx.toFloat()
    val screenHeightPx = LocalConfiguration.current.screenHeightDp.let {
        with(density) { it.dp.toPx() }
    }


    val navBarHeight = with(density) { (screenHeightPx * 0.095f).toDp() }
    val hiddenHeightPx       = screenHeightPx * 0.071f  // 60 / 844
    val collapsedHeightPx    = screenHeightPx * 0.130f  // 110 / 844
    val recommendTabHeightPx = screenHeightPx * 0.237f  // 200 / 844
    val expandedHeightPx     = screenHeightPx * 0.652f  // 550 / 844

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
                            color = PointColor,
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
                        if (homeUi == HomeUi.RUN) {
                            onHeightChange(
                                (sheetHeightPx - delta).coerceIn(
                                    hiddenHeightPx,
                                    maxAllowedHeight
                                )
                            )
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
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding()
                    ),
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
                                if (!isResultLocked && homeUi != HomeUi.RECOMMEND) { // 👈 평가 중에는 탭 클릭 무시
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