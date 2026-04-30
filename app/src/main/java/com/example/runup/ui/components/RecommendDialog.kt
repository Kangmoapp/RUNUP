package com.example.runup.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.runup.domain.model.SortType
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.White
import com.example.runup.ui.theme.WhiteTextColor

@Composable
fun LoopSelectionDialog(
    isLoop: Boolean,
    onSelect: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = BackGroudColor // 앱 메인 다크 배경색
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 타이틀 📍
                Text(
                    text = "경로 방식 설정",
                    color = WhiteTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. 선택 옵션 리스트 📍
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 왕복 옵션
                    LoopOptionItem(
                        title = "왕복 경로",
                        description = "출발지로 되돌아오는 코스",
                        isSelected = isLoop,
                        onClick = {
                            onSelect(true)
                            onDismiss()
                        }
                    )

                    // 편도 옵션
                    LoopOptionItem(
                        title = "편도 경로",
                        description = "목적지에서 종료되는 코스",
                        isSelected = !isLoop,
                        onClick = {
                            onSelect(false)
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. 닫기 버튼
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("취소", color = Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun LoopOptionItem(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        // 선택된 상태일 때만 포인트 컬러 경계선 또는 투명도 있는 배경 적용 📍
        color = if (isSelected) PointColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PointColor) else null
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    color = if (isSelected) PointColor else WhiteTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = description,
                    color = Gray,
                    fontSize = 12.sp
                )
            }

            // 선택 표시 라디오 아이콘 느낌
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(PointColor, androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}