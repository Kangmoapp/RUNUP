package com.example.runup.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.domain.model.AuthResult
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleteResult) {
        if (uiState.deleteResult is AuthResult.Success) {
            // 별도 파일(예: AppUtils.kt)에 빼둔 restartApp을 호출
            restartApp(context)
        }
    }

    // 로그아웃 확인 다이얼로그
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃", color = Color.White) },
            text = { Text("정말 로그아웃 하시겠어요?", color = Color.Gray, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogoutClick()
                }) { Text("로그아웃", color = Color(0xFF4A90E2)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }

    // 구글 전용 탈퇴 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("회원 탈퇴", color = Color.White) },
            text = {
                Text(
                    text = "탈퇴 시 모든 러닝 기록, 커뮤니티 활동 및 개인 정보가 영구히 삭제됩니다. 정말 탈퇴하시겠어요?",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAccount() // 👈 비번 없이 호출
                }) { Text("탈퇴하기", color = Color(0xFFE57373)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소", color = Color.Gray) }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }

    Scaffold(
        containerColor = BackGroudColor,
        topBar = { TopBar(text = "설정", isMenu = false, onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 계정 섹션
            SettingsSectionHeader("계정")
            SettingsItem(
                title = "계정 정보",
                subtitle = "이름, 이메일 등 내 정보 확인",
                onClick = { /* TODO: 계정 정보 화면으로 이동 */ }
            )

            // 알림 섹션
            SettingsSectionHeader("알림")
            SettingsSwitchItem(
                title = "알림 받기",
                subtitle = "앱 푸시 알림을 켜거나 끕니다",
                checked = uiState.notificationEnabled,
                // ── 🔹 직접 할당 대신 ViewModel의 함수 호출 📍 ──
                onCheckedChange = { viewModel.toggleNotification(it) }
            )

            // 앱 정보 섹션
            SettingsSectionHeader("앱 정보")
            SettingsItem(
                title = "개인정보처리방침",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://your-domain.com/privacy"))
                    context.startActivity(intent)
                }
            )
            SettingsItem(
                title = "이용약관",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://your-domain.com/terms"))
                    context.startActivity(intent)
                }
            )
            SettingsVersionItem()

            Spacer(modifier = Modifier.height(32.dp))

            // 로그아웃
            SettingsSectionHeader("계정 관리")
            SettingsItem(
                title = "로그아웃",
                titleColor = Color(0xFFE57373),
                onClick = { showLogoutDialog = true }
            )
            SettingsItem(
                title = "회원 탈퇴",
                titleColor = Color(0xFFE57373),
                subtitle = "앱 내 모든 기록 및 활동 삭제",
                onClick = { showDeleteDialog = true }
            )
        }
    }
    // ── 🔹 [추가] 로딩 화면 오버레이 📍 ──
    if (uiState.isLoading) {
        // 배경을 반투명하게 덮어서 클릭을 방지합니다.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFF4A90E2),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "회원 탈퇴 진행 중...",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF4A90E2),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 18.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    titleColor: Color = WhiteTextColor,
    onClick: () -> Unit
) {
    Surface(color = Color.Transparent) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = titleColor, fontSize = 16.sp)
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Divider(
                color = Color(0xFF2C2C2C),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(color = Color.Transparent) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = WhiteTextColor, fontSize = 16.sp)
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4A90E2)
                    )
                )
            }
            Divider(
                color = Color(0xFF2C2C2C),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun SettingsVersionItem() {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Surface(color = Color.Transparent) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "앱 버전", color = WhiteTextColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(text = versionName, color = Color.Gray, fontSize = 14.sp)
            }
            Divider(
                color = Color(0xFF2C2C2C),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

private fun restartApp(context: Context) {
    // 1. 앱의 런처 인텐트를 가져옵니다 (보통 MainActivity)
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    val componentName = intent?.component

    // 2. 모든 액티비티 스택을 날리고 새로 시작하는 인텐트 생성
    val mainIntent = Intent.makeRestartActivityTask(componentName)

    // 3. 앱 재시작 실행
    context.startActivity(mainIntent)

    // 4. 현재 실행 중인 프로세스를 완전히 종료 (이게 핵심! 🚨)
    // 0은 정상 종료를 의미합니다.
    Runtime.getRuntime().exit(0)
}