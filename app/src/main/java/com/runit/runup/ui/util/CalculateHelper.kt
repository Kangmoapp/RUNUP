package com.runit.runup.ui.util

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.runit.runup.domain.model.MarkerSlot
import com.runit.runup.domain.model.PostImage
import com.google.firebase.firestore.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// 두 좌표의 거리 계산 (지구 반지름 기반)
fun calculateDistance(p1: GeoPoint, p2: GeoPoint): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(p2.latitude - p1.latitude)
    val dLon = Math.toRadians(p2.longitude - p1.longitude)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) * sin(dLon / 2).pow(2)
    return 2 * atan2(sqrt(a), sqrt(1 - a)) * r
}

fun calculateCalories(totalDistanceMeter: Double, weightKg: Float = 70f): String {
    // 1. 미터를 킬로미터로 변환
    val distanceKm = totalDistanceMeter / 1000.0

    // 2. 보편적인 러닝 칼로리 계수(1.036) 사용
    val calories = distanceKm * weightKg * 1.036

    // 3. 소수점 없이 정수로 반환
    return "${calories.toInt()}kcal"
}

fun calculatePace(ms: Int, distanceMeters: Double): String {
    if (distanceMeters <= 0) return "0'00\""
    val distanceKm = distanceMeters / 1000.0
    val totalSeconds = ms / 1000
    val secondsPerKm = (totalSeconds / distanceKm).toInt()
    val minutes = secondsPerKm / 60
    val seconds = secondsPerKm % 60
    return String.format("%d'%02d\"", minutes, seconds)
}

fun latLngToPixel(
    lat: Double,
    lng: Double,
    centerLat: Double,
    centerLng: Double,
    zoom: Double,
    mapWidth: Float,
    mapHeight: Float
): Offset {

    val scale = 2.0 // Static Map에서 scale=2 썼으니까 반드시 맞춰야 함
    val tileSize = 256.0 * scale
    val worldSize = tileSize * 2.0.pow(zoom)

    fun mercatorX(lng: Double): Double {
        return (lng + 180.0) / 360.0
    }

    fun mercatorY(lat: Double): Double {
        val sinLat = sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
        return 0.5 - ln((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI)
    }

    val centerX = mercatorX(centerLng) * worldSize
    val centerY = mercatorY(centerLat) * worldSize

    val targetX = mercatorX(lng) * worldSize
    val targetY = mercatorY(lat) * worldSize

    val dx = (targetX - centerX).toFloat()
    val dy = (targetY - centerY).toFloat()

    return Offset(
        x = mapWidth / 2f + dx,
        y = mapHeight / 2f + dy
    )
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
    val TAG = "OFFSET_FIX"

    // 1. 출발점: 구석 (Corner)
    val margin = markerSizePx * 0.8f
    val cornerX = if (slot == MarkerSlot.TOP_RIGHT || slot == MarkerSlot.BOTTOM_RIGHT) mapWidthPx - margin else margin
    val cornerY = if (slot == MarkerSlot.BOTTOM_RIGHT || slot == MarkerSlot.BOTTOM_LEFT) mapHeightPx - margin else margin

    // 2. 방향 벡터: 구석(Corner) -> 실제 좌표(Actual)
    val dx = actualX - cornerX
    val dy = actualY - cornerY
    val totalDist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

    if (totalDist < 1f) return Offset(cornerX, cornerY)

    // 3. 코스 경계선 (벽)
    val padding = markerSizePx * 0.7f
    val boundLeft = (minOf(courseMinX, courseMaxX) - padding).coerceAtLeast(margin)
    val boundRight = (maxOf(courseMinX, courseMaxX) + padding).coerceAtMost(mapWidthPx - margin)
    val boundTop = (minOf(courseMinY, courseMaxY) - padding).coerceAtLeast(margin)
    val boundBottom = (maxOf(courseMinY, courseMaxY) + padding).coerceAtMost(mapHeightPx - margin)

    // 4. t 계산 (구석에서 실제 좌표로 얼마나 갈 수 있는가)
    var t = 1.0f

    if (dx > 0) {
        if (cornerX < boundLeft) {
            val tx = (boundLeft - cornerX) / dx
            t = minOf(t, tx.coerceAtLeast(0f))
        }
    } else if (dx < 0) {
        if (cornerX > boundRight) {
            val tx = (boundRight - cornerX) / dx
            t = minOf(t, tx.coerceAtLeast(0f))
        }
    }

    if (dy > 0) {
        if (cornerY < boundTop) {
            val ty = (boundTop - cornerY) / dy
            t = minOf(t, ty.coerceAtLeast(0f))
        }
    } else if (dy < 0) {
        if (cornerY > boundBottom) {
            val ty = (boundBottom - cornerY) / dy
            t = minOf(t, ty.coerceAtLeast(0f))
        }
    }

    // 5. 최종 거리 결정
    val finalDist = if (t == 1.0f) {
        val minGap = markerSizePx * 0.8f
        (totalDist - minGap)
    } else {
        totalDist * t
    }

    // 6. 결과 산출 및 지도 밖 이탈 방지 (추가됨)
    var finalX = cornerX + (dx / totalDist) * finalDist
    var finalY = cornerY + (dy / totalDist) * finalDist

    // 마커가 지도 픽셀 밖으로 나가지 않도록 제한 (마커 절반 크기만큼 여유)
    val markerMargin = markerSizePx *2 / 3f
    finalX = finalX.coerceIn(markerMargin, mapWidthPx - markerMargin)
    finalY = finalY.coerceIn(markerMargin, mapHeightPx - markerMargin)

    Log.d(TAG, "Corner:($cornerX,$cornerY) -> Final:($finalX,$finalY) t=$t")
    return Offset(finalX, finalY)
}