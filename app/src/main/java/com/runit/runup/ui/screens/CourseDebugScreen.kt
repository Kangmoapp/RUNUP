package com.runit.runup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.runit.runup.data.source.local.objectbox.entity.CourseEntity
import com.runit.runup.viewmodel.CourseDebugViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDebugScreen(
    onBackClick: () -> Unit,
    viewModel: CourseDebugViewModel = hiltViewModel()
) {
    val courseList by viewModel.courseList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RUNUP DB Inspector") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "총 ${courseList.size}건의 데이터가 로드되었습니다.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(items = courseList) {entity ->
                CourseDebugCard(entity)
            }
        }
    }
}

@Composable
fun CourseDebugCard(entity: CourseEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. 헤더: ID 및 주소
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🆔 ${entity.firebaseId?.takeLast(8)}...",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                // 거리 정보 배지 (미터 단위를 킬로미터로 보기 좋게 표시)
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${String.format("%.2f", entity.distance / 1000.0)} km",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "📍 ${entity.address ?: "주소 정보 없음"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2. 좌표 범위 정보 (Bounding Box)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🌐 Bounding Box (좌표 범위)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoTag(label = "Lat", value = "${entity.minLat.format(3)} ~ ${entity.maxLat.format(3)}")
                    InfoTag(label = "Lng", value = "${entity.minLng.format(3)} ~ ${entity.maxLng.format(3)}")
                }
            }

            // 3. AI 관련 정보 (임베딩 및 벡터)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.extraSmall
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🤖 AI Metadata",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "📝 Text: ${entity.embeddingText?.take(60) ?: "N/A"}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                val vectorSize = entity.vector?.size ?: 0
                Text(
                    text = "📊 Vector: ${vectorSize}d | [${entity.vector?.take(3)?.joinToString(", ")}...]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

// 간단한 도우미 함수들
@Composable
fun InfoTag(label: String, value: String) {
    Row {
        Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)