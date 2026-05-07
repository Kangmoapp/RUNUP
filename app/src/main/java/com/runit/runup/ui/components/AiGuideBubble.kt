package com.runit.runup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runit.runup.ui.theme.PointColor

val guide_text = "원하시는 코스 특징을 입력해보세요!\n정확한 장소명이나, 밝기, 유동인구, 난이도를 넣어\n형식에 맞춰 질문하면 더 찾기 쉬워요!!\n" +
        "ex) '경북대학교 근처 난이도 높은 코스 있어?'\n" +
        "ex) '대현어린이공원 근처 유동인구 적은 코스 있어?\n'" +
        "ex) '밝고 난이도 낮은 코스 있어?'"

@Composable
fun AiGuideBubble(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.padding(bottom = 8.dp)
    ) {
        Surface(
            // 1. 단순 단색보다는 아주 깊은 네이비~보라 그라데이션 느낌의 투명도 조절
            color = Color(0xFF1A1C2E).copy(alpha = 0.9f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp), // 말풍선 느낌 강조
            border = BorderStroke(
                width = 1.dp,
                // 2. 테두리에 살짝 그라데이션이나 포인트 컬러의 낮은 투명도 적용
                brush = Brush.linearGradient(
                    colors = listOf(PointColor.copy(alpha = 0.6f), Color.Transparent)
                )
            ),
            shadowElevation = 8.dp,
            modifier = Modifier.wrapContentWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3. 텍스트 앞에 아주 작은 반짝임 아이콘 추가 (AI 느낌)
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PointColor,
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = guide_text,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.3).sp // 자간을 살짝 좁히면 더 세련되어 보임
                )
            }
        }
    }
}