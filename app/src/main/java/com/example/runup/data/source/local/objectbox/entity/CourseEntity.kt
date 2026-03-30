package com.example.runup.data.source.local.objectbox.entity

import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique

@Entity
data class CourseEntity(
    @Id var id: Long = 0,

    @Unique(onConflict = ConflictStrategy.REPLACE)
    var firebaseId: String? = null,

    // Course의 핵심 필드들을 밖으로 꺼냅니다 (검색 가능하게!)
    var distance: Int = 0,
    var minLat: Double = 0.0,
    var maxLat: Double = 0.0,
    var minLng: Double = 0.0,
    var maxLng: Double = 0.0,

    // 복잡한 객체(Scores)나 리스트(Node)만 JSON으로 유지
    var locationPointsJson: String? = null,
    var scoresJson: String? = null,

    // AI/검색용 필드
    var embeddingText: String? = null,
    var address: String? = null,
    var landmark: String? = null,

    @HnswIndex(dimensions = 384)
    var vector: FloatArray? = null
)