package com.example.runup.ui.screens

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.runup.ui.components.SelectableExpandableRunItem
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.ui.util.mapper.TimeMapper.formatDuration
import com.example.runup.ui.util.mapper.TimeMapper.formatTimestamp
import com.example.runup.viewmodel.CommunityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostUploadScreen(
    onBackClick: () -> Unit,
    onBackHandlerClick: () -> Unit,
    onUploadSuccess: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel<CommunityViewModel>()
) {
    var content by remember { mutableStateOf("") }
    val selectedLocationImageUris = viewModel.selectedLocationImageUris
    val selectedCommonImageUris = viewModel.selectedCommonImageUris

    // 추가된 상태 관찰
    val uiState by viewModel.postUploadUiState.collectAsState()
    // 시트 상태 정의 (맨 위 확장 상태로 고정하기 위함)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { newValue ->
        // 여기서는 기본적으로 true를 반환하되, 스와이프 시 너무 민감하게 반응하지 않도록 skipPartiallyExpanded가 이미 돕고 있습니다.
        true
    })

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

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetUploadState()
        }
    }

    // 화면 진입 시 권한 체크 및 요청
    LaunchedEffect(Unit) {
        Log.d("Exif", "LaunchedEffect 시작")
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
        onBackHandlerClick()
    }

    // --- 러닝 기록 선택 바텀 시트 ---
    if (uiState.isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setSheetOpen(false) },
            sheetState = sheetState,
            containerColor = Color(0xFF1C1C1C)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .heightIn(min = 500.dp, max = 600.dp)
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
                        SelectableExpandableRunItem(
                            run = record,
                            onSelect = {
                                viewModel.selectRunRecord(record)
                                viewModel.setSheetOpen(false) // 선택 후 시트 닫기
                            }
                        )
                    }

                    // 🔹 [추가] 더 보기 버튼 섹션
                    if (uiState.hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
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
                .fillMaxSize()
                .padding(horizontal = 20.dp) // 여백을 살짝 넓혀서 고급스럽게 🔹
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── [1] 활동 기록 선택 영역 ──
            SectionTitle("활동 기록", Icons.Default.DirectionsRun) // 커스텀 타이틀 컴포저블 🔹

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.selectedRunRecord == null) {
                // 선택 전: 깔끔한 대시보드 스타일 버튼
                Surface(
                    onClick = { viewModel.setSheetOpen(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF252525),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = PointColor)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("공유할 러닝 기록 선택", color = WhiteTextColor, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // 선택 후: 정보가 한눈에 들어오는 카드 형태 🔹
                val selected = uiState.selectedRunRecord!!
                Card(
                    onClick = { viewModel.setSheetOpen(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)) // MyPage와 통일
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // ── 🔹 [상단] 강조 라벨 (노란 점 + 선택된 기록) ──
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PointColor) // RUNUP 포인트 컬러
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "선택된 기록",
                                color = PointColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp)) // 라벨과 데이터 사이 간격

                        // ── 🔹 [하단] 상세 데이터 영역 (좌우 정렬) ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom // 날짜와 시간이 바닥 선에 맞게 정렬
                        ) {
                            // [왼쪽] 날짜 및 코스 ID
                            Column {
                                Text(
                                    text = formatTimestamp(selected.recordDate),
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = selected.course.id,
                                    color = WhiteTextColor,
                                    fontWeight = FontWeight.ExtraBold, // 조금 더 강조
                                    fontSize = 18.sp
                                )
                            }

                            // [오른쪽] 거리 및 시간 (우측 정렬)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${selected.course.distance}m",
                                    color = PointColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = formatDuration(selected.time),
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── [2] 코스 사진 선택 (Location Images) ──
            SectionTitle("코스 추천 사진", Icons.Default.PhotoCamera)
            Text("코스의 특징이 잘 나타난 사진을 올려주세요 (최대 4장)", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

            Spacer(modifier = Modifier.height(16.dp))

            ImageSelectionRow(
                uris = selectedLocationImageUris,
                maxCount = 4,
                onAddClick = { galleryLauncher.launch("image/*") },
                onRemoveClick = { viewModel.removeLocationImage(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── [3] 일반 사진 선택 (Common Images) ──
            SectionTitle("그 외 사진", Icons.Default.Collections)

            Spacer(modifier = Modifier.height(16.dp))

            ImageSelectionRow(
                uris = selectedCommonImageUris,
                maxCount = 10, // 여유 있게 설정
                onAddClick = { commonGalleryLauncher.launch("image/*") },
                onRemoveClick = { viewModel.removeCommonImage(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── [4] 본문 입력창 ──
            SectionTitle("내용", Icons.Default.Description)

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("오늘의 러닝은 어떠셨나요?", color = Color.DarkGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp), // 🔹 높이를 넓혀서 시원하게 입력 가능
                colors = TextFieldDefaults.colors(
                    focusedTextColor = WhiteTextColor,
                    unfocusedTextColor = WhiteTextColor,
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    cursorColor = PointColor,
                    focusedIndicatorColor = PointColor,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── [5] 업로드 버튼 ──
            Button(
                onClick = { viewModel.uploadPost(content = content) { onUploadSuccess() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState.selectedRunRecord != null &&  !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PointColor, // 🔹 브랜드 컬러로 강조
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                    contentColor = Color.Black // 포인트 컬러 위에는 검정색 글씨가 더 세련됨 🔹
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "공유하기 (${selectedLocationImageUris.size + selectedCommonImageUris.size}장)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── 💡 디자인 통일성을 위한 서브 컴포저블들 ──

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = PointColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = WhiteTextColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun ImageSelectionRow(
    uris: List<Uri>,
    maxCount: Int,
    onAddClick: () -> Unit,
    onRemoveClick: (Uri) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(uris) { uri ->
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 삭제 버튼 디자인 개선 🔹
                IconButton(
                    onClick = { onRemoveClick(uri) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }

        if (uris.size < maxCount) {
            item {
                Surface(
                    onClick = onAddClick,
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF252525),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null, // 또는 "추가" 같은 설명
                            tint = Color.Gray          // color 대신 tint 사용! 🔹
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${uris.size}/$maxCount", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}