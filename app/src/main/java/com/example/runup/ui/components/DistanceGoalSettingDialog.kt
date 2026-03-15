package com.example.runup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.BlackTextColor
import com.example.runup.ui.theme.White
import kotlin.div
import kotlin.toString

@Composable
fun DistanceGoalSettingDialog(
    range: IntRange,
    startNumber: Int = 0,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedNumber by remember { mutableStateOf(range.first) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedNumber) },
                shape = RectangleShape,
            ) {
                Text(
                    text = "확인",
                    color = BlackTextColor)
            }
        },
        text = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                RunupLazyColumn(
                    range = range,
                    startNumber = startNumber,
                    textMapper = { (it / 10.0).toString() },
                    onSelectedNumberChange = { number ->
                        selectedNumber = number
                    }
                )
                Text(
                    text = "km",
                    fontSize = 30.sp,
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(start = 10.dp)
                )
            }
        },
        containerColor = White,
        shape = RoundedCornerShape(15.dp),
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