package com.runit.runup.ui.screens

import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.runit.runup.ui.theme.BackGroudColor
import com.runit.runup.viewmodel.StartViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runit.runup.R
import com.runit.runup.ui.components.PageIndicator
import com.runit.runup.ui.navigation.TermsType
import com.runit.runup.ui.navigation.getTermsText
import com.runit.runup.ui.theme.Gray
import com.runit.runup.ui.theme.TextGray
import com.runit.runup.ui.theme.White


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
        it.title
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
                    .wrapContentHeight()
                    //.height(400.dp),
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
            text = getTermsText(termsType),
            fontSize = 13.sp,
            modifier = Modifier
                .height(360.dp)
                .verticalScroll(rememberScrollState())
        )
    }
}

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