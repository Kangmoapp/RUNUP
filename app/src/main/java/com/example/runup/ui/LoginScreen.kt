package com.example.runup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.runup.R
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.Black
import com.example.runup.ui.theme.Gray

@Preview(showBackground = true)
@Composable
fun LoginPagePreview() {
    LoginScreen()
}

@Composable
fun LoginScreen(){
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