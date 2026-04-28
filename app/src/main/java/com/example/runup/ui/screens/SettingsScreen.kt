package com.example.runup.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var notificationEnabled by remember { mutableStateOf(true) }

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
                checked = notificationEnabled,
                onCheckedChange = { notificationEnabled = it }
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
