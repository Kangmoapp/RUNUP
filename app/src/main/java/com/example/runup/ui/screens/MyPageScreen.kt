package com.example.runup.ui.screens

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.runup.BuildConfig
import com.example.runup.domain.model.RunRecord
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.ui.util.mapper.TimeMapper
import com.example.runup.viewmodel.MyPageViewModel
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.runup.domain.model.RunFilter
import com.example.runup.ui.components.DetailMetricItem
import com.example.runup.ui.components.FriendListDialog
import com.example.runup.ui.components.GoalItem
import com.example.runup.ui.components.ScoreIndicator
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.util.calculatePace
import com.example.runup.ui.util.latLngToPixel
import com.example.runup.ui.util.mapper.DistanceMapper
import com.example.runup.ui.util.mapper.TimeMapper.formatDuration
import com.example.runup.ui.util.mapper.TimeMapper.formatSeconds
import com.example.runup.viewmodel.CommunityViewModel
import com.example.runup.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    onBackClick: () -> Unit,
    onPostClick: (String) -> Unit,
    onUploadClick: () -> Unit,
    viewModel: MyPageViewModel = hiltViewModel(),
    communityViewModel: CommunityViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
) {
    val context = LocalContext.current // 토스트를 위해 컨텍스트 가져오기

    val userData by viewModel.userState.collectAsState()
    val profileBitmaps by viewModel.profileBitmaps.collectAsState()
    val runState by viewModel.runState.collectAsState()

    // 내 프로필 URL 추출
    val myProfileUrl = userData?.userProfileUrl ?: ""
    // Map에서 내 URL에 해당하는 비트맵만 찾기
    val myBitmap = profileBitmaps[myProfileUrl]


    var showFriendDialog by remember { mutableStateOf(false) } // 친구 다이얼로그 상태 추가

    val scrollState = rememberLazyListState() // 스크롤 상태
    val runListStartIndex = 4

    // 🔹 스낵바를 관리하는 상태와 실행을 위한 스코프 추가
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeActionItemId by remember { mutableStateOf<Long?>(null) }

    // 갤러리 실행기 설정
    val profileGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent() // 변경됨
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfileImage(it) }
    }

    // 이름 수정을 위한 상태값
    var showEditDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // 화면 진입 시 데이터 호출
    LaunchedEffect(Unit) {
        viewModel.initData()
    }

    BackHandler {
        onBackClick()
    }

    LaunchedEffect(Unit) {
        viewModel.courseSaveSuccess.collect {
            android.widget.Toast.makeText(
                context,
                "코스가 내 리스트에 추가되었습니다.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false), // 커스텀 너비 사용 가능하게 🔹
            modifier = Modifier
                .fillMaxWidth(0.85f) // 화면의 85% 너비 사용
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E1E)) // 조금 더 깊은 다크톤
                .padding(24.dp),
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // ── [1] 타이틀 ──
                    Text(
                        text = "프로필 이름 수정",
                        color = WhiteTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "RUNUP에서 사용할 이름을 입력해 주세요.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── [2] 세련된 커스텀 입력창 ── 🔹
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { if (it.length <= 10) newName = it }, // 글자수 제한 팁 🔹
                        singleLine = true,
                        placeholder = { Text("새 이름 입력", color = Color.DarkGray, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = WhiteTextColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PointColor, // 포커스 시 포인트 컬러 🔹
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color(0xFF252525),
                            unfocusedContainerColor = Color(0xFF252525),
                            cursorColor = PointColor
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newName.isNotBlank()) {
                                viewModel.updateName(newName)
                                showEditDialog = false
                                newName = ""
                            }
                        })
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── [3] 버튼 영역 ── 🔹
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 취소 버튼
                        TextButton(
                            onClick = { showEditDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                        ) {
                            Text("취소", fontWeight = FontWeight.Medium)
                        }

                        // 확인 버튼 (포인트 컬러 강조)
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    viewModel.updateName(newName)
                                    showEditDialog = false
                                    // 🔹 성공 스낵바 띄우기
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "성공적으로 수정되었습니다.",
                                            duration = SnackbarDuration.Short // 짧게 보여주고 사라짐
                                        )
                                    }
                                    newName = ""
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PointColor),
                            enabled = newName.isNotBlank()
                        ) {
                            Text("확인", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = BackGroudColor,
        topBar = { TopBar(text = "마이페이지", isMenu = false, onBackClick = {onBackClick()}) },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF333333), // 다크한 배경
                    contentColor = Color.White,         // 글자 색상
                    shape = RoundedCornerShape(10.dp)   // 둥근 모서리
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 프로필
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp) // 위아래 여백을 줄여 전체적으로 끌어올림 🔹
                ) {
                    // [상단] 프로필 사진 + 유저 정보
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 좌측 프로필 이미지
                        Box(
                            modifier = Modifier
                                .size(72.dp) // 약간 줄여서 더 컴팩트하게 🔹
                                .clip(CircleShape)
                                .background(Color(0xFF2C2C2C))
                                .clickable { profileGalleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (myBitmap != null) {
                                Image(
                                    bitmap = myBitmap.asImageBitmap(),
                                    contentDescription = "Profile Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (myProfileUrl.isNotEmpty()) {
                                SubcomposeAsyncImage(
                                    model = myProfileUrl,
                                    contentDescription = "Profile Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PointColor, strokeWidth = 2.dp)
                                        }
                                    }
                                )
                            } else {
                                Text("👤", fontSize = 36.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 우측 이름 및 이메일
                        Column(
                            modifier = Modifier.clickable {
                                newName = userData?.userName ?: ""
                                showEditDialog = true
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userData?.userName ?: "Runner",
                                    color = WhiteTextColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = userData?.userEmail ?: "",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // 사진 영역과 버튼 사이 간격 🔹

                    // [하단] 액션 버튼 (두 버튼을 가로로 균등 배치) 🔹
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileCompactButton(
                            text = "기록 보기",
                            icon = Icons.Default.Description,
                            modifier = Modifier.weight(1f), // 버튼이 가로를 꽉 채우도록 🔹
                            onClick = {
                                val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                if (myUid.isNotEmpty()) onPostClick(myUid)
                            }
                        )
                        ProfileCompactButton(
                            text = "친구 목록",
                            icon = Icons.Default.People,
                            modifier = Modifier.weight(1f), // 버튼이 가로를 꽉 채우도록 🔹
                            onClick = { showFriendDialog = true }
                        )
                    }
                }
            }

            // 2. 최근 목표 영역
            item {
                Column {
                    Text("최근 목표", color = WhiteTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            GoalItem("목표 거리", "${(userData?.goalDistance ?: 0) / 1000f}km")
                            GoalItem("목표 페이스", formatSeconds(userData?.goalTime ?: 0))
                        }
                    }
                }
            }

            // 전체 통계 영역 (달린 횟수, 총 거리)
            item {
                Column {
                    Text("전체 통계", color = WhiteTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // 총 달린 횟수
                            GoalItem("달린 횟수", "${userData?.totalRunningCount ?: 0}회")
                            // 총 달린 거리:
                            val totalDistanceMeters = userData?.totalRunningDistance ?: 0L
                            GoalItem("총 거리", DistanceMapper.formatDistance(totalDistanceMeters.toDouble()))
                        }
                    }
                }
            }

            // 나의 러닝 섹션 헤더 + 필터 칩
            item {
                Column {
                    Text("나의 러닝", color = WhiteTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RunFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = runState.selectedFilter  == filter,
                                onClick = { viewModel.updateFilter(filter) },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = Color.Gray,
                                    selectedLabelColor = Color.White,
                                    selectedContainerColor = PointColor
                                )
                            )
                        }
                    }
                }
            }

            if (userData == null && runState.isLoadingMore) {
                // 초기 로딩
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PointColor)
                    }
                }
            } else if (runState.pagedRuns.isEmpty() && !runState.isLoadingMore) {
                // 데이터 없음
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("해당 기간의 러닝 기록이 없습니다.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                // 리스트 표시
                itemsIndexed(
                    runState.pagedRuns,
                    key = { _, run -> run.recordDate },
                    contentType = { _, _ -> "run_record_item" }
                ) { index, run ->
                    ExpandableRunItem(
                        index = index + runListStartIndex,          // 🔹 인덱스 추가
                        scrollState = scrollState, // 🔹 스크롤 상태 추가
                        run = run,
                        showQuickActions = activeActionItemId == run.recordDate,
                        onToggleActions = {
                            // 롱클릭 시: 현재 이거면 끄고, 아니면 이걸로 교체
                            activeActionItemId = if (activeActionItemId == run.recordDate) null else run.recordDate
                        },
                        onExpandClick = {
                            // 그냥 클릭 시: 메뉴가 떠 있으면 메뉴만 끄고, 아니면 확장 토글
                            if (activeActionItemId != null) {
                                activeActionItemId = null
                            } else {
                                // 이 내부 로직은 아래 컴포저블 안에서 처리하도록 위임
                            }
                        },
                        onDeleteConfirm = { viewModel.deleteRun(it) },
                        onUploadClick = { run: RunRecord ->
                            communityViewModel.selectRunRecordFromMyPage(run)
                            onUploadClick()
                        },
                        onAddCourseClick = { record ->
                            viewModel.addCourseFromRecord(record)
                        }
                    )
                }

                item(key = "list_footer") { // 👈 고정 키를 주면 리스트가 위치를 정확히 기억해!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(), // 👈 내용 변경 시 높이 변화를 부드럽게!
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (runState.hasMore) {
                            // [1] 더 보기 버튼 모드
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .offset(y = (-10).dp), // 이전 아이템과 살짝 밀착
                                contentAlignment = Alignment.Center
                            ) {
                                if (runState.isLoadingMore) {
                                    CircularProgressIndicator(color = PointColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = "더 보기 ▾",
                                        color = PointColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { viewModel.loadMoreRuns() }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        } else {
                            // [2] 모든 기록 로드 완료 모드 (8dp 여백만 남김)
                            // spacedBy(20dp)가 이미 있으므로, 여기 Spacer 높이를 조절해서 간격을 맞춤
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        if (showFriendDialog) {
            FriendListDialog(
                onDismiss = { showFriendDialog = false }, // X 버튼이나 배경 클릭 시 닫기
                onPostClick = { uid ->
                    onPostClick(uid) // MyPageScreen이 이미 가지고 있는 함수 전달
                }
            )
        }
    }
}

// ── 💡 버튼 컴포저블 (Modifier 인자 추가) ──
@Composable
fun ProfileCompactButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier.height(40.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PointColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = WhiteTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExpandableRunItem(
    index: Int,                // 🔹 추가
    scrollState: LazyListState, // 🔹 추가
    run: RunRecord,
    showQuickActions: Boolean,    // 🔹 외부에서 주입
    onToggleActions: () -> Unit, // 🔹 롱클릭 콜백
    onExpandClick: () -> Unit,   // 🔹 클릭 시 부모 상태 체크용 콜백
    onDeleteConfirm: (String) -> Unit,
    onUploadClick: (RunRecord) -> Unit,
    onAddCourseClick: (RunRecord) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) } // 삭제 다일로그


    LaunchedEffect(expanded) {
        if (expanded) {
            // 1. 애니메이션이 어느 정도 진행될 때까지 대기
            delay(150)

            // 2. 현재 내 아이템의 레이아웃 정보 가져오기
            val itemInfo = scrollState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

            if (itemInfo != null) {
                // 3. 현재 내 아이템의 하단 위치 (offset + size)
                val itemBottom = itemInfo.offset + itemInfo.size

                // 4. 하단 네비게이션 바 등을 고려한 실제 뷰포트 높이
                val viewportBottom = scrollState.layoutInfo.viewportEndOffset

                // 🔹 내 바닥이 화면 끝보다 아래에 있다면?
                if (itemBottom > viewportBottom) {
                    val scrollDelta = itemBottom - viewportBottom
                    // 딱 잘린 만큼만 + 여유분(50px) 스크롤
                    scrollState.animateScrollBy(scrollDelta.toFloat() + 125f)
                }
            }
        }
    }

    // 1. 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("기록 삭제") },
            text = { Text("이 러닝 기록을 정말로 삭제하시겠습니까?\n총 거리 통계에서도 차감됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConfirm(run.course.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            },
            containerColor = Color(0xFF2C2C2C),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        onExpandClick()
                        if (!showQuickActions) {
                            expanded = !expanded
                        }
                    },
                    onLongClick = {onToggleActions() } // 👈 꾹 누르면 퀵 버튼 등장
                )
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            TimeMapper.formatTimestamp(run.recordDate),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            run.course.id,
                            color = WhiteTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${run.course.distance}m",
                            color = PointColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(formatDuration(run.time), color = Color.LightGray, fontSize = 12.sp)
                    }
                }

                // --- [수정] 펼쳐졌을 때 나타나는 상세 영역 ---
                if (expanded) {
                    var isMapLoaded by remember { mutableStateOf(false) }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. 코스 지도 표시 영역
                    val centerLat = (run.course.minLat + run.course.maxLat) / 2
                    val centerLng = (run.course.minLng + run.course.maxLng) / 2

                    // 코스 크기에 따른 동적 줌 (CommunityScreen 로직 재사용)
                    val dynamicZoom = remember(run.course) {
                        val latDiff = run.course.maxLat - run.course.minLat
                        val lngDiff = run.course.maxLng - run.course.minLng
                        val maxDiff = maxOf(latDiff, lngDiff)
                        when {
                            maxDiff > 0.04 -> 13
                            maxDiff > 0.015 -> 14
                            maxDiff > 0.005 -> 15
                            else -> 16
                        }
                    }

                    val staticMapUrl = remember(run.recordDate) {
                        buildNaverStaticMapUrl(centerLat, centerLng, dynamicZoom)
                    }


                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)) // 지도 모서리도 둥글게 하면 세련돼 보입니다
                            .background(Color(0xFF2C2C2C))  // 지도 로드 전 배경색 (스켈레톤 느낌)
                    ) {

                        // 1. Static Map
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(staticMapUrl)
                                .crossfade(true) // 부드러운 전환 효과
                                .addHeader("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_API_KEY)
                                .addHeader("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_API_SECRET_KEY)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onSuccess = { isMapLoaded = true }
                        )

                        if (isMapLoaded) {
                            // 2. 경로 그리기 (시작/종료 마커 포함)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // 🔹 핵심: 모든 드로잉 로직은 이 Canvas { ... } 블록 안에 있어야 합니다! 📍
                                val points = run.course.locationPoints.map {
                                    val (x, y) = latLngToPixel(
                                        it.locationPoint.latitude,
                                        it.locationPoint.longitude,
                                        centerLat,
                                        centerLng,
                                        dynamicZoom.toDouble(),
                                        size.width,
                                        size.height
                                    )
                                    Offset(x, y)
                                }

                                if (points.isNotEmpty()) {
                                    val path = Path().apply {
                                        points.forEachIndexed { i, p ->
                                            if (i == 0) moveTo(p.x, p.y)
                                            else {
                                                if (!run.course.locationPoints[i - 1].stop) lineTo(
                                                    p.x,
                                                    p.y
                                                )
                                                else moveTo(p.x, p.y)
                                            }
                                        }
                                    }

                                    // 경로 선 그리기
                                    drawPath(
                                        path,
                                        Color.Black,
                                        style = Stroke(
                                            14f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                    drawPath(
                                        path,
                                        PointColor,
                                        style = Stroke(
                                            8f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )

                                    // 시작/종료 마커
                                    drawMarker(points.first(), Color(0xFF4CAF50), "START")
                                    drawMarker(points.last(), Color(0xFFF44336), "END")
                                }
                            }
                            // 🔹 3. [추가] 지도 좌측 상단 점수 정보 패널
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart) // 좌측 상단 정렬
                                    .padding(10.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.6f),
                                        RoundedCornerShape(8.dp)
                                    ) // 반투명 검정 배경
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ScoreIndicator(label = "밝기", score = run.course.scores.brightScore)
                                ScoreIndicator(label = "붐빔", score = run.course.scores.crowdedScore)
                                ScoreIndicator(label = "난이도", score = run.course.scores.hardScore)
                            }
                        } else {
                            // 🔹 로딩 중일 때 보여줄 인디케이터 (선택 사항)
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center).size(24.dp),
                                color = PointColor,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. 상세 지표 영역 (평균 페이스 등)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailMetricItem(
                            "평균 페이스",
                            calculatePace(run.time, run.course.distance.toDouble())
                        )
                        DetailMetricItem(
                            "평균 속도",
                            String.format(
                                "%.1f km/h",
                                (run.course.distance / 1000.0) / (run.time / 3600000.0)
                            )
                        )
                    }
                }
            }
        }

        // ── 🔹 [핵심] 우측 하단 퀵 액션 버튼 영역 📍 ──
        androidx.compose.animation.AnimatedVisibility(
            visible = showQuickActions,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 12.dp) // 카드 내부 위치 조정
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 업로드 버튼
                QuickActionButton(Icons.Default.CloudUpload, PointColor) {
                    onUploadClick(run)
                    onToggleActions() // 작업 후 닫기
                }
                // 2. 코스 추가 버튼
                QuickActionButton(Icons.Default.AddLocation, PointColor) {
                    onAddCourseClick(run)
                    onToggleActions() // 작업 후 닫기
                }
                // 3. 삭제 버튼
                QuickActionButton(Icons.Default.Delete, Color.Red) {
                    showDeleteDialog = true
                    onToggleActions() // 작업 후 닫기
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, color: Color, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}

fun buildNaverStaticMapUrl(
    centerLat: Double,
    centerLng: Double,
    zoom: Int
): String {
    return "https://maps.apigw.ntruss.com/map-static/v2/raster" +
            "?w=600&h=400" +
            "&center=$centerLng,$centerLat" + // ⚠️ 순서 중요 (경도,위도)
            "&level=$zoom" +
            "&maptype=basic" +
            "&format=png" +
            "&scale=2"
}



