package com.example.runup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.util.mapper.DistanceMapper
import com.example.runup.viewmodel.FriendViewModel

@Composable
fun ProfileMiniPopup(
    userId: String,
    onDismiss: () -> Unit,
    onViewPosts: (String) -> Unit,
    viewModel: FriendViewModel = hiltViewModel()
) {
    val targetUser by viewModel.targetUserProfile.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(userId) {
        viewModel.fetchTargetUserProfile(userId)
    }

    // 🔹 1. [핵심 수정] AlertDialog 대신 Dialog + Surface를 사용하여 패딩을 정밀 제어합니다.
    Dialog(
        onDismissRequest = {
            viewModel.clearTargetUserProfile()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(), // 🔹 높이는 컨텐츠에 맞게
            color = Color(0xFF1A1A1A), // 짙은 다크 모드 배경
            shape = RoundedCornerShape(20.dp), // 둥근 모서리
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) // 미세한 테두리
        ) {
            // 🔹 2. 컨텐츠를 감싸는 Box에 정밀한 내부 패딩 설정
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp
                    )
            ) {
                if (targetUser == null) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PointColor, strokeWidth = 2.dp)
                    }
                } else {
                    val user = targetUser!!

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // ── [상단] 프로필 정보 + 액션 아이콘 ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (user.userProfileUrl.isNullOrEmpty()) {
                                // ── 🔹 URL이 없을 때 보여줄 기본 아이콘 👤 📍 ──
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2C2C2C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "기본 프로필",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                // ── 🔹 URL이 있을 때 기존 이미지 로드 📍 ──
                                AsyncImage(
                                    model = user.userProfileUrl,
                                    contentDescription = "프로필 이미지",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2C2C2C)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            // 이름 및 이메일
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.userName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(user.userEmail, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                            }

                            // 우측 액션 아이콘 모음 (POSTS, ADD)
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                IconButton(
                                    onClick = { onViewPosts(userId) },
                                    modifier = Modifier.size(36.dp).background(Color.White.copy(0.05f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Description, null, tint = PointColor, modifier = Modifier.size(18.dp))
                                }

                                // 🔹 3. [핵심 수정] 포스트 아이콘을 왼쪽으로 옮기기 위해 간격을 벌립니다. (8dp -> 16dp)
                                Spacer(Modifier.width(16.dp))

                                // 1. 현재 로그인한 내 UID 가져오기
                                val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                val isAlreadyFriend = uiState.friends.any { it.userId == userId }
                                val isAlreadySent = uiState.sentRequests.any { it.userId == userId }
                                val isMe = userId == myUid

                                val isActionDisabled = isAlreadyFriend || isAlreadySent || isMe

                                IconButton(
                                    onClick = {
                                        if (!isActionDisabled) {
                                            viewModel.sendRequest(userId)
                                        }
                                    },
                                    enabled = !isActionDisabled,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .alpha(if (isActionDisabled) 0.3f else 1f)
                                        .background(Color.White.copy(0.05f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isAlreadyFriend) Icons.Default.Check else Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = if (isAlreadyFriend) PointColor else if (isActionDisabled) Color.Gray else PointColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Divider(Modifier.padding(vertical = 16.dp), color = Color.White.copy(0.05f))

                        // ── [하단] 4대 통계 (가로 배치 최적화) ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MiniStatItem("목표 거리", "${user.goalDistance / 1000f}km")
                            MiniStatItem("목표 페이스", "${user.goalTime / 60}'${user.goalTime % 60}\"")
                            MiniStatItem("달린 횟수", "${user.totalRunningCount}회")
                            MiniStatItem("총 거리", DistanceMapper.formatDistance(user.totalRunningDistance.toDouble()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp)) // 라벨과 값 사이 미세 간격
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}