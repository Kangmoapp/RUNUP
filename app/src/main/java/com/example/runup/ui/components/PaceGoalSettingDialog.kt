package com.example.runup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.White
import com.example.runup.ui.theme.WhiteTextColor

@Composable
fun PaceGoalSettingDialog(
    rangeMinutes: IntRange,
    rangeSeconds: IntRange = 0..59,
    startMinute: Int = 0,
    startSecond: Int = 0,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(startMinute) }
    var selectedSeconds by remember { mutableStateOf(startSecond) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.9f) // 숫자가 두 개라 너비를 조금 더 넉넉히 (0.9f)
            .clip(RoundedCornerShape(24.dp)),
        containerColor = BackGroudColor,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. 타이틀 📍
                Text(
                    text = "목표 페이스 설정",
                    color = WhiteTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // 2. 분/초 피커 영역 📍
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // --- [분 선택 영역] ---
                    Box(
                        modifier = Modifier.width(65.dp).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {}
                        RunupLazyColumn(
                            range = rangeMinutes,
                            startNumber = startMinute,
                            ItemHeight = 56,
                            textMapper = { it.toString() },
                            onSelectedNumberChange = { selectedMinutes = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = "분",
                        fontSize = 18.sp,
                        color = Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // --- [초 선택 영역] ---
                    Box(
                        modifier = Modifier.width(65.dp).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {}
                        RunupLazyColumn(
                            range = rangeSeconds,
                            startNumber = startSecond,
                            ItemHeight = 56,
                            textMapper = { String.format("%02d", it) }, // 초는 00, 01 처럼 두자리로!
                            onSelectedNumberChange = { selectedSeconds = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = "초",
                        fontSize = 18.sp,
                        color = Gray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            // 메인 버튼 (확인) 📍
            Button(
                onClick = { onConfirm(selectedMinutes, selectedSeconds) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PointColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("설정 완료", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            // 취소 버튼
            TextButton(onClick = onDismiss) {
                Text("취소", color = Gray, fontSize = 14.sp)
            }
        }
    )
}


@Preview
@Composable
fun PreviewPaceGoalSettingDialog(){
    PaceGoalSettingDialog(
        rangeMinutes = 0..60,
        rangeSeconds = 0..60,
        startMinute = 6,
        startSecond = 30,
        onConfirm = { _, _ -> },
        onDismiss = {}
    )
}