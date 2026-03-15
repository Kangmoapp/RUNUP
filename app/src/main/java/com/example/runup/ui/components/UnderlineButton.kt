package com.example.runup.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.White

@Composable
fun UnderlineButton(
    text:String,
    fontsize: TextUnit = 15.sp,
    textcolor:Color = White,
    onClick:()->Unit,
    modifier : Modifier = Modifier
        .wrapContentSize()
        .padding(top = 5.dp)
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(start = 5.dp, end = 5.dp, top = 0.dp, bottom = 0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RectangleShape,
        modifier = modifier
    ) {
        Text(
            text = text,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.Center,
            color = textcolor,
            fontSize = fontsize
        )
    }
}