package com.example.runup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.ui.util.calculateCalories
import com.example.runup.ui.util.mapper.DistanceMapper
import com.example.runup.viewmodel.RunningUiState

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