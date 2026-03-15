package com.example.runup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.Black
import com.example.runup.ui.theme.White


@Composable
fun BorderButton(
    text:String,
    onClick:()->Unit,
    fontSize: TextUnit = 30.sp,
    modifier:Modifier = Modifier
){
    Button(
        onClick=onClick,
        modifier=modifier,
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Black
        ),
        border = BorderStroke(3.dp, White)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = White,
            fontSize = fontSize
        )
    }
}