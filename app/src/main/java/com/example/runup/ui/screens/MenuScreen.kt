package com.example.runup.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.PointColor
import com.example.runup.viewmodel.MenuViewModel

@Preview
@Composable
fun PreviewMenuScreen(){
    MenuScreen({},{},{},{},{}, {}, {})
}

@Composable
fun MenuScreen(
    onBackClick: () -> Unit,
    onOptionClick: () -> Unit,
    onHelpClick: () -> Unit,
    onCommunityClick: () -> Unit,
    onMypageClick: () -> Unit,
    onLocalDBClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: MenuViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // 데이터 프리로드는 백그라운드에서 계속 진행 (마이페이지 등을 위해 유지)
    LaunchedEffect(Unit) {
        viewModel.initPreload(context)
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        containerColor = BackGroudColor,
        topBar = { TopBar(onBackClick = onBackClick, isMenu = false) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()) // 메뉴가 많아질 경우를 대비
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. 주요 활동 및 커뮤니티 섹션
            MenuSectionTitle("나의 러닝")

            // 핵심 메뉴는 PointColor 아이콘으로 강조
            MainMenuItem(text = "커뮤니티", icon = Icons.Default.People, onClick = onCommunityClick)
            MainMenuItem(text = "마이페이지", icon = Icons.Default.Person, onClick = onMypageClick)

            Spacer(modifier = Modifier.height(32.dp))

            // 2. 관리 및 설정 섹션
            MenuSectionTitle("앱 관리")

            // 보조 메뉴는 흰색 아이콘으로 깔끔하게
            SubMenuItem(text = "설정", icon = Icons.Default.Settings, onClick = onOptionClick)
            SubMenuItem(text = "도움말", icon = Icons.Default.HelpOutline, onClick = onHelpClick)
            SubMenuItem(text = "로컬 데이터", icon = Icons.Default.Storage, onClick = onLocalDBClick)

            Spacer(modifier = Modifier.weight(1f)) // 로그아웃을 하단으로 밀어냄

            // 3. 로그아웃 (별도 버튼 디자인)
            LogoutButton {
                viewModel.signOutWithGoogle(context) { onLogoutClick() }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── 세부 컴포넌트들 ──

@Composable
private fun MenuSectionTitle(title: String) {
    Text(
        text = title,
        color = Color.Gray, // 섹션 타이틀은 연한 회색으로
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
    )
}

// 핵심 메뉴 (PointColor 리플 효과 적용)
@Composable
private fun MainMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    // 🔹 클릭 인터랙션을 감지하기 위한 소스
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(
                interactionSource = interactionSource,
                // 🔹 여기서 리플 색상을 PointColor로 지정!
                indication = ripple(color = PointColor),
                onClick = onClick
            ),
        color = Color(0xFF2C2C2C),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PointColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

// 보조 메뉴 (PointColor 리플 효과 적용)
@Composable
private fun SubMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = interactionSource,
                // 🔹 보조 메뉴도 클릭 시 PointColor로 번지게 설정
                indication = ripple(color = PointColor),
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF444444), modifier = Modifier.size(18.dp))
    }
}

// 로그아웃 버튼 (빨간색 리플 효과 적용)
@Composable
private fun LogoutButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                // 🔹 로그아웃은 경고 의미로 빨간색 리플을 줍니다.
                indication = ripple(color = Color(0xFFFF5252)),
                onClick = onClick
            ),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "로그아웃", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}