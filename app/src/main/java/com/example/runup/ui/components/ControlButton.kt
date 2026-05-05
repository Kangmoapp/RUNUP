package com.example.runup.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun ControlButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White, // ── 🔹 기본값은 화이트로 설정 📍 ──
    isLoading: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(width = 85.dp, height = 50.dp)
            .background(color = color, shape = RoundedCornerShape(12.dp))
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor, // ── 🔹 인디케이터도 텍스트 색상을 따라갑니다 📍 ──
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = contentColor, // ── 🔹 넘겨받은 색상 적용 📍 ──
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}