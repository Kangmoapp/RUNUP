package com.example.runup.ui.screens

import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.viewmodel.StartViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.util.TableInfo
import com.example.runup.R
import com.example.runup.ui.components.PageIndicator
import com.example.runup.ui.theme.DarkGray
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.theme.TextGray
import com.example.runup.ui.theme.White

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartScreen(
    onHomeClick: () -> Unit,
    viewModel: StartViewModel = hiltViewModel()
) {

    //위치 권한 받기
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    LaunchedEffect(Unit) {
        if (!locationPermissionState.allPermissionsGranted) {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    val context = LocalContext.current
    StartContent(
        onLoginClick = {
            viewModel.onGoogleLoginClick(
                context = context,
                onSuccess = onHomeClick   // 로그인 성공 시 홈 이동
            )
        },
    )
}

private enum class TermsType {
    SERVICE,
    PRIVACY,
    LOCATION,
    SENSITIVE,
    COMMUNITY
}
@Composable
private fun StartContent(
    onLoginClick: () -> Unit,
) {
    var serviceTermsChecked by remember { mutableStateOf(false) }
    var privacyChecked by remember { mutableStateOf(false) }
    var locationChecked by remember { mutableStateOf(false) }
    var sensitiveInfoChecked by remember { mutableStateOf(false) }
    var communityChecked by remember { mutableStateOf(false) }

    var showTermsDialog by remember { mutableStateOf(false) }

    val requiredTermsChecked =
        serviceTermsChecked &&
                privacyChecked &&
                locationChecked &&
                communityChecked

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            SimpleHorizontalPager()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 18.dp)
                    .clickable {
                        showTermsDialog = true
                    }
            ) {
                Checkbox(
                    checked = requiredTermsChecked,
                    onCheckedChange = {
                        showTermsDialog = true
                    }
                )

                Text(
                    text = "이용약관 및 개인정보 처리방침에 동의합니다",
                    color = White,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        showTermsDialog = true
                    }
                )
            }

            Image(
                painter = painterResource(R.drawable.google_light_sq_si),
                contentDescription = "Google Login",
                modifier = Modifier
                    .clickable {
                        if (requiredTermsChecked) {
                            onLoginClick()
                        } else {
                            showTermsDialog = true
                        }
                    }
            )

            if (!requiredTermsChecked) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "필수 약관에 모두 동의해야 시작할 수 있습니다.",
                    color = Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (showTermsDialog) {
            TermsAgreementDialog(
                serviceTermsChecked = serviceTermsChecked,
                onServiceTermsCheckedChange = { serviceTermsChecked = it },
                privacyChecked = privacyChecked,
                onPrivacyCheckedChange = { privacyChecked = it },
                locationChecked = locationChecked,
                onLocationCheckedChange = { locationChecked = it },
                sensitiveInfoChecked = sensitiveInfoChecked,
                onSensitiveInfoCheckedChange = { sensitiveInfoChecked = it },
                communityChecked = communityChecked,
                onCommunityCheckedChange = { communityChecked = it },
                onDismiss = {
                    showTermsDialog = false
                }
            )
        }
    }
}
@Composable
private fun TermsAgreementDialog(
    serviceTermsChecked: Boolean,
    onServiceTermsCheckedChange: (Boolean) -> Unit,
    privacyChecked: Boolean,
    onPrivacyCheckedChange: (Boolean) -> Unit,
    locationChecked: Boolean,
    onLocationCheckedChange: (Boolean) -> Unit,
    sensitiveInfoChecked: Boolean,
    onSensitiveInfoCheckedChange: (Boolean) -> Unit,
    communityChecked: Boolean,
    onCommunityCheckedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTermsType by remember { mutableStateOf<TermsType?>(null) }

    val allRequiredChecked =
        serviceTermsChecked &&
                privacyChecked &&
                locationChecked &&
                communityChecked

    val allChecked =
        serviceTermsChecked &&
                privacyChecked &&
                locationChecked &&
                sensitiveInfoChecked &&
                communityChecked

    val title = selectedTermsType?.let {
        getTermsTitle(it)
    } ?: "약관 동의"

    AlertDialog(
        onDismissRequest = onDismiss,

        containerColor = Color.White,
        title = {
            Text(text = title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
            ) {
                if (selectedTermsType == null) {
                    TermsAgreementListContent(
                        allChecked = allChecked,
                        onAllCheckedChange = { checked ->
                            onServiceTermsCheckedChange(checked)
                            onPrivacyCheckedChange(checked)
                            onLocationCheckedChange(checked)
                            onSensitiveInfoCheckedChange(checked)
                            onCommunityCheckedChange(checked)
                        },
                        serviceTermsChecked = serviceTermsChecked,
                        onServiceTermsCheckedChange = onServiceTermsCheckedChange,
                        privacyChecked = privacyChecked,
                        onPrivacyCheckedChange = onPrivacyCheckedChange,
                        locationChecked = locationChecked,
                        onLocationCheckedChange = onLocationCheckedChange,
                        sensitiveInfoChecked = sensitiveInfoChecked,
                        onSensitiveInfoCheckedChange = onSensitiveInfoCheckedChange,
                        communityChecked = communityChecked,
                        onCommunityCheckedChange = onCommunityCheckedChange,
                        onDetailClick = { termsType ->
                            selectedTermsType = termsType
                        },
                        allRequiredChecked = allRequiredChecked
                    )
                } else {
                    TermsDetailContent(
                        termsType = selectedTermsType!!,
                        onBackClick = {
                            selectedTermsType = null
                        }
                    )
                }
            }
        },
        confirmButton = {
            if (selectedTermsType == null) {
                TextButton(
                    onClick = onDismiss,
                    enabled = allRequiredChecked
                ) {
                    Text("확인")
                }
            } else {
                TextButton(
                    onClick = {
                        when (selectedTermsType) {
                            TermsType.SERVICE -> onServiceTermsCheckedChange(true)
                            TermsType.PRIVACY -> onPrivacyCheckedChange(true)
                            TermsType.LOCATION -> onLocationCheckedChange(true)
                            TermsType.SENSITIVE -> onSensitiveInfoCheckedChange(true)
                            TermsType.COMMUNITY -> onCommunityCheckedChange(true)
                            null -> Unit
                        }
                        selectedTermsType = null
                    }
                ) {
                    Text("동의")
                }
            }
        },
        dismissButton = {
            if (selectedTermsType == null) {
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            } else {
                TextButton(
                    onClick = {
                        selectedTermsType = null
                    }
                ) {
                    Text("뒤로")
                }
            }
        }
    )
}
@Composable
private fun TermsAgreementListContent(
    allChecked: Boolean,
    onAllCheckedChange: (Boolean) -> Unit,
    serviceTermsChecked: Boolean,
    onServiceTermsCheckedChange: (Boolean) -> Unit,
    privacyChecked: Boolean,
    onPrivacyCheckedChange: (Boolean) -> Unit,
    locationChecked: Boolean,
    onLocationCheckedChange: (Boolean) -> Unit,
    sensitiveInfoChecked: Boolean,
    onSensitiveInfoCheckedChange: (Boolean) -> Unit,
    communityChecked: Boolean,
    onCommunityCheckedChange: (Boolean) -> Unit,
    onDetailClick: (TermsType) -> Unit,
    allRequiredChecked: Boolean
) {
    Column {
        Text(
            text = "RUNUP 서비스 이용을 위해 필수 약관에 동의해 주세요.",
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        TermsDialogCheckRow(
            checked = allChecked,
            onCheckedChange = onAllCheckedChange,
            text = "전체 동의",
            showDetailButton = false,
            onDetailClick = {}
        )

        Spacer(modifier = Modifier.height(8.dp))

        TermsDialogCheckRow(
            checked = serviceTermsChecked,
            onCheckedChange = onServiceTermsCheckedChange,
            text = "[필수] 서비스 이용약관에 동의합니다.",
            onDetailClick = { onDetailClick(TermsType.SERVICE) }
        )

        TermsDialogCheckRow(
            checked = privacyChecked,
            onCheckedChange = onPrivacyCheckedChange,
            text = "[필수] 개인정보 수집 및 이용에 동의합니다.",
            onDetailClick = { onDetailClick(TermsType.PRIVACY) }
        )

        TermsDialogCheckRow(
            checked = locationChecked,
            onCheckedChange = onLocationCheckedChange,
            text = "[필수] 위치기반서비스 이용약관에 동의합니다.",
            onDetailClick = { onDetailClick(TermsType.LOCATION) }
        )

        TermsDialogCheckRow(
            checked = communityChecked,
            onCheckedChange = onCommunityCheckedChange,
            text = "[필수] 커뮤니티 게시물 활용 및 코스 공유에 동의합니다.",
            onDetailClick = { onDetailClick(TermsType.COMMUNITY) }
        )

        TermsDialogCheckRow(
            checked = sensitiveInfoChecked,
            onCheckedChange = onSensitiveInfoCheckedChange,
            text = "[선택] 민감정보 처리에 동의합니다.",
            onDetailClick = { onDetailClick(TermsType.SENSITIVE) }
        )

        if (!allRequiredChecked) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "필수 약관 4개에 모두 동의해야 가입할 수 있습니다.",
                color = Color.Red,
                fontSize = 12.sp
            )
        }
    }
}
@Composable
private fun TermsDialogCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    showDetailButton: Boolean = true,
    onDetailClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Text(
            text = text,
            fontSize = 13.sp,
            modifier = Modifier
                .weight(1f)
                .clickable {
                    onCheckedChange(!checked)
                }
        )

        if (showDetailButton) {
            Text(
                text = "보기",
                fontSize = 12.sp,
                color = TextGray,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable {
                        onDetailClick()
                    }
            )
        }
    }
}
@Composable
private fun TermsDetailContent(
    termsType: TermsType,
    onBackClick: () -> Unit
) {
    Column {
        Text(
            text = getTermsContent(termsType),
            fontSize = 13.sp,
            modifier = Modifier
                .height(360.dp)
                .verticalScroll(rememberScrollState())
        )
    }
}
private fun getTermsContent(termsType: TermsType): String {
    return when (termsType) {
        TermsType.SERVICE -> """
            RUNUP 서비스 이용약관

            제1조 목적
            본 약관은 RUNUP이 제공하는 러닝 기록, 경로 추천, 운동 분석, 커뮤니티 기능 등 서비스의 이용 조건과 절차, 이용자와 RUNUP의 권리와 의무를 정하는 것을 목적으로 합니다.

            제2조 서비스 내용
            RUNUP은 다음 기능을 제공합니다.
            1. 러닝 기록 측정 및 저장
            2. 러닝 목표 설정 및 관리
            3. 위치 기반 러닝 경로 추천
            4. 운동 데이터 분석
            5. 코스 및 게시물 공유 기능
            6. 기타 RUNUP이 제공하는 러닝 관련 기능

            제3조 회원가입 및 로그인
            이용자는 Google 계정을 이용하여 RUNUP에 가입하거나 로그인할 수 있습니다. 이 과정에서 이메일, 이름 등 기본 계정 정보가 수집될 수 있습니다.

            제4조 이용자의 의무
            이용자는 타인의 계정 도용, 허위 위치정보 등록, 조작된 운동 기록 등록, 타인의 권리 침해, 서비스 운영 방해, 법령 위반 행위를 해서는 안 됩니다.

            제5조 서비스 이용 제한
            RUNUP은 이용자가 약관을 위반하거나 서비스 운영을 방해하는 경우 서비스 이용을 제한할 수 있습니다.

            제6조 책임의 제한
            RUNUP이 제공하는 경로 추천, 운동 분석, 목표 제안 등은 참고용 정보입니다. 이용자는 자신의 건강 상태와 주변 환경을 고려하여 안전하게 서비스를 이용해야 합니다.
        """.trimIndent()

        TermsType.PRIVACY -> """
            개인정보 수집 및 이용 동의

            1. 수집 항목
            - 이메일
            - 이름
            - Google 계정 식별 정보
            - 러닝 기록
            - 운동 목표
            - 위치 데이터
            - 센서 데이터
            - 앱 접속 기록 및 기기 정보

            2. 이용 목적
            - 회원 식별 및 로그인
            - 러닝 기록 저장
            - 운동 목표 관리
            - 경로 추천 및 운동 분석
            - 서비스 오류 확인 및 품질 개선
            - 부정 이용 방지

            3. 보유 및 이용 기간
            수집된 개인정보는 회원 탈퇴 시까지 보관됩니다.
            단, 관련 법령에 따라 보관이 필요한 경우 해당 기간 동안 보관될 수 있습니다.

            4. 동의 거부 권리
            이용자는 개인정보 수집 및 이용에 동의하지 않을 수 있습니다.
            다만 필수 개인정보 수집 및 이용에 동의하지 않을 경우 RUNUP 서비스 이용이 제한될 수 있습니다.
        """.trimIndent()

        TermsType.LOCATION -> """
            위치기반서비스 이용약관

            제1조 목적
            본 약관은 RUNUP이 제공하는 위치기반서비스와 관련하여 이용자의 위치정보 수집, 이용 및 보호에 관한 사항을 정하는 것을 목적으로 합니다.

            제2조 위치기반서비스 내용
            RUNUP은 이용자의 위치정보를 활용하여 다음 서비스를 제공합니다.
            1. 현재 위치 기반 러닝 경로 추천
            2. 러닝 중 실시간 경로 기록
            3. 이동 거리, 속도, 페이스 측정
            4. 출발지, 도착지 및 이동 경로 저장
            5. 위치 기반 운동 기록 분석

            제3조 수집하는 위치정보
            RUNUP은 GPS 기반 실시간 위치정보, 러닝 중 이동 경로, 출발 위치 및 종료 위치, 위치 기반 운동 기록을 수집할 수 있습니다.

            제4조 위치정보 이용 목적
            위치정보는 러닝 경로 추천, 운동 기록 측정, 이동 거리 계산, 페이스 분석, 경로 저장 및 서비스 개선을 위해 이용됩니다.

            제5조 보유 및 이용 기간
            실시간 위치정보는 서비스 제공 중에만 이용됩니다.
            러닝 경로 기록은 이용자가 삭제하거나 회원 탈퇴할 때까지 보관될 수 있습니다.

            제6조 동의 철회
            이용자는 언제든지 앱 권한 설정을 통해 위치정보 접근 권한을 변경하거나 차단할 수 있습니다.
            위치 권한을 허용하지 않을 경우 경로 추천 및 러닝 기록 기능 이용이 제한될 수 있습니다.
        """.trimIndent()

        TermsType.COMMUNITY -> """
            커뮤니티 게시물 활용 및 코스 공유 동의

            RUNUP은 이용자가 직접 생성한 코스, 사진, 러닝 기록, 게시글 등을 다른 사용자와 공유할 수 있는 기능을 제공할 수 있습니다.

            1. 대상 콘텐츠
            - 사용자가 생성한 러닝 코스
            - 사용자가 업로드한 사진
            - 운동 기록
            - 리뷰, 게시글, 댓글
            - 공유 설정한 경로 정보

            2. 활용 목적
            - 다른 이용자에게 코스 공유
            - 추천 코스 또는 인기 코스 노출
            - 커뮤니티 게시물 표시
            - 서비스 내 검색 및 추천 기능 제공
            - 서비스 개선 및 품질 향상

            3. 게시물 권리
            이용자가 작성한 게시물의 저작권은 원칙적으로 이용자에게 있습니다.
            다만 이용자는 RUNUP이 서비스 운영 및 노출에 필요한 범위 내에서 해당 게시물을 사용할 수 있도록 허락합니다.

            4. 필수 동의
            본 항목은 RUNUP 서비스 이용을 위한 필수 동의 항목입니다.
            동의하지 않을 경우 커뮤니티 게시물 활용, 코스 공유 기능을 포함한 RUNUP 서비스 이용이 제한될 수 있습니다.
        """.trimIndent()
        TermsType.SENSITIVE -> """
            민감정보 처리 동의

            RUNUP은 운동 분석 및 맞춤형 피드백 제공을 위해 건강 상태와 관련될 가능성이 있는 데이터를 처리할 수 있습니다.

            1. 수집 및 처리 항목
            - 발 압력 데이터
            - 가속도 데이터
            - 움직임 패턴
            - 운동 부하 데이터
            - 러닝 자세 분석 결과
            - 피로도 또는 운동 강도 추정 정보

            2. 이용 목적
            - 러닝 자세 분석
            - 운동 부하 분석
            - 맞춤형 운동 피드백 제공
            - 부상 위험 감소를 위한 참고 정보 제공
            - 개인별 러닝 기록 및 목표 관리

            3. 보유 및 이용 기간
            민감정보에 해당할 수 있는 데이터는 회원 탈퇴 시 또는 이용자가 해당 기록을 삭제할 때까지 보관됩니다.

            4. 동의 거부 권리
            이용자는 민감정보 처리에 동의하지 않을 수 있습니다.
            다만 동의하지 않을 경우 발 압력 분석, 운동 부하 분석, 맞춤형 피드백 등 일부 기능 이용이 제한될 수 있습니다.
        """.trimIndent()
    }
}
private fun getTermsTitle(termsType: TermsType): String {
    return when (termsType) {
        TermsType.SERVICE -> "서비스 이용약관"
        TermsType.PRIVACY -> "개인정보 수집 및 이용 동의"
        TermsType.LOCATION -> "위치기반서비스 이용약관"
        TermsType.SENSITIVE -> "민감정보 처리 동의"
        TermsType.COMMUNITY -> "커뮤니티 게시물 활용 및 코스 공유 동의"
    }
}

/*
@Composable
private fun StartContent(
    onLoginClick: () -> Unit,
) {
    var isTermsChecked by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            SimpleHorizontalPager()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 18.dp)
            ) {
                Checkbox(
                    checked = isTermsChecked,
                    onCheckedChange = { isTermsChecked = it }
                )

                Text(
                    text = "이용약관 및 개인정보 처리방침에 동의합니다",
                    color = White,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        showTermsDialog = true
                    }
                )
            }

            Image(
                painter = painterResource(R.drawable.google_light_sq_si),
                contentDescription = "Google Login",
                modifier = Modifier
                    .clickable {
                        if (isTermsChecked) {
                            onLoginClick()
                        } else {
                            showTermsDialog = true
                        }
                    }
            )
        }

        if (showTermsDialog) {
            TermsDialog(
                onDismiss = { showTermsDialog = false },
                onAgree = {
                    isTermsChecked = true
                    showTermsDialog = false
                }
            )
        }
    }
}

 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimpleHorizontalPager() {
    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount }) //

    Column(Modifier
        .fillMaxWidth()
        .wrapContentHeight()

        .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp)
    ) {
        HorizontalPager(
            state = pagerState, //
            modifier = Modifier.height(600.dp).fillMaxWidth()
        ) { pageIndex ->
            when (pageIndex) {
                0 -> { page0() }
                1 -> { page1() }
                2 -> { page2() }
                3 -> { page3() }
            }
        }
        PageIndicator(pagerState, pageCount)
    }
}



@Preview
@Composable
private fun Previewpage0(){
    page3()
}

@Composable
private fun page0(){
    Column(modifier = Modifier.fillMaxSize().padding(top = 100.dp)){
        StartText(text = "달리기를 시작해 볼까요?")
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ){
            Image(
                painter = painterResource(id = R.drawable.icon_character),
                contentDescription = "목표를 세우고 러닝을 시작해요"
            )
        }
    }
}

@Composable
private fun page1(){
    Column(modifier = Modifier.fillMaxSize().padding(top = 100.dp)){
        StartText(text = "목표를 세우고 러닝을 시작해요")
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ){
            Image(
                painter = painterResource(id = R.drawable.tuto_goalset),
                contentDescription = "목표를 세우고 러닝을 시작해요"
            )
        }
    }
}

@Composable
private fun page2(){
    Column(modifier = Modifier.fillMaxSize().padding(top = 100.dp)){
        StartText(text = "새로운 코스를 찾아 달려봐요")
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ){
            Image(
                painter = painterResource(id = R.drawable.tuto_course),
                contentDescription = "새로운 코스를 찾고 달려봐요"
            )
        }
    }
}

@Composable
private fun page3(){
    Column(modifier = Modifier.fillMaxSize().padding(top = 100.dp)){
        StartText(text = "함께 달리고 러닝을 공유해요")
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ){
            Image(
                painter = painterResource(id = R.drawable.tuto_community),
                contentDescription = "함께 달리고 러닝을 공유해요"
            )
        }
    }
}

@Composable
private fun StartText(
    text:String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    fontSize: TextUnit = 48.sp
) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        color = White,
        modifier = modifier
    )
}

@Preview
@Composable
private fun PreviewStartContent() {
    StartContent(
        {},
    )
}