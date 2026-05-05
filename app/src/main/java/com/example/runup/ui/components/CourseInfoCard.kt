package com.example.runup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.domain.model.CourseRecommendation
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.TextGray
import com.example.runup.ui.theme.White
import com.example.runup.viewmodel.CourseRecommendationUiState
import kotlin.text.ifEmpty

@Composable
fun CourseInfoCard(
    uiState: CourseRecommendationUiState,
    course: CourseRecommendation,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(120.dp)
            // ── 🔹 [핵심] 층 분리 현상 박멸 레이어 📍 ──
            .graphicsLayer {
                // 이 설정이 "투명한 배경 뒤로 그림자가 비치는 현상"을 해결하는 치트키입니다.
                compositingStrategy = CompositingStrategy.Offscreen
            }
            // 1. 그림자 설정 (너무 진하면 층이 보일 수 있으니 값을 살짝 조절)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.2f), // 그림자 자체를 연하게
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            // 2. 배경과 테두리
            .background(
                color = White.copy(alpha = 0.8f), // 지도가 더 잘 보이게 0.7f로 상향
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                border = BorderStroke(1.dp, Color.Black),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp) // 내부 컨텐츠 여백
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 1. 헤더: 코스 번호와 거리
            Column {
                Text(
                    text = "${uiState.courseIndex + 1}. ${course.originCourse.id}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = PointColor,
                    letterSpacing = 0.5.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.2f", course.path.distance / 1000.0),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextBlack,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "km",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        modifier = Modifier.padding(start = 1.dp, bottom = 3.dp)
                    )
                }
            }

            // 2. 중간: 주변 장소
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = PointColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "주변",
                        fontSize = 9.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 쉼표(,)를 기준으로 문자열을 나누고 공백을 제거한 리스트 생성
                val landmarkList = course.originCourse.landmark
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (landmarkList.isEmpty()) {
                    Text(
                        text = "정보 없음",
                        fontSize = 11.sp,
                        color = TextBlack,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    // 각 장소명을 순회하며 별도의 Text로 출력 (Column 안이므로 세로로 쌓임)
                    landmarkList.forEach { name ->
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            color = TextBlack,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Gray.copy(0.1f)))

            // 3. 하단: 스코어 (텍스트 기반 초밀착) 📍
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactTextScore("밝기", course.originCourse.scores.brightScore)
                CompactTextScore("유동인구", course.originCourse.scores.crowdedScore)
                CompactTextScore("난이도", course.originCourse.scores.hardScore)
            }
        }
    }
}

@Composable
private fun CompactTextScore(label: String, score: Double) {
    // ── 🔹 이모지 대신 한글 텍스트 + 점수를 세로 배치하여 가로 폭 절약 📍 ──
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontSize = 8.sp, // 아주 작게
            fontWeight = FontWeight.Medium,
            color = TextGray,
            letterSpacing = (-0.5).sp // 자간 축소
        )
        Text(
            text = String.format("%.2f", score),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextBlack
        )
    }
}