package com.example.runup.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.navigation.TermsTexts
import com.example.runup.ui.navigation.TermsType
import com.example.runup.ui.navigation.getTermsText


@Preview
@Composable
fun prevTermsScreen() {
    TermsScreen(
        termsType = TermsType.SERVICE,
        onBackClick = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    termsType: TermsType = TermsType.SERVICE,
    onBackClick: () -> Unit
) {
    var selectedTermsType by remember {
        mutableStateOf(termsType)
    }

    BackHandler {
        onBackClick()
    }
    val termsText = remember(selectedTermsType) {
        getTermsText(selectedTermsType)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "이용 약관",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.Black
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TermsTypeTabs(
                selectedTermsType = selectedTermsType,
                onTermsTypeClick = {
                    selectedTermsType = it
                }
            )

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = termsText,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFF222222)
                )
            }
        }
    }
}

@Composable
private fun TermsTypeTabs(
    selectedTermsType: TermsType,
    onTermsTypeClick: (TermsType) -> Unit
) {
    val horizontalScrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .horizontalScroll(horizontalScrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TermsType.entries.forEach { type ->
            val isSelected = selectedTermsType == type

            Text(
                text = type.title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF444444),
                modifier = Modifier
                    .height(44.dp)
                    .widthIn(min = 120.dp)
                    .background(
                        color = if (isSelected) Color(0xFF555555) else Color.White
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFD0D0D0)
                    )
                    .clickable {
                        onTermsTypeClick(type)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}