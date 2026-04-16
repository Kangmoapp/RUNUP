package com.example.runup.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.domain.model.CourseRecommendation
import com.example.runup.domain.model.SortType
import com.example.runup.ui.components.DistanceGoalSettingDialog
import com.example.runup.ui.components.MyGoogleMap
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.TextGray
import com.example.runup.ui.theme.TextWhite
import com.example.runup.ui.theme.White
import com.example.runup.viewmodel.CourseRecommendationUiState
import com.example.runup.viewmodel.CourseRecommendationViewModel
import kotlin.Boolean

@Composable
fun CourseRecommendationScreen(
    onBackClick:()->Unit,
    onMenuClick:()->Unit,
    viewModel: CourseRecommendationViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsState()
    CourseRecommendationContent(
        uiState = uiState,
        onDistanceClick = {viewModel.openDistanceDialog()},
        onDistanceClose = {viewModel.closeDistanceDialog()},
        onDistanceConfirm = {viewModel.confirmDistance(it)},
        onSortClick = {viewModel.openSortDialog()},
        onSortClose = {viewModel.closeSortDialog()},
        onSortConfirm = { sort ->
            viewModel.confirmSort(sort)
        },
        openLoopDialog = {viewModel.openLoopDialog()},

        loopSelect = { isFirst ->
            viewModel.loopSelect(isFirst)
        },
        onBackClick = onBackClick,
        onMenuClick = onMenuClick,
        onSearchClick = {viewModel.onSearchClick()},
        addIndex = {viewModel.addIndex()},
        subtractIndex = {viewModel.subtractIndex()}
    )


    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetUiState()
        }
    }
}


@Composable
private fun CourseRecommendationContent(
    uiState: CourseRecommendationUiState,
    onDistanceClick:()->Unit,
    onDistanceClose:()->Unit,
    onDistanceConfirm:(Int)->Unit,
    onSortClick:()->Unit,
    onSortClose:()->Unit,
    onSortConfirm:(SortType)->Unit,
    openLoopDialog:()->Unit,
    loopSelect:(Boolean)->Unit,
    onBackClick:()->Unit,
    onMenuClick:()->Unit,
    onSearchClick:()->Unit,
    addIndex:()->Unit,
    subtractIndex:()->Unit
){
    val textLoopFirst :String = if(uiState.isLoop) "왕복" else "편도"
    val textLoopSecond :String = if(uiState.isLoop) "편도" else "왕복"

    val loopVisibleState = remember { MutableTransitionState(false) }
    loopVisibleState.targetState = uiState.showLoop
    val hasCourse = uiState.recommendedCourses.isNotEmpty()
    val firstCourse = uiState.recommendedCourses.firstOrNull()

    val RecommendBtnText =
        if(uiState.isRecommendClick){
            "코스 선택"
        }
        else {
        "코스 추천 받기"
        }

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
        ){
            Box(){

            }
            TopBar(onBackClick = onBackClick, onMenuClick = onMenuClick)
            Box(
                modifier = Modifier
                    .height(550.dp)
                    .fillMaxWidth()
            ){
                Box{
                    uiState.cameraLocation?.let { location ->
                        MyGoogleMap(
                            cameraPosition = location,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(color = White),
                            isCourse = uiState.isRecommendClick,
                            course = uiState.recommendedCourses
                                .getOrNull(uiState.courseIndex)
                                ?.path
                                ?.points
                                ?: emptyList()
                        )
                    }
                    if(uiState.isLoading){
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(450.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    if(uiState.isRecommendClick){
                        val currentCourse = uiState.recommendedCourses.getOrNull(uiState.courseIndex)
                        Column(
                            modifier = Modifier.padding(start = 18.dp, top = 18.dp)
                        ){
                            // [추가] 상세 정보 카드
                            currentCourse?.let { course ->
                                CourseInfoCard(uiState, course = course)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .padding(top = 350.dp,start = 18.dp, end = 18.dp)
                                .fillMaxWidth()
                        ){
                            RecommendButton(
                                text = "이전",
                                modifier = Modifier
                                    .height(100.dp)
                                    .width(80.dp),
                                onClick = subtractIndex
                            )
                            Spacer(Modifier.weight(1f))
                            RecommendButton(
                                text = "다음",
                                modifier = Modifier
                                    .height(100.dp)
                                    .width(80.dp),
                                onClick = addIndex
                            )
                        }
                    }
                }
                RecommendButton(
                    text = RecommendBtnText,
                    modifier = Modifier
                        .align(Alignment.BottomCenter),
                    onClick = onSearchClick
                )
            }

            Spacer(modifier = Modifier.height(25.dp))

            Row(
                modifier = Modifier
                    .fillMaxSize()
            ){
                Column(
                    modifier = Modifier
                        .weight(1f)
                ){
                    TextBottom(text = "목표 러닝 거리", isLeft = true)
                    TextBottom(text = "거리 계산 방법", isLeft = true)
                    TextBottom(text = "정렬 방법", isLeft = true)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                ){
                    TextBottom(text = "${uiState.goalDistance/1000.toDouble()} km", isLeft = false, onClick = onDistanceClick)

                    LoopDialog(
                        textLoopFirst = textLoopFirst,
                        textLoopSecond = textLoopSecond,
                        visibleState = loopVisibleState,
                        onMainClick = openLoopDialog,
                        loopSelect = loopSelect
                    )
                    if (
                        !loopVisibleState.currentState &&
                        !loopVisibleState.targetState &&
                        loopVisibleState.isIdle
                    ) {
                        TextBottom(
                            text = uiState.currentSort.label,
                            isLeft = false,
                            onClick = onSortClick
                        )
                    }
                }
            }
        }
    }


    if (uiState.showDistanceDialog) {
        DistanceGoalSettingDialog(
            range = 0..100,
            startNumber = (uiState.goalDistance/100 + 1),
            onConfirm = onDistanceConfirm,
            onDismiss = onDistanceClose
        )
    }

    if (uiState.showSortDialog) {
        CategoryDialog(
            currentSort = uiState.currentSort,
            onConfirm = {
                onSortConfirm(it)
            },
            onDismiss = {
                onSortClose()
            }
        )
    }
}

@Preview
@Composable
private fun PreviewCategoryDialog(){
    CategoryDialog(currentSort = SortType.DISTANCE,{},{})
}

@Composable
private fun CategoryDialog(
    currentSort: SortType,
    onConfirm: (SortType) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSort by remember(currentSort) { mutableStateOf(currentSort) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedSort) },
                shape = RectangleShape,
            ) {
                Text(
                    text = "취소",
                    color = TextBlack)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                SortType.entries.forEach { sortType ->
                    Column (
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable { onConfirm(sortType) }
                            .padding(start = 18.dp)
                    ){
                        Text(
                            text = sortType.label,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                }
            }
        },
        containerColor = White,
        shape = RoundedCornerShape(15.dp),
    )
}
@Composable
private fun RecommendButton(
    text:String,
    modifier:Modifier = Modifier,
    onClick:()->Unit
){
    Column(
        modifier = modifier
            .padding(bottom = 30.dp)
    ){
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(60.dp)
                .width(220.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color = White, shape = RoundedCornerShape(10.dp))
                .border(
                    width = 3.dp,
                    color = PointColor,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onClick() }
        ){
            Text(
                text = text,
                fontSize = 25.sp
            )

        }
    }
}



@Composable
private fun TextBottom(
    text:String,
    modifier:Modifier = Modifier,
    isLeft:Boolean,
    onClick:()->Unit = {}
){
    Box(
        modifier = modifier
            .height(55.dp)
            .padding(start = 18.dp, top = 10.dp, bottom = 10.dp, end = 18.dp)
    ){
        if(isLeft){
            Text(
                text = text,
                fontSize = 22.sp,
                color = TextWhite,
                modifier = Modifier
            )
        }
        else{
            InfoText(text = text,
                modifier = Modifier
                    .background(color = White, shape = RoundedCornerShape(5.dp))
                    .clickable { onClick() }
            )
        }

    }
}

@Composable
private fun LoopDialog(
    textLoopFirst: String,
    textLoopSecond: String,
    visibleState: MutableTransitionState<Boolean>,
    onMainClick: () -> Unit,
    loopSelect: (Boolean) -> Unit,
) {
    val isExpanded = visibleState.currentState || visibleState.targetState

    Column(
        modifier = Modifier
            .padding(start = 18.dp, top = 10.dp, bottom = 10.dp, end = 18.dp)
            .background(shape = RoundedCornerShape(5.dp), color = Color.Transparent)
    ) {
        val boxModifier:Modifier
        if(isExpanded) boxModifier = Modifier.background(shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp), color = White)
        else boxModifier = Modifier.background(shape = RoundedCornerShape(5.dp), color = White)
        InfoText(
            text = textLoopFirst,
            modifier = boxModifier
                .fillMaxWidth()
                .clickable {
                    if (isExpanded) {
                        loopSelect(true)   // 현재 값 다시 선택하면서 닫기
                    } else {
                        onMainClick()      // 펼치기
                    }
                }
        )

        AnimatedVisibility(
            visibleState = visibleState,
            enter = expandVertically(
                expandFrom = Alignment.Top
            ) + fadeIn(),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top
            ) + fadeOut()
        ) {
            Column (
                modifier = Modifier
                    .background(shape = RoundedCornerShape(bottomStart = 5.

                    dp, bottomEnd = 5.dp), color = White)
            ){
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = Gray)
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { loopSelect(false) }
                        .height(38.dp)
                        .fillMaxWidth()
                        .padding(end = 10.dp)
                ){
                    Text(
                        text = textLoopSecond,
                        fontSize = 22.sp,
                        color = TextBlack,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoText(
    text:String,
    modifier:Modifier = Modifier
){
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(38.dp)
            .fillMaxWidth()
            .padding(end = 10.dp)
    ){
        Text(
            text = text,
            fontSize = 22.sp,
            color = TextBlack,
        )
    }
}

@Composable
private fun CourseInfoCard(
    uiState: CourseRecommendationUiState,
    course: CourseRecommendation,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(220.dp) // 가로를 살짝 넓혀서 정보를 병렬로 배치
            .background(
                color = White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 2.dp,
                color = PointColor,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. 상단 행: [번호. ID] [거리]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.courseIndex + 1}. ${course.originCourse.id}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            Text(
                text = String.format("%.2f km", course.path.distance / 1000.0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PointColor
            )
        }

        Divider(color = Gray.copy(alpha = 0.3f), thickness = 1.dp)

        // 2. 하단 행: [주변 장소(좌)] | [Score(우)]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 주변 장소
            Column(modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "주변 장소", fontSize = 10.sp, color = TextGray)
                Text(
                    text = course.originCourse.landmark.ifEmpty { "정보 없음" },
                    fontSize = 13.sp,
                    color = TextBlack,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2, // 장소 이름이 길어질 경우 대비
                    textAlign = TextAlign.Center
                )
            }

            // 중앙 구분선 (선택 사항)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(1.dp).height(30.dp).background(Gray.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.width(8.dp))

            // 우측: Score 영역
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "score", fontSize = 9.sp, color = TextGray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ScoreSmallItem("밝기", course.originCourse.scores.brightScore)
                    ScoreSmallItem("붐빔", course.originCourse.scores.crowdedScore)
                    ScoreSmallItem("난이도", course.originCourse.scores.hardScore)
                }
            }
        }
    }
}

@Composable
private fun ScoreSmallItem(label: String, score: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = TextGray)
        Text(
            text = String.format("%.1f", score),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack
        )
    }
}

@Preview
@Composable
private fun PreviewCourseRecommendationContent(){
    CourseRecommendationContent(
        uiState = CourseRecommendationUiState(
            showLoop= false,
        ),
        {},{},{},{},{},{}, {},{},{},{},{},{},{},
    )
}