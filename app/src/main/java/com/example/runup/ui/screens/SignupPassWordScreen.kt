package com.example.runup.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.ui.components.BorderButton
import com.example.runup.ui.components.RunupTextfield
import com.example.runup.ui.components.SignupText
import com.example.runup.ui.components.UnderlineButton
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.White
import com.example.runup.viewmodel.SignUpViewModel

@Preview
@Composable
fun PreviewPassWord(){
    SignupPassWordScreen({},{})
}

@Composable
fun SignupPassWordScreen(
    onContinueClick:()->Unit,
    onLoginClick:()->Unit,
    viewModel : SignUpViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsState()
    val passwordFocusRequester = remember { FocusRequester() }

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
            SignupText(text = "비밀번호 입력")
            RunupTextfield(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                placeholderText = "6자리 이상 입력해 주세요",
                textcolor = White,
                placeholdercolor = White,
                containercolor = Color.Transparent,
                indicatorcolor = White,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        passwordFocusRequester.requestFocus()
                    }
                ),
                modifier = Modifier
                    .padding(top= 10.dp)
                    .height(52.dp)
                    .width(365.dp)
            )
            SignupText(text = "비밀번호 확인", modifier = Modifier.padding(top = 50.dp))
            RunupTextfield(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                placeholderText = "다시 한번 입력해 주세요",
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
                    .padding(top= 10.dp)
                    .height(52.dp)
                    .width(365.dp)
                    .focusRequester(passwordFocusRequester)
            )
            Column(
                modifier = Modifier
                    .wrapContentWidth(),
                horizontalAlignment = Alignment.End
            ) {
                BorderButton(
                    text = "계속하기",
                    onClick = {
                        viewModel.signUp(onSuccess = onContinueClick)
                    },
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