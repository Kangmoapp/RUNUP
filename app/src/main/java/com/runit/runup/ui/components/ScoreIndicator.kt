package com.runit.runup.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 🔹 별점 표시용 소형 컴포넌트
@Composable
fun ScoreIndicator(label: String, score: Double) {
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