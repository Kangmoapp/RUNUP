package com.example.runup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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