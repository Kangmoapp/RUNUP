package com.example.runup.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.runup.ui.components.MenuBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.viewmodel.CommunityViewModel

@Composable
fun PostUploadScreen(
    onBackClick: () -> Unit,
    onUploadSuccess: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel<CommunityViewModel>()
) {
    var content by remember { mutableStateOf("") }
    val selectedImageUris = viewModel.selectedImageUris

    // 여러 장 선택 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addSelectedImages(uris)
        }
    }

    BackHandler {
        viewModel.clearSelectedImages()
        onBackClick()
    }

    Scaffold(
        containerColor = BackGroudColor,
        /*
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    BackButton(onClick = {
                        viewModel.clearSelectedImages()
                        onBackClick()
                    })
                }
                Text(
                    text = "새 게시물",
                    color = WhiteTextColor,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
         */
        topBar = { MenuBar(onBackClick = onBackClick, text = "새 게시물", isMenu = false) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- 사진 선택 및 미리보기 영역 ---
            if (selectedImageUris.isEmpty()) {
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhiteTextColor)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("사진 추가하기", fontSize = 16.sp)
                        Text("여러 장 선택이 가능합니다", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                ) {
                    items(selectedImageUris) { uri ->
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(200.dp)
                                    .fillMaxHeight()
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                            // 개별 사진 삭제 버튼
                            IconButton(
                                onClick = { viewModel.removeImage(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small)
                                    .size(28.dp)
                            ) {
                                Text("✕", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                    // 추가 버튼 아이템
                    item {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .clip(MaterialTheme.shapes.medium)
                                .background(Color(0xFF333333))
                                .clickable {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("+ 추가", color = WhiteTextColor, fontSize = 16.sp)
                                Text("${selectedImageUris.size}장 선택됨", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 본문 입력창 ---
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("오늘의 러닝은 어떠셨나요?", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = WhiteTextColor,
                    unfocusedTextColor = WhiteTextColor,
                    focusedContainerColor = Color(0xFF2C2C2C),
                    unfocusedContainerColor = Color(0xFF2C2C2C),
                    cursorColor = WhiteTextColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- 업로드 버튼 ---
            Button(
                onClick = {
                    viewModel.uploadPost(content = content) {
                        onUploadSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = selectedImageUris.isNotEmpty() && content.isNotBlank() && !viewModel.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2),
                    disabledContainerColor = Color.Gray
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "공유하기 (${selectedImageUris.size}장)",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}