package com.example.runup.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.runup.domain.model.MarkerSlot
import com.example.runup.domain.model.Post
import com.example.runup.domain.model.PostImage
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.viewmodel.CommunityViewModel
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.example.runup.ui.util.mapper.TimeMapper.formatTimestamp
import com.google.firebase.firestore.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onBackClick: () -> Unit, // 뒤로가기 클릭
    onPostClick: (String) -> Unit, // 포스트 클릭
    onUploadClick: () -> Unit, // 업로드 버튼 클릭
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val communityState by viewModel.communityUiState.collectAsState()

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.savedScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.savedScrollOffset
    )

    // 처음 진입 시 데이터 호출
    LaunchedEffect(Unit) {
        viewModel.fetchPosts(isInitial = true) // 포스트 불러옴
    }

    // 스크롤 저장용 추가
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            viewModel.saveScrollState(
                scrollState.firstVisibleItemIndex,
                scrollState.firstVisibleItemScrollOffset
            )
        }
    }

    // 무한 스크롤 감지 (기존 유지)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= scrollState.layoutInfo.totalItemsCount - 1
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !communityState.isLoading) {
            viewModel.fetchPosts(isInitial = false)
        }
    }

    BackHandler { onBackClick() }

    Scaffold(
        containerColor = BackGroudColor,
        topBar = {
            TopBar(
                onBackClick = onBackClick,
                text = "커뮤니티",
                isMenu = false,
                insteadMenuComponent = {
                    Icon(
                        Icons.Default.Add,
                        "글쓰기",
                        tint = WhiteTextColor,
                        modifier = Modifier.size(28.dp).clickable{onUploadClick()}
                    )
                }
            ) },
    ) { padding ->
        PullToRefreshBox(
            // 로딩 아이콘을 보여줄지 말지 ViewModel 상태에 맡깁니다.
            isRefreshing = communityState.isLoading,
            onRefresh = {
                // 위로 당기면 실행될 로직
                viewModel.fetchPosts(isInitial = true, forceRefresh = true)
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ){
            // 메인 콘텐츠: 초기 로딩이 끝났을 때만 보여줌
            if (!communityState.isInitialLoading) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(communityState.posts, key = { it.postId }) { post ->
                        PostItem(
                            post = post,
                            onClick = { onPostClick(post.postId) },
                            onLikeClick = { viewModel.onLikeClick(post.postId) },
                            viewModel = viewModel
                        )
                    }

                    // 추가 로딩 바
                    if (communityState.isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }

            // 전체 화면 로딩 오버레이
            // 데이터 수신 + 비트맵 캐시 완료까지 이 화면이 유지됩니다.
            if (communityState.isInitialLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(BackGroudColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("다른 러너들과 연결 중입니다...", color = Color.White)
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostItem(
    post: Post,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    viewModel: CommunityViewModel // 1. ViewModel 추가
) {
    val bitmapMap by viewModel.bitmapCache.collectAsState() // ViewModel의 캐시 구독
    var hasReportedLoaded by remember(post.postId) { mutableStateOf(false) } // 이 포스트가 보고를 완료했는지 체크
    var enlargedImageUri by remember { mutableStateOf<String?>(null) }

    // 페이저 상태 관리 (총 페이지 수 = 지도(1) + 일반 이미지 개수)
    val totalPages = 1 + post.commonImages.size
    val pagerState = rememberPagerState(pageCount = { totalPages })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // 유저 헤더 영역
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Gray))
            Spacer(modifier = Modifier.width(10.dp))
            Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreVert, null, tint = WhiteTextColor)
        }

        // 지도 및 마커 오버레이 영역
        // --- 수정된 메인 콘텐츠 영역 (페이저 적용) ---
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f) // 정사각형 비율 유지
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    // [페이지 0] 기존의 지도 및 마커 오버레이 영역
                    post.runRecord?.let { record ->
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val density = LocalDensity.current
                            val widthDp = maxWidth.value.toInt()
                            val highResRequestSize = 640
                            val mapWidthPx = constraints.maxWidth.toFloat()
                            val mapHeightPx = constraints.maxHeight.toFloat()

                            val dynamicZoom = remember(record.course) {
                                val latDiff = record.course.maxLat - record.course.minLat
                                val lngDiff = record.course.maxLng - record.course.minLng
                                val maxDiff = maxOf(latDiff, lngDiff)
                                when {
                                    maxDiff > 0.04 -> 14
                                    maxDiff > 0.015 -> 15
                                    maxDiff > 0.005 -> 16
                                    maxDiff > 0.002 -> 17
                                    else -> 18
                                }
                            }

                            val centerLat = (record.course.minLat + record.course.maxLat) / 2
                            val centerLng = (record.course.minLng + record.course.maxLng) / 2

                            val (cMinX, cMinY) = latLngToPixel(record.course.minLat, record.course.minLng, centerLat, centerLng, dynamicZoom, mapWidthPx, mapHeightPx, highResRequestSize)
                            val (cMaxX, cMaxY) = latLngToPixel(record.course.maxLat, record.course.maxLng, centerLat, centerLng, dynamicZoom, mapWidthPx, mapHeightPx, highResRequestSize)

                            val staticMapUrl = remember(post.postId, highResRequestSize, dynamicZoom) {
                                val pathParam = record.course.locationPoints.joinToString("|") { "${it.locationPoint.latitude},${it.locationPoint.longitude}" }
                                buildString {
                                    append("https://maps.googleapis.com/maps/api/staticmap?")
                                    append("center=${centerLat},${centerLng}&zoom=${dynamicZoom}&size=${highResRequestSize}x${highResRequestSize}&scale=2")
                                    append("&path=color:0x000000FF|weight:3|${pathParam}")
                                    append("&markers=color:green|label:S|${record.course.locationPoints.first().locationPoint.latitude},${record.course.locationPoints.first().locationPoint.longitude}")
                                    append("&markers=color:red|label:E|${record.course.locationPoints.last().locationPoint.latitude},${record.course.locationPoints.last().locationPoint.longitude}")
                                    append("&key=AIzaSyDqeWVV33wzA4O2ZwHQpMazISZ-EMcjzTw") // 반드시 본인의 키로 변경하세요
                                }
                            }

                            AsyncImage(model = staticMapUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

                            val markerSizePx = with(density) { 64.dp.toPx() }
                            val slotAssignments = remember(post.postId, mapWidthPx) {
                                assignSlots(post.locationImages.filter { it.location != null }, centerLat, centerLng)
                            }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                slotAssignments.forEach { (postImage, slot) ->
                                    val location = postImage.location ?: return@forEach
                                    val (origX, origY) = latLngToPixel(location.latitude, location.longitude, centerLat, centerLng, dynamicZoom, mapWidthPx, mapHeightPx, highResRequestSize)
                                    val closerOffset = calculateCloserOffset(origX, origY, slot, mapWidthPx, mapHeightPx, cMinX, cMaxX, cMinY, cMaxY, markerSizePx)
                                    drawLine(color = Color.Black.copy(alpha = 0.8f), start = Offset(origX, origY), end = closerOffset, strokeWidth = 2f)
                                    drawCircle(color = Color.Black, radius = 5f, center = Offset(origX, origY))
                                }
                            }

                            slotAssignments.forEach { (postImage, slot) ->
                                val bitmap = bitmapMap[postImage.url] ?: return@forEach
                                val location = postImage.location ?: return@forEach
                                val (origX, origY) = latLngToPixel(location.latitude, location.longitude, centerLat, centerLng, dynamicZoom, mapWidthPx, mapHeightPx, highResRequestSize)
                                val closerOffset = calculateCloserOffset(origX, origY, slot, mapWidthPx, mapHeightPx, cMinX, cMaxX, cMinY, cMaxY, markerSizePx)

                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset((closerOffset.x - markerSizePx / 2).toInt(), (closerOffset.y - markerSizePx / 2).toInt()) }
                                        .clickable { enlargedImageUri = postImage.url }
                                ) {
                                    PhotoMarkerFromBitmap(bitmap = bitmap)
                                }
                            }
                        }
                    } ?: run {
                        // 러닝 기록이 없는 경우에 대한 예외 처리 (검은 바탕 등)
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                    }
                } else {
                    // [페이지 1 ~ N] commonImages 보여주기
                    val imageIndex = page - 1
                    val commonImage = post.commonImages[imageIndex]

                    AsyncImage(
                        model = commonImage.url, // 일반 이미지는 원본 URL 바로 사용
                        contentDescription = "Post Image ${imageIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { enlargedImageUri = commonImage.url }, // 클릭 시 확대
                        contentScale = ContentScale.Crop // 영역에 맞게 자름
                    )
                }
            }

            // --- 상단 페이지 네비게이션 (예: 1/3) ---
            if (totalPages > 1) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/$totalPages",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // --- 하단 인디케이터 (점) 영역 ---
        if (totalPages > 1) {
            Row(
                Modifier
                    .height(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { iteration ->
                    val color = if (pagerState.currentPage == iteration) WhiteTextColor else WhiteTextColor.copy(alpha = 0.3f)
                    val size = if (pagerState.currentPage == iteration) 8.dp else 6.dp
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(size)
                    )
                }
            }
        } else {
            // 이미지가 한 장일 때는 인디케이터 대신 여백
            Spacer(modifier = Modifier.height(8.dp))
        }


        // 하단 좋아요/댓글 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // vertical을 0.dp로 하거나 아주 작게(2.dp) 조정하여 위아래 간격을 줄임
                .padding(start = 10.dp, end = 16.dp, top = 0.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 좋아요 섹션
            Box(
                modifier = Modifier
                    // 높이를 40.dp에서 32.dp 정도로 줄여서 위아래 여백을 제거
                    .size(width = 42.dp, height = 32.dp)
                    .clickable { onLikeClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = if (post.likes > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (post.likes > 0) Color.Red else WhiteTextColor,
                    modifier = Modifier.size(24.dp) // 아이콘 크기를 살짝 줄여 더 밀착시킴
                )
                if (post.likes > 0) {
                    Text(
                        text = "${post.likes}",
                        color = WhiteTextColor,
                        fontSize = 10.sp, // 숫자 크기도 살짝 조절
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = -4.dp, y = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. 댓글 섹션
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 32.dp)
                    .clickable { onClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "💬",
                    fontSize = 18.sp, // 이모지 크기도 살짝 조절
                    modifier = Modifier.padding(bottom = 0.dp)
                )

                if (post.commentCount > 0) {
                    Text(
                        text = "${post.commentCount}",
                        color = WhiteTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = -4.dp, y = 2.dp)
                    )
                }
            }
        }

        // 내용 영역
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row {
                Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(post.content, color = WhiteTextColor)
            }

            // [추가] 날짜 영역
            Spacer(modifier = Modifier.height(4.dp)) // 내용과 날짜 사이 간격
            Text(
                text = formatTimestamp(post.timestamp),
                color = WhiteTextColor.copy(alpha = 0.5f), // 날짜는 약간 흐릿하게
                fontSize = 12.sp
            )
        }
    }

    if (enlargedImageUri != null) {
        EnlargedImageDialog(imageUrl = enlargedImageUri!!, onDismiss = { enlargedImageUri = null })
    }
}

fun latLngToPixel(
    lat: Double, lng: Double,
    centerLat: Double, centerLng: Double,
    zoom: Int,
    mapWidth: Float,
    mapHeight: Float,
    requestSize: Int
): Pair<Float, Float> {
    fun getMercatorY(latitude: Double): Double {
        val sinLat = sin(Math.toRadians(latitude))
        return ln((1 + sinLat) / (1 - sinLat)) / (2 * Math.PI)
    }

    val worldSize = 256.0 * 2.0.pow(zoom.toDouble())
    val dx = (lng - centerLng) * (worldSize / 360.0)
    val dy = (getMercatorY(lat) - getMercatorY(centerLat)) * (worldSize / 2.0)

    val ratio = mapWidth / requestSize.toFloat()

    val finalX = (mapWidth / 2f) + (dx.toFloat() * ratio)
    val finalY = (mapHeight / 2f) - (dy.toFloat() * ratio)

    return Pair(finalX, finalY)
}

fun assignSlots(
    images: List<PostImage>,
    centerLat: Double,
    centerLng: Double
): Map<PostImage, MarkerSlot> {
    val result = mutableMapOf<PostImage, MarkerSlot>()
    val unassignedImages = images.filter { it.location != null }.toMutableList()
    val availableSlots = MarkerSlot.entries.toMutableList()

    // 1단계: 각 사진을 사분면별로 그룹화 (우상, 우하, 좌하, 좌상)
    val quadrantGroups = mutableMapOf<MarkerSlot, MutableList<PostImage>>()
    MarkerSlot.entries.forEach { quadrantGroups[it] = mutableListOf() }

    unassignedImages.forEach { image ->
        val loc = image.location!!
        val dLat = loc.latitude - centerLat
        val dLng = loc.longitude - centerLng

        // 시계 방향 각도 계산 (12시 방향 0도 기준)
        var clockAngle = 90.0 - Math.toDegrees(Math.atan2(dLat, dLng))
        if (clockAngle < 0) clockAngle += 360.0
        if (clockAngle >= 360) clockAngle -= 360.0

        val preferredSlot = when {
            clockAngle in 0.0..90.0   -> MarkerSlot.TOP_RIGHT
            clockAngle in 90.0..180.0  -> MarkerSlot.BOTTOM_RIGHT
            clockAngle in 180.0..270.0 -> MarkerSlot.BOTTOM_LEFT
            else                       -> MarkerSlot.TOP_LEFT
        }
        quadrantGroups[preferredSlot]?.add(image)
    }

    // 2단계: 각 구역별로 '해당 슬롯'과 '사진' 사이의 거리를 계산하여 가장 가까운 것 배정
    quadrantGroups.forEach { (slot, candidateList) ->
        if (candidateList.isNotEmpty()) {
            // [핵심] 중심점이 아니라, 배정될 슬롯(구석)과 사진 사이의 거리 기준
            val bestMatch = candidateList.minBy { image ->
                calculateDistanceToSlot(image.location!!, slot, centerLat, centerLng)
            }
            result[bestMatch] = slot

            unassignedImages.remove(bestMatch)
            availableSlots.remove(slot)
        }
    }

    // 3단계: 남은 사진들을 남은 슬롯들과의 절대 거리가 가장 짧은 조합으로 매칭
    while (unassignedImages.isNotEmpty() && availableSlots.isNotEmpty()) {
        var minDistance = Double.MAX_VALUE
        var bestPair: Pair<PostImage, MarkerSlot>? = null

        for (image in unassignedImages) {
            for (slot in availableSlots) {
                val dist = calculateDistanceToSlot(image.location!!, slot, centerLat, centerLng)
                if (dist < minDistance) {
                    minDistance = dist
                    bestPair = image to slot
                }
            }
        }

        bestPair?.let { (image, slot) ->
            result[image] = slot
            unassignedImages.remove(image)
            availableSlots.remove(slot)
        } ?: break
    }

    return result
}

// 슬롯(화면 구석)과 사진 좌표 사이의 상대적 거리를 계산하는 보조 함수
private fun calculateDistanceToSlot(
    photoLoc: GeoPoint,
    slot: MarkerSlot,
    centerLat: Double,
    centerLng: Double
): Double {
    // 슬롯의 가상 좌표 설정 (중심에서 충분히 멀리 떨어진 구석 지점)
    // dLat, dLng의 부호만 중요하므로 방향성을 부여합니다.
    val targetLat = when (slot) {
        MarkerSlot.TOP_RIGHT, MarkerSlot.TOP_LEFT -> centerLat + 1.0 // 위쪽
        MarkerSlot.BOTTOM_RIGHT, MarkerSlot.BOTTOM_LEFT -> centerLat - 1.0 // 아래쪽
    }
    val targetLng = when (slot) {
        MarkerSlot.TOP_RIGHT, MarkerSlot.BOTTOM_RIGHT -> centerLng + 1.0 // 오른쪽
        MarkerSlot.BOTTOM_LEFT, MarkerSlot.TOP_LEFT -> centerLng - 1.0 // 왼쪽
    }

    val dLat = photoLoc.latitude - targetLat
    val dLng = photoLoc.longitude - targetLng
    return Math.sqrt(dLat * dLat + dLng * dLng)
}

// 비트맵 사진 마커 씌우는 컴포저블
@Composable
fun PhotoMarkerFromBitmap(bitmap: Bitmap) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 전체를 감싸는 검은색 프레임
        Box(
            modifier = Modifier
                .size(64.dp) // 크기를 살짝 조절
                .background(Color.Black, shape = MaterialTheme.shapes.small)
                .padding(3.dp) // 검은색 테두리 두께
        ) {
            Image(
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraSmall), // 사진 끝을 살짝만 굴림
                contentScale = ContentScale.Crop
            )
        }
    }
}

// 클릭한 마커의 사진 원본 가져오기
@Composable
fun EnlargedImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        confirmButton = {},
        containerColor = Color.Black.copy(alpha = 0.9f),
        text = {
            Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }
    )
}

fun calculateCloserOffset(
    actualX: Float,
    actualY: Float,
    slot: MarkerSlot,
    mapWidthPx: Float,
    mapHeightPx: Float,
    courseMinX: Float,
    courseMaxX: Float,
    courseMinY: Float,
    courseMaxY: Float,
    markerSizePx: Float
): Offset {
    // 1. 가려던 구석(Corner) 위치 결정 (마커 사이즈의 0.2만큼 여유 남겨두고 마커 슬롯 박기)
    val margin = markerSizePx * 0.8f
    val cornerX = when (slot) {
        MarkerSlot.TOP_RIGHT, MarkerSlot.BOTTOM_RIGHT -> mapWidthPx - margin
        else -> margin
    }
    val cornerY = when (slot) {
        MarkerSlot.BOTTOM_RIGHT, MarkerSlot.BOTTOM_LEFT -> mapHeightPx - margin
        else -> margin
    }

    // 2. 코스 경계선 (Padding 포함)
    val padding = markerSizePx * 0.9f
    val boundLeft = (minOf(courseMinX, courseMaxX) - padding).coerceAtLeast(margin)
    val boundRight = (maxOf(courseMinX, courseMaxX) + padding).coerceAtMost(mapWidthPx - margin)
    val boundTop = (minOf(courseMinY, courseMaxY) - padding).coerceAtLeast(margin)
    val boundBottom = (maxOf(courseMinY, courseMaxY) + padding).coerceAtMost(mapHeightPx - margin)

    // 3. 방향 벡터 및 거리 계산
    val dx = cornerX - actualX
    val dy = cornerY - actualY
    val totalDist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

    if (totalDist < 1f) return Offset(actualX, actualY)

    // 4. 경계선에 부딪히는 비율(t) 계산
    var t = 1.0f
    if (dx > 0) t = minOf(t, ((boundRight - actualX) / dx).coerceAtLeast(0f))
    else if (dx < 0) t = minOf(t, ((boundLeft - actualX) / dx).coerceAtLeast(0f))

    if (dy > 0) t = minOf(t, ((boundBottom - actualY) / dy).coerceAtLeast(0f))
    else if (dy < 0) t = minOf(t, ((boundTop - actualY) / dy).coerceAtLeast(0f))

    // [핵심 수정] 5. 최소 거리(Min Distance) 확보 로직
    // 사진 중앙이 멈출 '최소 거리' = 마커 반지름(0.5) + 원하는 선 길이(0.25) = 마커 크기의 0.75배
    val minAllowedDist = markerSizePx * 0.75f

    // 현재 t까지 갔을 때의 거리
    val currentDist = totalDist * t

    // 만약 계산된 위치가 너무 실제 좌표와 가깝다면, 최소 거리만큼 밀어냄
    val finalDist = if (currentDist < minAllowedDist) {
        minOf(minAllowedDist, totalDist) // 구석보다 더 멀리 갈 수는 없으므로 totalDist로 제한
    } else {
        currentDist
    }

    // 6. 최종 좌표 계산 (단위 벡터 사용)
    val finalX = actualX + (dx / totalDist) * finalDist
    val finalY = actualY + (dy / totalDist) * finalDist

    return Offset(finalX, finalY)
}



