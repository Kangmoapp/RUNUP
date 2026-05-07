package com.runit.runup.ui.util.mapper

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeMapper {
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA)
        return sdf.format(date)
    }

    fun formatTimeAgo(timestamp: Long): String { // 댓글 창 시간 표시
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30
        val years = days / 365

        return when {
            seconds < 60 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            hours < 24 -> "${hours}시간 전"
            days < 30 -> "${days}일 전"
            months < 12 -> "${months}달 전"
            else -> "${years}년 전"
        }
    }

    // ── 🔹 시간 포맷팅 헬퍼 (mm:ss) ── 📍
    fun formatDurationMmSs(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    // 시간 포맷팅 헬퍼 (ms -> 00:00:00)
    fun formatDuration(ms: Int): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // 목표 시간용 (seconds -> 00:00:00)
    fun formatSeconds(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }


}