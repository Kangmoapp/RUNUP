package com.example.runup.ui.util

import com.google.firebase.firestore.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
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