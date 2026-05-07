package com.runit.runup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runit.runup.domain.model.FilterState
import com.runit.runup.domain.model.FilterType
import com.runit.runup.domain.model.ViewScope
import com.runit.runup.ui.theme.PointColor
import com.runit.runup.ui.theme.WhiteTextColor


@Composable
fun ScopeButton(
    label: String,
    target: ViewScope,
    current: ViewScope,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isSelected = target == current
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) PointColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isSelected) PointColor else Color.Transparent)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (isSelected) PointColor else WhiteTextColor.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// 2. 지역 메뉴 아이템 내용
@Composable
fun LocationMenuContent(emoji: String, title: String, sub: String, isSelected: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(emoji, fontSize = 16.sp)
        Column {
            Text(title, color = if (isSelected) PointColor else WhiteTextColor, fontSize = 13.sp)
            Text(sub, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

// 3. 선택된 지역 텍스트 가공용
fun getSelectedLocationText(filter: FilterState): String {
    return if (filter.type == FilterType.CUSTOM_LOCATION) {
        "${filter.city} ${filter.district} ${filter.dong}".trim()
    } else {
        "시/도 · 구/군 · 동 선택"
    }
}