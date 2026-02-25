package com.example.runup.ui

import com.example.runup.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.Black
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.White



@Preview(showBackground = true)
@Composable
fun IntroUiPreview() {
    TutorialScreen({}, {})
}

@Composable
fun TutorialScreen(
    onYesClick:()-> Unit,
    onNoClick:()->Unit
){
    Surface(modifier = Modifier
        .fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ){
            Intro()
            Row(
                horizontalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                IntroButton(text = "예", onClick = onYesClick)
                IntroButton(text="아니요", onClick = onNoClick)
            }
        }
    }
}

@Composable
fun Intro(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = PointColor)) {
                append("RunUp")
            }
            append("을 처음\n사용하시나요?")
        },
        fontSize = 48.sp,
        color = White
    )
}

@Composable
fun IntroButton(
    text:String,
    onClick:()->Unit,
    modifier:Modifier = Modifier
){
    Button(
        onClick=onClick,
        modifier=modifier
            .width(140 .dp)
            .height(77.dp),
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Black
        ),
        border = BorderStroke(3.dp, White)
    ) {
        Text(
            text = text,
            color = White,
            fontSize = 30.sp
            )
    }
}