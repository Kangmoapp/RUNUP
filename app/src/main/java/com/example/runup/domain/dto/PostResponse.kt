package com.example.runup.domain.dto

import com.example.runup.domain.model.* // Post, PostImage, RunRecord, Course 등을 가져옴
import java.text.SimpleDateFormat
import java.util.Locale

// 1. 서버가 보내주는 응답 데이터 (서버 DTO와 100% 일치)
data class PostResponse(
    val postId: String,
    val userUid: String,
    val userName: String,
    val userProfileUrl: String?,
    val content: String,
    val locationImages: List<PostImageDto> = emptyList(), // 누락되었던 클래스!
    val commonImages: List<String> = emptyList(),
    val runRecord: RunRecordResponse? = null,             // 누락되었던 클래스!
    val city: String = "",
    val district: String = "",
    val dong: String = "",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: String = ""
)

// 2. 사진 위치 데이터 (추가)
data class PostImageDto(
    val url: String,
    val lat: Double? = null,
    val lng: Double? = null
)

// 3. 러닝 기록 & 좌표 데이터 (추가)
data class RunRecordResponse(
    val recordId: Long,
    val distance: Int,
    val time: Int,
    val recordDate: Long,
    val nodes: List<RemoteNode>
)

// 4. 지도 위도/경도 데이터 (추가)
data class RemoteNode(
    val lat: Double,
    val lng: Double
)

// ---------------------------------------------------------
// 🌟 서버 가방 -> 안드로이드 화면용(Domain) 가방으로 바꾸는 마법의 함수들

fun PostResponse.toDomain(): Post {
    // 실제 폰 연결 시 본인 컴퓨터의 IP로 꼭 변경하세요! (예: 192.168.x.x)
    val BASE_URL = "http://192.168.4.6:8080"

    fun makeFullUrl(url: String): String {
        return if (url.startsWith("http")) url else BASE_URL + url
    }

    // 🌟 1. 러닝 기록을 먼저 변환합니다.
    val domainRunRecord = this.runRecord?.toDomain()

    // 🌟 2. 사진이 바다(0,0)로 가는 걸 막기 위해, 코스의 '첫 번째 좌표'를 구합니다.
    val fallbackLat = domainRunRecord?.course?.locationPoints?.firstOrNull()?.locationPoint?.latitude ?: 35.8714
    val fallbackLng = domainRunRecord?.course?.locationPoints?.firstOrNull()?.locationPoint?.longitude ?: 128.6014

    return Post(
        postId = this.postId,
        authorId = this.userUid,
        authorName = this.userName,
        authorProfileUrl = this.userProfileUrl?.let { makeFullUrl(it) } ?: "",
        content = this.content,

        // 🌟 3. 위치 이미지를 코스 위로 구출합니다!
        locationImages = this.locationImages.map {
            // 🚨 핵심 포인트: it.lat == 0.0 조건이 추가되었습니다!
            val finalLat = if (it.lat == null || it.lat == 0.0) fallbackLat else it.lat
            val finalLng = if (it.lng == null || it.lng == 0.0) fallbackLng else it.lng

            PostImage(
                url = makeFullUrl(it.url),
                thumbnailUrl = makeFullUrl(it.url),
                location = GeoPoint(finalLat, finalLng) // 이제 무조건 한국 땅에 박힙니다!
            )
        },
        commonImages = this.commonImages.map {
            PostImage(url = makeFullUrl(it), thumbnailUrl = makeFullUrl(it))
        },
        runRecord = domainRunRecord,
        city = this.city,
        district = this.district,
        dong = this.dong,
        likes = this.likeCount,
        commentCount = this.commentCount,
        isLiked = this.isLiked,
        timestamp = System.currentTimeMillis()
    )
}

// 🌟 서버의 좌표 데이터(RemoteNode)를 안드로이드가 쓰는 Course 형태로 변환!
// 🌟 서버의 좌표 데이터(RemoteNode)를 안드로이드가 쓰는 Course 형태로 변환!
// 🌟 서버의 좌표 데이터를 안드로이드가 쓰는 RunRecord 형태로 변환!
fun RunRecordResponse.toDomain(): RunRecord {
    // 1. 좌표 리스트 변환
    val mappedNodes = this.nodes.map { node ->
        Node(
            locationPoint = GeoPoint(node.lat, node.lng),
            stop = false
        )
    }

    // 2. 최대/최소 위경도 계산
    val minLat = if (this.nodes.isEmpty()) 0.0 else this.nodes.minOf { it.lat }
    val maxLat = if (this.nodes.isEmpty()) 0.0 else this.nodes.maxOf { it.lat }
    val minLng = if (this.nodes.isEmpty()) 0.0 else this.nodes.minOf { it.lng }
    val maxLng = if (this.nodes.isEmpty()) 0.0 else this.nodes.maxOf { it.lng }

    return RunRecord(
        // 🚨 안드로이드 RunRecord 모델에는 id가 없으므로 해당 줄을 삭제했습니다!
        recordDate = this.recordDate,
        time = this.time,

        course = Course(
            id = this.recordId.toString(), // Course에는 id가 있으므로 여기에만 넣습니다
            distance = this.distance,
            locationPoints = mappedNodes,
            minLat = minLat,
            maxLat = maxLat,
            minLng = minLng,
            maxLng = maxLng
        )
    )
}

data class CommentResponseDto(
    val commentId: String,
    val postId: String,
    val userUid: String,
    val userName: String,
    val userProfileUrl: String?,
    val content: String,
    val createdAt: String // 서버에서 주는 문자열 시간
) {
    fun toDomain(): Comment {
        val BASE_URL = "http://192.168.4.6:8080"// 🚨 본인 IP로 변경

        // "2026-05-03T12:54:05" 형태의 글자를 숫자(Long)로 번역합니다.
        val timestamp = try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            // 소수점(밀리초)이 붙어올 수 있으므로 제거 후 변환
            format.parse(this.createdAt.substringBefore("."))?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis() // 에러 나면 현재 시간으로 대체
        }

        return Comment(
            commentId = this.commentId,
            authorId = this.userUid,
            authorName = this.userName,
            authorProfileUrlMini = this.userProfileUrl?.let { if (it.startsWith("http")) it else BASE_URL + it } ?: "",
            content = this.content,
            timestamp = timestamp // 🌟 이제 정확한 시간(방금 전, 1분 전 등)이 뜹니다!
        )
    }
}