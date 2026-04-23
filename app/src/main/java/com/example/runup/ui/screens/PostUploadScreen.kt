package com.example.runup.ui.screens

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.ui.util.mapper.TimeMapper.formatTimestamp
import com.example.runup.viewmodel.CommunityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostUploadScreen(
    onBackClick: () -> Unit,
    onUploadSuccess: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel<CommunityViewModel>()
) {
    var content by remember { mutableStateOf("") }
    val selectedLocationImageUris = viewModel.selectedLocationImageUris
    val selectedCommonImageUris = viewModel.selectedCommonImageUris

    // 추가된 상태 관찰
    val uiState by viewModel.postUploadUiState.collectAsState()

    // 미디어 위치 권한 요청 런처 추가
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("Exif", "미디어 위치 권한 승인됨")
        } else {
            Log.d("Exif", "미디어 위치 권한 거부됨 - 좌표 추출이 불가능할 수 있음")
        }
    }

    // 화면 진입 시 권한 체크 및 요청
    LaunchedEffect(Unit) {
        Log.d("Exif", "LaunchedEffect 시작")
        viewModel.clearSelectedImages()
        viewModel.fetchMyRunRecords()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permission = android.Manifest.permission.ACCESS_MEDIA_LOCATION
            Log.d("Exif", "권한 요청 시도: $permission")
            permissionLauncher.launch(permission)
        }


    }

    // PickMultipleVisualMedia 대신 GetMultipleContents 사용
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents() // 변경됨
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addSelectedLocationImages(uris)
        }
    }

    val commonGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents() // 변경됨
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addSelectedCommonImages(uris)
        }
    }

    BackHandler { // 안드로이드 뒤로가기 버튼
        viewModel.clearSelectedImages()
        onBackClick()
    }

    // --- 러닝 기록 선택 바텀 시트 ---
    if (uiState.isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setSheetOpen(false) },
            containerColor = Color(0xFF1C1C1C)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
                    .heightIn(max = 500.dp)
            ) {
                item {
                    Text("내 러닝 기록", color = WhiteTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // 초기 로딩 중일 때
                if (uiState.isLoading && uiState.runRecords.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PointColor)
                        }
                    }
                } else {
                    items(uiState.runRecords) { record ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectRunRecord(record) }
                                .padding(vertical = 12.dp)
                        ) {
                            // 🔹 날짜 포맷팅 적용 (TimeMapper 사용 추천)
                            Text(formatTimestamp(record.recordDate), color = Color.Gray, fontSize = 12.sp)
                            Text("${String.format("%.2f", record.course.distance / 1000.0)}km 러닝", color = WhiteTextColor, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color.DarkGray)
                    }

                    // 🔹 [추가] 더 보기 버튼 섹션
                    if (uiState.hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isPaging) {
                                    CircularProgressIndicator(color = PointColor, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = "이전 기록 더 보기 ▾",
                                        color = PointColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { viewModel.fetchMyRunRecords(isInitial = false) }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = BackGroudColor,
        topBar = { TopBar(onBackClick = onBackClick, text = "새 게시물", isMenu = false) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- [추가] 러닝 기록 선택 영역 ---
            Text("활동 기록", color = WhiteTextColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // UI 분기: 선택된 기록 여부에 따라 표시
            if (uiState.selectedRunRecord == null) {
                OutlinedButton(
                    onClick = { viewModel.setSheetOpen(true) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhiteTextColor)
                ) {
                    Text("🏃 공유할 러닝 기록 불러오기")
                }
            } else {
                val selected = uiState.selectedRunRecord!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF333333), MaterialTheme.shapes.medium)
                        .clickable { viewModel.setSheetOpen(true) }
                        .padding(16.dp)
                ) {
                    Column {
                        Text("선택된 기록", color = Color(0xFF4A90E2), fontSize = 12.sp)
                        Text("${selected.recordDate} 러닝", color = WhiteTextColor, fontWeight = FontWeight.Bold)
                        Text("${selected.course.distance}km | ${selected.time}", color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 위치 사진 선택 및 미리보기 영역 ---
            if (selectedLocationImageUris.isEmpty()) {
                OutlinedButton(
                    onClick = {
                        if (selectedLocationImageUris.size < 4) {
                            galleryLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhiteTextColor)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("코스 주변에서 찍은 사진을 추가해주세요!", fontSize = 16.sp)
                        Text("4장까지 선택 가능합니다!", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                ) {
                    items(selectedLocationImageUris) { uri ->
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
                                onClick = { viewModel.removeLocationImage(uri) },
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
                    if (selectedLocationImageUris.size < 4) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .fillMaxHeight()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(Color(0xFF333333))
                                    .clickable {
                                        galleryLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("+ 추가", color = WhiteTextColor, fontSize = 16.sp)
                                    Text("${selectedLocationImageUris.size}/4장 선택됨", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 일반 사진 선택 및 미리보기 영역 ---
            if (selectedCommonImageUris.isEmpty()) {
                OutlinedButton(
                    onClick = {
                        commonGalleryLauncher.launch(
                            "image/*"
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhiteTextColor)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("그 외 사진 추가하기", fontSize = 16.sp)
                        Text("여러 장 선택이 가능합니다!", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                ) {
                    items(selectedCommonImageUris) { uri ->
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
                                onClick = { viewModel.removeCommonImage(uri) },
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
                                        "image/*"
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("+ 추가", color = WhiteTextColor, fontSize = 16.sp)
                                Text("${selectedCommonImageUris.size}장 선택됨", color = Color.Gray, fontSize = 12.sp)
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
                modifier = Modifier.fillMaxWidth().height(60.dp),
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
                enabled = uiState.selectedRunRecord != null && selectedLocationImageUris.isNotEmpty() && !uiState.isLoading, //선택된 코스가 없거나, 선택된 사진이 없거나, 내용이 비어있지 않고, uiState가 로딩 중이 아닐때
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2),
                    disabledContainerColor = Color.Gray
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "공유하기 (${selectedLocationImageUris.size + selectedCommonImageUris.size}장)",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}