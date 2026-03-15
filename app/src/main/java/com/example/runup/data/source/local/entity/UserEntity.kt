package com.example.runup.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey val id: Int = 0, // 사용자 정보는 하나만 저장하므로 ID 고정
    val goalDistance: Int,      // 목표 거리
    val goalTime: Int,           // 목표 시간
)