package com.example.runup.ui

import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import com.example.runup.R
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalConsumer
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
import com.example.runup.ui.theme.RunUpTheme
import com.example.runup.ui.theme.White

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunUpTheme {
                IntroUi()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IntroUiPreview() {
    IntroUi()
}

@Composable
fun IntroUi(){
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
                IntroButton(text = "예", onClick = {})
                IntroButton(text="아니요", onClick = {})
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

@Preview(showBackground = true)
@Composable
fun LoginPagePreview() {
    LoginPage()
}

@Composable
fun LoginPage(){
    Surface(modifier = Modifier
        .fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Image(
                painter = painterResource(id = R.drawable.yellow_shoes),
                contentDescription = null,
                contentScale = ContentScale.Crop, // 이미지가 꽉 차게 비율 조정
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape) // 원형으로 자르기
            )
            LoginBar("아이디 또는 이메일", modifier = Modifier.padding(top= 50.dp))
            LoginBar("비밀번호", modifier = Modifier.padding(top= 10.dp))
        }
    }
}

@Composable
fun LoginBar(
    text: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = text,
        onValueChange = {},
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Gray,
            focusedContainerColor = Black
        ),
        placeholder = {
            Text(text)
        },
        modifier = modifier
            .height(52.dp)
            .width(365.dp),

    )
}