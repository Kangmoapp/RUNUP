package com.example.runup.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.WhiteTextColor

@Composable
fun DetailMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = WhiteTextColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}