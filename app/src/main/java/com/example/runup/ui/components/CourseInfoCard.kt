package com.example.runup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
    Surface(
        modifier = modifier
            .width(150.dp) // ── 🔹 폭을 150dp로 더 콤팩트하게! 📍
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(16.dp)),
        color = White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Gray.copy(alpha = 0.1f)) // 아주 얇은 테두리로 선명함 추가
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. 헤더: 코스 번호와 거리
            Column {
                Text(
                    text = "COURSE ${uiState.courseIndex + 1}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = PointColor,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.2f", course.path.distance / 1000.0),
                        fontSize = 24.sp, // 거리를 더 강조
                        fontWeight = FontWeight.Black,
                        color = TextBlack
                    )
                    Text(
                        text = "km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )
                }
            }

            // 2. 중간: 주변 장소 (세로로 길어져도 괜찮게 배치)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = PointColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "주변 장소",
                        fontSize = 9.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = course.originCourse.landmark.ifEmpty { "정보 없음" },
                    fontSize = 12.sp,
                    color = TextBlack,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2, // 세로로 길어지도록 2줄 허용 📍
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Gray.copy(0.2f)))

            // 3. 하단: 스코어 (가로로 꽉 차게)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CompactScore("💡", course.originCourse.scores.brightScore)
                CompactScore("👥", course.originCourse.scores.crowdedScore)
                CompactScore("⛰️", course.originCourse.scores.hardScore)
            }
        }
    }
}

@Composable
private fun CompactScore(emoji: String, score: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 11.sp)
        Text(
            text = String.format("%.1f", score),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextBlack
        )
    }
}