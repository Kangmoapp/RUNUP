package com.example.runup.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.R
import com.example.runup.ui.components.RunupTextfield
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.Black
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.White

@Preview(showBackground = true)
@Composable
fun LoginPagePreview() {
    LoginScreen({})
}

@Composable
fun LoginScreen(
    onLoginClick:()->Unit
){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val passwordFocusRequester = remember { FocusRequester() }

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
            RunupTextfield(
                value = email,
                onValueChange = {email = it},
                placeholderText = "이메일",
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        passwordFocusRequester.requestFocus()
                    }
                ),
                modifier = Modifier
                    .padding(top= 50.dp)
                    .height(52.dp)
                    .width(365.dp)
            )
            RunupTextfield(
                value = password,
                onValueChange = {password = it},
                placeholderText = "비밀번호",
                isPassword = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onLoginClick()
                    }
                ),
                modifier = Modifier
                    .padding(top= 10.dp)
                    .height(52.dp)
                    .width(365.dp)
                    .focusRequester(passwordFocusRequester)
            )
            Button(
                onClick = onLoginClick,
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PointColor
                ),
                modifier = Modifier
                    .width(300.dp)
                    .height(60.dp)
                    .padding(top = 15.dp)
            ) {
                Text(
                    text = "로그인",
                    textAlign = TextAlign.Center,
                    color = Black,
                    fontSize = 20.sp
                )
            }
        }
    }
}
