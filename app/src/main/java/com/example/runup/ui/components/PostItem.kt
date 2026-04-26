package com.example.runup.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.runup.BuildConfig
import com.example.runup.domain.model.Post
import com.example.runup.ui.screens.buildNaverStaticMapUrl
import com.example.runup.ui.screens.drawMarker
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.ui.util.assignSlots
import com.example.runup.ui.util.calculateCloserOffset
import com.example.runup.ui.util.latLngToPixel
import com.example.runup.ui.util.mapper.TimeMapper.formatTimestamp
import com.example.runup.viewmodel.MapSnapshot
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import kotlin.collections.component1
import kotlin.collections.component2

@OptIn(ExperimentalNaverMapApi::class, ExperimentalFoundationApi::class)
@Composable
fun PostItem(
    post: Post,
    cachedSnapshot: MapSnapshot?,         // 👈 추가
    authorBitmap: Bitmap?,                // 👈 추가
    thumbnailCache: Map<String, Bitmap>,   // 👈 추가
    fullBitmapCache: Map<String, Bitmap>,  // 👈 추가
    onLikeClick: () -> Unit,
    onPostClick: () -> Unit,              // 👈 onClick을 더 명확하게
    onDeletePost: () -> Unit,             // 👈 삭제 콜백
    onImageClick: (String) -> Unit,
    onSaveSnapshot: (MapSnapshot) -> Unit, // 👈 계산된 지도 저장 콜백
    onProfileClick: (String) -> Unit
) {
    // 삭제 메뉴 상태 및 본인 확인
    var showMenu by remember { mutableStateOf(false) }
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val isMyPost = post.authorId == currentUserId

    // 페이저 상태 관리 (총 페이지 수 = 지도(1) + 일반 이미지 개수)
    val totalPages = 1 + post.commonImages.size
    val pagerState = rememberPagerState(pageCount = { totalPages })

    val density = LocalDensity.current
    val context = LocalContext.current

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
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                if (authorBitmap != null) {
                    Image(bitmap = authorBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text("👤", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            if (isMyPost) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                    ) {
                        Text("MY", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, "더보기", tint = WhiteTextColor.copy(alpha = 0.7f))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color(0xFF2C2C2C))) {
                            DropdownMenuItem(text = { Text("게시글 삭제", color = Color.Red) }, onClick = { onDeletePost(); showMenu = false })
                        }
                    }
                }
            }
        }

        // --- [2] 메인 콘텐츠 (페이저) ---
        Box(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)) {

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val actualWidthPx = with(LocalDensity.current) { maxWidth.toPx() } // 👈 실제 너비!
                val markerSizePx = with(LocalDensity.current) { 64.dp.toPx() }

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    // HorizontalPager의 page == 0 내부
                    if (page == 0) {
                        var isMapLoaded by remember(post.postId) { mutableStateOf(false) }
                        post.runRecord?.let { record ->
                            // 🔹 1. BoxWithConstraints 대신 화면의 가로 픽셀을 직접 가져옵니다.
                            val context = LocalContext.current
                            val density = LocalDensity.current
                            val screenWidthPx = remember { context.resources.displayMetrics.widthPixels.toFloat() }
                            val markerSizePx = with(density) { 64.dp.toPx() }

                            // 🔹 2. 일반 Box 사용 (성능 최적화)
                            Box(modifier = Modifier.fillMaxSize()) {

                                LaunchedEffect(post.postId, actualWidthPx) {
                                    if (cachedSnapshot == null) {
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

                                        // 윤석님이 성공하신 'apiW = 화면폭' 로직 그대로 유지
                                        val apiW = screenWidthPx.toInt().coerceAtMost(1024)
                                        val apiH = apiW

                                        val url = buildNaverStaticMapUrl(centerLat, centerLng, dynamicZoom.toInt(), actualWidthPx.toInt().coerceAtMost(1024), actualWidthPx.toInt().coerceAtMost(1024))

                                        // 좌표 계산도 screenWidthPx(이전의 mapWidthPx) 기준으로 1:1 유지
                                        val pMin = latLngToPixel(record.course.minLat, record.course.minLng, centerLat, centerLng, dynamicZoom, actualWidthPx, actualWidthPx)
                                        val pMax = latLngToPixel(record.course.maxLat, record.course.maxLng, centerLat, centerLng, dynamicZoom, actualWidthPx, actualWidthPx)
                                        val newBounds = Rect(minOf(pMin.x, pMax.x), minOf(pMin.y, pMax.y), maxOf(pMin.x, pMax.x), maxOf(pMin.y, pMax.y))

                                        val newPositions = post.locationImages.filter { it.location != null }.associate { img ->
                                            img.url to latLngToPixel(img.location!!.latitude, img.location!!.longitude, centerLat, centerLng, dynamicZoom, actualWidthPx, actualWidthPx)
                                        }

                                        val slots = assignSlots(post.locationImages.filter { it.location != null }, centerLat, centerLng)
                                        val newCloserOffsets = slots.entries.associate { (postImage, slot) ->
                                            val orig = newPositions[postImage.url]!!
                                            postImage.url to calculateCloserOffset(orig.x, orig.y, slot, actualWidthPx, actualWidthPx, newBounds.left, newBounds.right, newBounds.top, newBounds.bottom, markerSizePx)
                                        }

                                        onSaveSnapshot(MapSnapshot(url, newPositions, newCloserOffsets, newBounds))
                                    }
                                }

                                // --- 지도 및 오버레이 그리기 영역 (동일) ---
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(cachedSnapshot?.staticMapUrl ?: "")
                                            .addHeader("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_API_KEY)
                                            .addHeader("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_API_SECRET_KEY)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillBounds,
                                        onSuccess = { isMapLoaded = true }
                                    )

                                    if (isMapLoaded && cachedSnapshot != null) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            // 🔹 4. 그리기 로직에서도 mapWidthPx(화면 실제 크기)를 사용합니다.
                                            val centerLat =
                                                (record.course.minLat + record.course.maxLat) / 2
                                            val centerLng =
                                                (record.course.minLng + record.course.maxLng) / 2
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
                                            val points = record.course.locationPoints.map {
                                                latLngToPixel(
                                                    it.locationPoint.latitude,
                                                    it.locationPoint.longitude,
                                                    centerLat,
                                                    centerLng,
                                                    dynamicZoom,
                                                    actualWidthPx,
                                                    actualWidthPx
                                                )
                                            }

                                            val path = Path().apply {
                                                points.forEachIndexed { i, p ->
                                                    if (i == 0) moveTo(p.x, p.y)
                                                    else if (!record.course.locationPoints[i - 1].stop) lineTo(
                                                        p.x,
                                                        p.y
                                                    )
                                                    else moveTo(p.x, p.y)
                                                }
                                            }
                                            drawPath(
                                                path,
                                                Color.Black,
                                                style = Stroke(
                                                    14f,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                            drawPath(
                                                path,
                                                PointColor,
                                                style = Stroke(
                                                    8f,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )

                                            if (points.isNotEmpty()) {
                                                drawMarker(points.first(), Color(0xFF4CAF50), "START")
                                                drawMarker(points.last(), Color(0xFFF44336), "END")
                                            }

                                            cachedSnapshot.closerOffsets.forEach { (url, closerPos) ->
                                                val origPos = cachedSnapshot.markerPositions[url]
                                                    ?: return@forEach
                                                drawLine(Color.Black.copy(0.8f), origPos, closerPos, 2f)
                                                drawCircle(Color.Black, 5f, origPos)
                                            }
                                        }

                                        cachedSnapshot.closerOffsets.forEach { (url, closerPos) ->
                                            val bitmap = thumbnailCache[url] ?: return@forEach
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
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = PointColor,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // --- 일반 이미지 페이지 ---
                        val imageUrl = post.commonImages[page - 1].url
                        val bitmap = fullBitmapCache[imageUrl]
                        if (bitmap != null) {
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier
                                .fillMaxSize()
                                .clickable { onImageClick(imageUrl) }, contentScale = ContentScale.Crop)
                        }
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

        // 🔹 좋아요/댓글/주소 영역 (윤석님의 원본 레이아웃 복구)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 16.dp, top = 0.dp, bottom = 4.dp), // top을 0으로 해서 위로 밀착
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 좋아요 섹션 (배지 스타일)
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
                            .offset(x = (-4).dp, y = 2.dp) // 우상단 45도 위치
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. 댓글 섹션 (배지 스타일)
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 32.dp)
                    .clickable { onPostClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = "💬", fontSize = 18.sp)
                if (post.commentCount > 0) {
                    Text(
                        text = "${post.commentCount}",
                        color = WhiteTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 2.dp) // 우상단 45도 위치
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // 주소를 오른쪽으로 밀어냄

            // 3. 주소 표시
            if (post.dong.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = WhiteTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${post.city} ${post.district} ${post.dong}",
                        color = WhiteTextColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
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