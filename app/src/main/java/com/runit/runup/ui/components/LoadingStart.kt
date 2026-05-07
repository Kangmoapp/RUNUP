package com.runit.runup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.runit.runup.ui.theme.PointColor

@Composable
fun LoadingStart(
    timeNumber: Int,
) {
    // 🔹 Surface 대신 Box를 사용하고 배경색을 투명하게 설정
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f)), // 👈 지도가 살짝 어두워지면 숫자가 더 잘 보여요
        contentAlignment = Alignment.Center
    ) {
        Box {
            // 1. 외곽선 (주황색)
            Text(
                text = timeNumber.toString(),
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    fontSize = 160.sp,
                    color = PointColor,
                    drawStyle = Stroke(width = 15f), // 외곽선 두께 조절
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(4f, 4f),
                        blurRadius = 8f
                    )
                )
            )

            // 2. 내부 채우기 (흰색)
            Text(
                text = timeNumber.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 160.sp,
                color = PointColor,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(4f, 4f),
                        blurRadius = 8f
                    )
                )
            )
        }
    }
}