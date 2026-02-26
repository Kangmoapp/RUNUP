package com.example.runup.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.components.BorderButton
import com.example.runup.ui.components.RunupTextfield
import com.example.runup.ui.components.SignupText
import com.example.runup.ui.components.UnderlineButton
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.White

@Preview
@Composable
fun PreviewSignupScreen(){
    SignupEmailScreen({}, {})
}

@Composable
fun SignupEmailScreen(
    onContinueClick:()->Unit,
    onLoginClick:()->Unit
){
    var email by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackGroudColor
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            SignupText(text = "달리기를 시작해\n 볼까요?")
            RunupTextfield(
                value = email,
                onValueChange = {email = it},
                placeholderText = "이메일 주소를 입력해 주세요",
                textcolor = White,
                placeholdercolor = White,
                containercolor = Color.Transparent,
                indicatorcolor = White,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onContinueClick()
                    }
                ),
                modifier = Modifier
                    .padding(top= 50.dp)
                    .height(52.dp)
                    .width(365.dp)
            )
            Column(
                modifier = Modifier
                    .wrapContentWidth(),
                horizontalAlignment = Alignment.End
            ) {
                BorderButton(
                    text = "계속하기",
                    onClick = onContinueClick,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .padding(top = 50.dp)
                        .width(354.dp)
                        .height(60.dp)
                )
                UnderlineButton(
                    text = "로그인",
                    onClick = onLoginClick
                )
            }
        }
    }
}

