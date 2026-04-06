package com.example.runup.ui.util.mapper

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeMapper {
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA)
        return sdf.format(date)
    }
}