package com.example.runup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
fun DistanceGoalSettingDialog(
    titleName: String = "",
    range: IntRange,
    startNumber: Int = 0,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedNumber by remember { mutableStateOf(range.first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(24.dp)),
        containerColor = BackGroudColor,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. 타이틀
                Text(
                    text = titleName,
                    color = WhiteTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // ── 🔹 [핵심] 숫자 피커 + km 단위 영역 📍 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center // 👈 전체 묶음을 가로 중앙으로!
                ) {
                    // ── [A] 숫자 선택 영역 (배경과 리스트를 하나로 묶음) 📍 ──
                    Box(
                        modifier = Modifier
                            .width(70.dp) // 너비를 확실히 고정! (스크린샷보다 넓게 설정)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center // 내부 요소(배경, 숫자)를 중앙으로 집결
                    ) {
                        // 1. 하이라이트 배경 (박스 너비를 꽉 채움)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth() // 👈 Box 너비인 100.dp를 다 씀
                                .height(56.dp),
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {}

                        // 2. 숫자 리스트 (배경 위에 정확히 올림)
                        RunupLazyColumn(
                            range = range,
                            startNumber = startNumber,
                            ItemHeight = 56,
                            textMapper = { (it / 10.0).toString() },
                            onSelectedNumberChange = { number -> selectedNumber = number },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ── [B] 단위 표시 (피커와 겹치지 않게 거리 두기) 📍 ──
                    Spacer(modifier = Modifier.width(16.dp)) // 👈 겹침 방지용 안전거리

                    Text(
                        text = "km",
                        fontSize = 24.sp,
                        color = PointColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedNumber) },
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
            TextButton(onClick = onDismiss) {
                Text("취소", color = Gray, fontSize = 14.sp)
            }
        }
    )
}


@Preview
@Composable
fun PreviewDistanceScrollBox(){
    DistanceGoalSettingDialog(
        range = 0..100,
        startNumber = 20,
        onConfirm = {},
        onDismiss = {}
    )
}