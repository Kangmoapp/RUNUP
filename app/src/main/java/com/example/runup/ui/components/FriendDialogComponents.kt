package com.example.runup.ui.components

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.runup.domain.model.FriendSummary
import com.example.runup.domain.model.UserData
import com.example.runup.ui.theme.PointColor
import com.example.runup.viewmodel.FriendUiState
import com.example.runup.viewmodel.FriendViewModel


@Composable
fun FriendListDialog(
    onDismiss: () -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: FriendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val myUid = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
    var selectedTab by remember { mutableIntStateOf(0) }

    // 선택된 친구 id
    var selectedProfileId by remember { mutableStateOf<String?>(null) }
    val profileBitmaps by viewModel.profileBitmaps.collectAsState()

    // ── 🔹 [해결 3] 탭 전환 시 일시적 상태 초기화 ── 📍
    LaunchedEffect(selectedTab) {
        viewModel.resetTransientStates() // 검색 결과 및 삭제 모드 초기화
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable {
                viewModel.resetTransientStates()
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A)) // 더 깊은 블랙 계열로 변경
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .clickable(enabled = false) { }
        ) {
            // ── 상단 헤더 (이모티콘 제거, 폰트 정제) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "친구",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            viewModel.resetTransientStates() // 초기화 후 닫기
                            onDismiss()
                        }
                )
            }

            // ── 탭 메뉴 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                FriendTabItem("친구 목록", isSelected = selectedTab == 0, modifier = Modifier.weight(1f)) { selectedTab = 0 }
                FriendTabItem("검색", isSelected = selectedTab == 1, modifier = Modifier.weight(1f)) { selectedTab = 1 }
            }

            // ── 중앙 컨텐츠 ──
            Box(modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 10.dp)) {
                if (selectedTab == 0) {
                    FriendListTab(
                        uiState,
                        profileBitmaps,
                        onManageClick = { viewModel.toggleManagementDialog(true) },
                        onFriendClick = { id -> selectedProfileId = id },
                        viewModel)
                } else {
                    FriendSearchTab(
                        uiState,
                        profileBitmaps,
                        myUid = myUid,
                        onSearch = { viewModel.searchUser(it) },
                        onAdd = { viewModel.sendRequest(it) } ,
                        onUserClick = { id -> selectedProfileId = id }
                    )

                }
            }
        }
    }

    // 🔹 7. [팝업 배치] 상태값이 null이 아니면 팝업을 화면 맨 위에 띄움
    if (selectedProfileId != null) {
        ProfileMiniPopup(
            userId = selectedProfileId!!,
            onDismiss = { selectedProfileId = null },
            onViewPosts = { uid ->
                onPostClick(uid)
                onDismiss() // 활동 보러 가면서 친구 목록 창도 닫아주는 게 자연스럽습니다.}
            }
        )
    }

    if (uiState.showManagementDialog) {
        FriendManagementDialog(
            onDismiss = { viewModel.toggleManagementDialog(false) },
            viewModel = viewModel,
            onUserClick = { id -> selectedProfileId = id }
        )
    }
}

@Composable
fun FriendSearchTab(
    uiState: FriendUiState,
    profileBitmaps: Map<String, Bitmap>,
    myUid: String?,
    onSearch: (String) -> Unit,
    onAdd: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("이메일로 검색하세요", color = Color.DarkGray, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { onSearch(searchText) }) {
                    Icon(Icons.Default.Search, null, tint = PointColor)
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedIndicatorColor = PointColor.copy(alpha = 0.5f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "검색 결과",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.05f))

        uiState.searchResult?.let { user ->
            val isMe = user.userId == myUid
            val isAlreadyFriend = uiState.friends.any { it.userId == user.userId }
            val isAlreadySent = uiState.sentRequests.any { it.userId == user.userId }

            val isActionDisabled = isAlreadyFriend || isAlreadySent || isMe

            FriendItem(
                user = user,
                profileBitmaps,
                onClick = { onUserClick(user.userId) },
                trailingContent = {
                    IconButton(
                        onClick = {
                            if (!isActionDisabled) {
                                onAdd(user.userId)
                                Toast.makeText(context, "친구 요청을 보냈습니다", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isActionDisabled,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .alpha(if (isActionDisabled) 0.2f else 1f) // 🔹 신청/친구 상태일 때 흐리게 처리
                            .background(if (isActionDisabled) Color.Transparent else Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = if (isAlreadyFriend) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = if (isAlreadyFriend) PointColor else if (isActionDisabled) Color.Gray else PointColor
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun FriendManagementDialog(
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: FriendViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var managementTab by remember { mutableIntStateOf(0) }

    val profileBitmaps by viewModel.profileBitmaps.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).zIndex(10f).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E1E))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .clickable(enabled = false) { }
        ) {
            Text(
                "친구 신청",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(20.dp),
                letterSpacing = 1.sp
            )

            ManagementTabs(selectedTab = managementTab, onTabSelected = { managementTab = it })

            val list = if (managementTab == 0) uiState.receivedRequests else uiState.sentRequests

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                if (list.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("친구 요청이 없습니다.", color = Color.DarkGray, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(list) { user ->
                        FriendItem(user = user,profileBitmaps, onClick = { onUserClick(user.userId) }) {
                            if (managementTab == 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = { viewModel.declineRequest(user.userId) }) {
                                        Icon(Icons.Default.Close, null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { viewModel.acceptRequest(user.userId) }) {
                                        Icon(Icons.Default.Check, null, tint = PointColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            } else {
                                IconButton(onClick = { viewModel.declineRequest(user.userId) }) {
                                    Icon(Icons.Default.Close, null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("닫기", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FriendItem(
    user: FriendSummary,
    profileBitmaps: Map<String, Bitmap>,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit = {}
) {
    val bitmap = profileBitmaps[user.userProfileUrl]

    val itemModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp)
        .let { modifier ->
            if (onClick != null) modifier.clickable { onClick() }
            else modifier // null이면 클릭 기능을 아예 넣지 않음
        }
    Row(
        modifier = itemModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF252525))
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            if (bitmap != null) {
                // [1] 비트맵 창고에 사진이 있는 경우 (즉시 표시) 🔹
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (!user.userProfileUrl.isNullOrEmpty()) {
                // [2] 서버에서 로드해야 하는 경우 (로딩 바 표시) 🔹
                SubcomposeAsyncImage(
                    model = user.userProfileUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        // 친구 목록용 작은 로딩 바
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp), // 아이템 사이즈에 맞춰 작게 조절
                                color = PointColor,
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        // 로드 실패 시 기본 아이콘
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 24.sp)
                        }
                    }
                )
            } else {
                // [3] URL도 없고 비트맵도 없는 경우
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 24.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.userName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = user.userEmail,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        trailingContent()
    }
}

@Composable
fun FriendTabItem(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(30.dp) // 🔹 하단 바 길이를 조절해 더 세련되게 변경
                .background(if (isSelected) PointColor else Color.Transparent)
        )
    }
}

@Composable
fun FriendListTab(
    uiState: FriendUiState,
    profileBitmaps: Map<String, Bitmap>,
    onManageClick: () -> Unit,
    onFriendClick: (String) -> Unit,
    viewModel: FriendViewModel
) {
    // ── 🔹 삭제 확인을 위한 임시 상태 ──
    var friendToDelete by remember { mutableStateOf<FriendSummary?>(null) }

    // ── 🔹 삭제 확인 다이얼로그 ──
    if (friendToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { friendToDelete = null },
            containerColor = Color(0xFF1E1E1E), // 배경과 통일감 유지
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "친구를 삭제할까요?",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${friendToDelete?.userName}님이 친구 목록에서 삭제됩니다.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        friendToDelete?.let { viewModel.deleteFriend(it.userId) }
                        friendToDelete = null // 다이얼로그 닫기
                    }
                ) {
                    Text("삭제", color = Color(0xFFE57373), fontWeight = FontWeight.Bold) // 삭제는 레드 계열 유지
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { friendToDelete = null }
                ) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 친구 수 표시 (예: 30 / 30)
        Text(
            text = "친구 목록 ( ${uiState.friends.size} / 30 )",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 친구 리스트
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(uiState.friends) { friend ->
                val isDeleteMode = uiState.deletingFriendId == friend.userId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (isDeleteMode) {
                                    // 1. 삭제 모드일 때는 삭제 모드 해제
                                    viewModel.setDeletingFriend(null)
                                } else {
                                    // 2. [기능 유지] 일반 모드일 때 클릭하면 미니 프로필 띄우기 🔹
                                    onFriendClick(friend.userId)
                                }
                            },
                            onLongClick = {
                                // 꾹 누르면 해당 친구의 삭제 버튼 활성화
                                viewModel.setDeletingFriend(friend.userId)
                            }
                        )
                ) {
                    FriendItem(
                        user = friend,
                        profileBitmaps,
                        onClick = null,
                        trailingContent = {
                            if (isDeleteMode) {
                                // 🔹 배경 없이 담백하게 아이콘만 배치
                                IconButton(
                                    onClick = { friendToDelete = friend },
                                    modifier = Modifier.size(32.dp) // 배경(background) 제거 🔹
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "삭제 요청",
                                        tint = Color(0xFFE57373), // 👈 너무 튀지 않으면서 '경고'의 의미가 확실한 레드
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // 🔹 하단 친구 관리 버튼 (네 번째 사진으로 가는 통로)
        Button(
            onClick = onManageClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D3D3D))
        ) {
            Text("📝 친구 관리", color = Color.White)
        }
    }
}

@Composable
fun ManagementTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF323232))
    ) {
        val tabs = listOf("받은 신청", "보낸 신청")
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .height(2.dp)
                            .fillMaxWidth(0.5f)
                            .background(PointColor)
                    )
                }
            }
        }
    }
}