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