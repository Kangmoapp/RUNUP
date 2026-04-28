package com.example.runup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.data.cityList
import com.example.runup.domain.model.AddressModel
import com.example.runup.domain.model.AdmVO
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.WhiteTextColor
import kotlin.text.ifEmpty

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomLocationPicker(
    currentAddress: AddressModel?,
    districtList: List<AdmVO>, // 🔹 데이터 직접 받기
    dongList: List<AdmVO>,     // 🔹 데이터 직접 받기
    isLoadingDistrict: Boolean,
    isLoadingDong: Boolean,
    onLoadDistricts: (String, String) -> Unit, // 🔹 함수 직접 받기
    onLoadDongs: (String, String, String) -> Unit,
    onApply: (String, String, String) -> Unit,
    onBack: () -> Unit
) {

    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var dong by remember { mutableStateOf("") }
    var currentParentCode by remember { mutableStateOf("") }

    // 🔹 현재 보여줄 탭 단계 (0=시도, 1=구군, 2=동)
    var activeStep by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .width(300.dp)
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🔹 뒤로가기 버튼
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("<", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(Modifier.width(10.dp))

            Text(
                "지역 선택",
                color = WhiteTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(Modifier.weight(1f))

            // 🔹 내 위치 버튼
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PointColor.copy(alpha = 0.1f))
                    .clickable {
                        city = currentAddress?.city ?: ""
                        district = currentAddress?.district ?: ""
                        dong = currentAddress?.dong ?: ""
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("📍", fontSize = 9.sp)
                Text(
                    "내 위치",
                    color = PointColor,
                    fontSize = 11.sp
                )
            }
        }

        // ── 탭 헤더 3개 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // 시/도 탭
            LocationTab(
                label = city.ifEmpty { "시/도" },
                isActive = activeStep == 0,
                isSelected = city.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { activeStep = 0 }

            // 구/군 탭
            LocationTab(
                label = district.ifEmpty { "구/군" },
                isActive = activeStep == 1,
                isSelected = district.isNotEmpty(),
                enabled = city.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { if (city.isNotEmpty()) activeStep = 1 }

            // 동/읍/면 탭
            LocationTab(
                label = dong.ifEmpty { "동/읍/면" },
                isActive = activeStep == 2,
                isSelected = dong.isNotEmpty(),
                enabled = district.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { if (district.isNotEmpty()) activeStep = 2 }
        }

        // 탭 아래 구분선
        Divider(
            color = Color.White.copy(alpha = 0.08f),
            thickness = 1.dp
        )

        // ── 리스트 본문 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)  // 고정 높이
        ) {
            when (activeStep) {

                // 0단계: 시/도 목록
                0 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(cityList) { (name, code) ->
                            LocationRowItem(
                                label = name,
                                isSelected = city == name
                            ) {
                                city = name
                                currentParentCode = code
                                district = ""
                                dong = ""
                                onLoadDistricts(code, name)
                                activeStep = 1  // 자동으로 다음 탭
                            }
                        }
                    }
                }

                // 1단계: 구/군 목록
                1 -> {
                    if (isLoadingDistrict) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = PointColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(districtList) { vo ->
                                LocationRowItem(
                                    label = vo.lowestAdmName,
                                    isSelected = district == vo.lowestAdmName
                                ) {
                                    district = vo.lowestAdmName
                                    currentParentCode = vo.admCode
                                    dong = ""
                                    onLoadDongs(vo.admCode, city, vo.lowestAdmName)
                                    activeStep = 2  // 자동으로 다음 탭
                                }
                            }
                        }
                    }
                }

                // 2단계: 동/읍/면 목록
                2 -> {
                    if (isLoadingDong) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = PointColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // 🔹 구/군 전체 옵션
                            item {
                                LocationRowItem(
                                    label = "전체",
                                    isSelected = dong == "전체"
                                ) {
                                    dong = "전체"
                                }
                            }
                            items(dongList) { vo ->
                                LocationRowItem(
                                    label = vo.lowestAdmName,
                                    isSelected = dong == vo.lowestAdmName
                                ) {
                                    dong = vo.lowestAdmName
                                }
                            }
                        }
                    }
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // ── 선택된 지역 태그 (기존 유지) ──
        if (city.isNotEmpty() || district.isNotEmpty() || dong.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (city.isNotEmpty()) LocationTag(city) {
                    city = ""; district = ""; dong = ""; activeStep = 0
                }
                if (district.isNotEmpty()) LocationTag(district) {
                    district = ""; dong = ""; activeStep = 1
                }
                if (dong.isNotEmpty()) LocationTag(dong) {
                    dong = ""
                }
            }
        }

        // ── 하단 버튼 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("뒤로", color = Color.Gray, fontSize = 13.sp)
            }
            Button(
                onClick = { onApply(city, district, dong) },
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(containerColor = PointColor),
                shape = RoundedCornerShape(8.dp),
                enabled = city.isNotEmpty()
            ) {
                Text("적용", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ── 탭 컴포넌트 ──
@Composable
fun LocationTab(
    label: String,
    isActive: Boolean,
    isSelected: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val textColor = when {
        isActive -> PointColor
        isSelected -> WhiteTextColor
        !enabled -> Color(0xFF555555)
        else -> Color.Gray
    }

    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        // 활성 탭 밑줄
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isActive) PointColor else Color.Transparent)
        )
    }
}

// ── 리스트 아이템 컴포넌트 ──
@Composable
fun LocationRowItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) PointColor.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isSelected) PointColor else WhiteTextColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PointColor)
            )
        }
    }
}

@Composable
fun LocationTag(text: String, onDelete: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, color = WhiteTextColor, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Add, // 'x' 아이콘이 적절하나 없으면 Add를 45도 돌려서 사용 가능
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer(rotationZ = 45f)
                    .clickable { onDelete() }
            )
        }
    }
}