package com.runit.runup.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.runit.runup.BuildConfig
import com.runit.runup.domain.model.Post
import com.runit.runup.ui.screens.buildNaverStaticMapUrl
import com.runit.runup.ui.screens.drawMarker
import com.runit.runup.ui.theme.PointColor
import com.runit.runup.ui.theme.WhiteTextColor
import com.runit.runup.ui.util.assignSlots
import com.runit.runup.ui.util.calculateCloserOffset
import com.runit.runup.ui.util.calculatePace
import com.runit.runup.ui.util.latLngToPixel
import com.runit.runup.ui.util.mapper.TimeMapper.formatDurationMmSs
import com.runit.runup.ui.util.mapper.TimeMapper.formatTimestamp
import com.runit.runup.viewmodel.MapSnapshot
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import kotlin.collections.component1
import kotlin.collections.component2

@OptIn(ExperimentalNaverMapApi::class, ExperimentalFoundationApi::class)
@Composable
fun PostItem(
    post: Post,
    maxWidthPx: Float,
    mapSnapShots : MapSnapshot?,         // 👈 추가
    authorProfileBitmap: Bitmap?,                // 👈 추가
    locationBitmaps: Map<String, Bitmap>,
    commonImageBitmaps: Map<String, Bitmap>,
    onLikeClick: () -> Unit,
    onFollowClick: () -> Unit,
    onCommentClick: () -> Unit,              // 👈 onClick을 더 명확하게
    onDeletePost: () -> Unit,             // 👈 삭제 콜백
    onImageClick: (String) -> Unit,
    onSaveMapSnapshot: (MapSnapshot) -> Unit, // 👈 계산된 지도 저장 콜백
    onProfileClick: (String) -> Unit
) {
    // 삭제 메뉴 상태 및 본인 확인
    var showMenu by remember { mutableStateOf(false) }
    var showFollowDialog by remember { mutableStateOf(false) } //따라 뛰기 다이얼로그
    var showDeleteDialog by remember { mutableStateOf(false) } //삭제 확인 다이얼로그

    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val isMyPost = post.authorId == myUid

    // 페이저 상태 관리 (총 페이지 수 = 지도(1) + 일반 이미지 개수)
    val totalPages = 1 + post.commonImages.size
    val pagerState = rememberPagerState(pageCount = { totalPages })

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "게시글 삭제",
                    color = WhiteTextColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "정말 이 게시글을 삭제하시겠어요?",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeletePost()           // 👈 실제 삭제 처리 로직 실행
                    }
                ) {
                    Text("삭제", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    // 따라뛰기 확인 다이얼로그
    if (showFollowDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showFollowDialog = false },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "함께 달릴까요? 🏃‍♂️",
                    color = WhiteTextColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "이 코스를 불러와서 러닝을 시작합니다.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onFollowClick() // 👈 확인 시 실행
                        showFollowDialog = false
                    }
                ) {
                    // 강조하고 싶은 버튼에 PointColor 적용
                    Text("시작", color = PointColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showFollowDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // --- [1] 유저 헤더 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable { onProfileClick(post.authorId) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2C)),
                contentAlignment = Alignment.Center
            ) {
                if (authorProfileBitmap != null) {
                    Image(
                        bitmap = authorProfileBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // ── 🔹 이모지 대신 Material Icon 적용 📍 ──
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "기본 프로필",
                            tint = Color.Gray,
                            // 18.sp 이모지 크기와 비슷하게 20.dp 내외로 설정했습니다.
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            // [디자인이 개선된 게시글 메뉴 영역]
            if (isMyPost) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 1. "MY" 배지 (기존보다 조금 더 정제된 골드톤)
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp), // 조금 더 각진 느낌으로 세련되게
                        border = BorderStroke(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
                    ) {
                        Text(
                            "MY",
                            color = Color(0xFFFFD700),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "더보기",
                                tint = WhiteTextColor.copy(alpha = 0.5f) // 너무 튀지 않게 조절
                            )
                        }

                        // ── 2. 세련된 드롭다운 메뉴 ── 📍
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .width(IntrinsicSize.Min) // 1. 내부 콘텐츠 길이에 딱 맞게 너비 조절 🔹
                                .background(Color(0xFF1E1E1E))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.height(36.dp), // 2. 높이를 줄여서 위아래 여백 압축 🔹
                                text = {
                                    Text(
                                        "게시글 삭제",
                                        color = Color(0xFFE57373),
                                        fontSize = 13.sp, // 폰트도 살짝 다듬기
                                        fontWeight = FontWeight.SemiBold,
                                        softWrap = false // 줄바꿈 방지
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFE57373),
                                        modifier = Modifier.size(16.dp) // 아이콘도 슬림하게
                                    )
                                },
                                // 3. 내부 기본 패딩을 최소화 (이게 핵심!) 📍
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- [2] 메인 콘텐츠 (페이저) ---
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->

                if (page == 0) {
                    MapSection(
                        post = post,
                        maxWidthPx = maxWidthPx, // 주입받은 너비 사용
                        mapSnapShots = mapSnapShots,
                        locationMarkerCache = locationBitmaps,
                        onSaveMapSnapshot = onSaveMapSnapshot,
                        onImageClick = onImageClick
                    )
                } else {
                    val imageUrl = post.commonImages[page - 1].url
                    val bitmap = commonImageBitmaps[imageUrl]
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier
                            .fillMaxSize()
                            .clickable { onImageClick(imageUrl) }, contentScale = ContentScale.Crop)
                    }
                }
            }


            // 페이지 인디케이터 (1/3)
            if (totalPages > 1) {
                Surface(color = Color.Black.copy(0.6f), shape = CircleShape, modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)) {
                    Text("${pagerState.currentPage + 1}/$totalPages", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        // --- [3] 하단 인디케이터 & 좋아요/댓글/주소 ---
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
            // 이미지가 한 장일 때 지도와 하단 아이콘 사이 간격을 최소화하기 위한 여백
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 16.dp, top = 0.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 좋아요 섹션
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 32.dp)
                    .clickable { onLikeClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (post.isLiked) Color.Red else WhiteTextColor,
                    modifier = Modifier.size(24.dp)
                )
                if (post.likes > 0) {
                    Text(
                        text = "${post.likes}",
                        color = WhiteTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. 댓글 섹션
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 32.dp)
                    .clickable { onCommentClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "댓글",
                    tint = WhiteTextColor, // 좋아요의 기본 색상과 통일
                    modifier = Modifier.size(24.dp) // 크기 24dp로 고정
                )
                if (post.commentCount > 0) {
                    Text(
                        text = "${post.commentCount}",
                        color = WhiteTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ── 🔹 따라뛰기(Follow) 섹션 수정 ── 📍
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 32.dp)
                    .clickable {
                        // 📍 조건문(if)을 제거하여 무조건 다이얼로그를 띄웁니다.
                        showFollowDialog = true
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Outlined.Route,
                    contentDescription = "따라뛰기",
                    tint = if (post.followedBy.contains(myUid)) PointColor else WhiteTextColor,
                    modifier = Modifier.size(24.dp)
                )
                if (post.followCount > 0) {
                    Text(
                        text = "${post.followCount}",
                        color = WhiteTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 2.dp)
                    )
                }
            }

            // ── 4. 공간 밀어내기 ──
            Spacer(modifier = Modifier.weight(1f)) // 왼쪽 버튼들과 오른쪽 주소 사이를 벌려줌 📍

            // 5. 주소 표시
            if (post.dong.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp) // 아이콘들과 시각적 높이 맞춤
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = WhiteTextColor.copy(alpha = 0.4f), // 조금 더 은은하게 조절
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${post.city} ${post.district} ${post.dong}",
                        color = WhiteTextColor.copy(alpha = 0.4f),
                        fontSize = 10.sp, // 주소는 정보를 방해하지 않게 살짝 작게 🔹
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }

        // --- [4] 본문 및 날짜 ---
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row {
                Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(post.content, color = WhiteTextColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(post.timestamp),
                color = WhiteTextColor.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MapSection(
    post: Post,
    maxWidthPx: Float,
    mapSnapShots: MapSnapshot?,
    locationMarkerCache: Map<String, Bitmap>,
    onSaveMapSnapshot: (MapSnapshot) -> Unit,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val markerSizePx = with(density) { 64.dp.toPx() }
    var isMapLoaded by remember(post.postId) { mutableStateOf(false) }

    val areLocationMarkersReady = remember(post.locationImages, locationMarkerCache) {
        // location이 있는 모든 이미지 URL이 캐시에 존재하는지 확인
        post.locationImages
            .filter { it.location != null }
            .all { locationMarkerCache.containsKey(it.url) }
    }

    val isEverythingReady = mapSnapShots != null && isMapLoaded && areLocationMarkersReady


    // ── 🔹 [계산 로직] Screen에서 받은 maxWidthPx를 기준으로 딱 한 번만 수행 ──
    LaunchedEffect(post.postId, maxWidthPx) {
        if (mapSnapShots == null && post.runRecord != null) {
            val record = post.runRecord
            val centerLat = (record.course.minLat + record.course.maxLat) / 2
            val centerLng = (record.course.minLng + record.course.maxLng) / 2

            val latDiff = record.course.maxLat - record.course.minLat
            val lngDiff = record.course.maxLng - record.course.minLng
            val maxDiff = maxOf(latDiff, lngDiff)
            val dynamicZoom = when {
                maxDiff > 0.04 -> 14.0
                maxDiff > 0.015 -> 15.0
                maxDiff > 0.005 -> 16.0
                maxDiff > 0.002 -> 17.0
                else -> 18.0
            }

            // 🔹 [핵심] 위치 정보와 중단 여부를 튜플로 묶어서 미리 계산 📍
            val computedPathPoints = record.course.locationPoints.map {
                val pixelPos = latLngToPixel(
                    it.locationPoint.latitude, it.locationPoint.longitude,
                    centerLat, centerLng, dynamicZoom, maxWidthPx, maxWidthPx
                )
                pixelPos to it.stop // Pair(Offset, Boolean)
            }

            val url = buildNaverStaticMapUrl(centerLat, centerLng, dynamicZoom.toInt(), maxWidthPx.toInt().coerceAtMost(1024), maxWidthPx.toInt().coerceAtMost(1024))

            // 좌표 계산도 actualWidthPx  기준으로 1:1 유지
            val pMin = latLngToPixel(record.course.minLat, record.course.minLng, centerLat, centerLng, dynamicZoom, maxWidthPx, maxWidthPx)
            val pMax = latLngToPixel(record.course.maxLat, record.course.maxLng, centerLat, centerLng, dynamicZoom, maxWidthPx, maxWidthPx)
            val newBounds = Rect(minOf(pMin.x, pMax.x), minOf(pMin.y, pMax.y), maxOf(pMin.x, pMax.x), maxOf(pMin.y, pMax.y))

            val newPositions = post.locationImages.filter { it.location != null }.associate { img ->
                img.url to latLngToPixel(img.location!!.latitude, img.location!!.longitude, centerLat, centerLng, dynamicZoom, maxWidthPx, maxWidthPx)
            }

            val slots = assignSlots(post.locationImages.filter { it.location != null }, centerLat, centerLng)
            val newCloserOffsets = slots.entries.associate { (postImage, slot) ->
                val orig = newPositions[postImage.url]!!
                postImage.url to calculateCloserOffset(orig.x, orig.y, slot, maxWidthPx, maxWidthPx, newBounds.left, newBounds.right, newBounds.top, newBounds.bottom, markerSizePx)
            }

            onSaveMapSnapshot(MapSnapshot(
                staticMapUrl = url,
                pathPoints = computedPathPoints, // 미리 계산된 값!
                startPoint = computedPathPoints.firstOrNull()?.first,
                endPoint = computedPathPoints.lastOrNull()?.first,
                markerPositions = newPositions,
                closerOffsets = newCloserOffsets,
                courseBounds = newBounds
            ))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        // ── 🔹 [추가] 지도가 준비되지 않았거나 로딩 중일 때 노란색 인디케이터 표시 📍 ──
        if (!isEverythingReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // PointColor가 노란색 계열이라면 이를 사용하여 인디케이터 표시
                androidx.compose.material3.CircularProgressIndicator(
                    color = PointColor,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        // Static Map 이미지
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mapSnapShots?.staticMapUrl ?: "")
                .addHeader("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_API_KEY)
                .addHeader("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_API_SECRET_KEY)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (isEverythingReady) 1f else 0f },
            contentScale = ContentScale.FillBounds,
            onSuccess = { isMapLoaded = true }
        )

        if (isEverythingReady) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val snapshot = mapSnapShots ?: return@Canvas

                // 코스 경로 그리기 (이미 계산된 pathPoints 사용) 📍
                if (snapshot.pathPoints.isNotEmpty()) {
                    val path = Path().apply {
                        snapshot.pathPoints.forEachIndexed { i, (currentPos, isStop) ->
                            if (i == 0) {
                                moveTo(currentPos.x, currentPos.y)
                            } else {
                                // 🔹 이전 노드의 stop 여부를 확인합니다.
                                val (prevPos, prevStop) = snapshot.pathPoints[i - 1]

                                if (prevStop) {
                                    // 이전 지점에서 끊겼다면 선을 긋지 않고 '점프' 📍
                                    moveTo(currentPos.x, currentPos.y)
                                } else {
                                    // 정상 연결 상태라면 시원하게 긋기!
                                    lineTo(currentPos.x, currentPos.y)
                                }
                            }
                        }
                    }
                    // 테두리 + 선 그리기
                    drawPath(path, Color.Black, style = Stroke(14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path, PointColor, style = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                // 2. 시작/종료 마커 (이미 계산된 값 사용) 📍
                snapshot.startPoint?.let { drawMarker(it, Color(0xFF4CAF50), "START") }
                snapshot.endPoint?.let { drawMarker(it, Color(0xFFF44336), "END") }

                // 3. 사진 마커 연결 선 그리기 📍
                snapshot.closerOffsets.forEach { (url, closerPos) ->
                    val origPos = snapshot.markerPositions[url] ?: return@forEach
                    drawLine(Color.Black.copy(0.8f), origPos, closerPos, 2f)
                    drawCircle(Color.Black, 5f, origPos)
                }
            }

            mapSnapShots.closerOffsets.forEach { (url, closerPos) ->
                val bitmap = locationMarkerCache[url] ?: return@forEach
                Box(modifier = Modifier
                    .offset {
                        IntOffset(
                            (closerPos.x - markerSizePx / 2).toInt(),
                            (closerPos.y - markerSizePx / 2).toInt()
                        )
                    }
                    .clickable { onImageClick(url) }) {
                    PhotoMarkerFromBitmap(bitmap)
                }
            }

            post.runRecord?.let { record ->
                // 잔상 없는 블랙 대시보드
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp)) // 1. 먼저 자르고
                        .background(Color(0xFF000000).copy(alpha = 0.75f)) // 2. 순수 블랙 85% (색 변형 방지) 🔹
                        .padding(horizontal = 18.dp, vertical = 10.dp), // 3. 내부 여백
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // [1] 대형 거리 표시
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = String.format("%.2f", record.course.distance / 1000.0),
                                color = PointColor,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = "km",
                                color = PointColor.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                            )
                        }

                        // [2] 하단 데이터 (페이스, 시간, 칼로리)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 📍 SmallStatItem 내부에 혹시 background가 있다면 꼭 제거하세요! 🔹
                            SmallStatItem("평균 페이스", calculatePace(record.time.toInt(), record.course.distance.toDouble()))
                            SmallStatItem("시간", formatDurationMmSs(record.time.toLong() / 1000))

                            val kcal = (record.course.distance / 1000.0 * 70).toInt()
                            SmallStatItem("칼로리", "$kcal")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Medium
        )
    }
}