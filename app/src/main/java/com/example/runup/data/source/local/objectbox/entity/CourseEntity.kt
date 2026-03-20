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
    var firebaseId: String? = null, // Firestore 문서 ID

    // 💡 기존의 Course 객체를 JSON 문자열로 변환해서 저장할 공간
    var courseDataJson: String? = null,

    var embeddingText: String? = null,

    var address: String? = null,
    var landmark: String? = null,

    // 벡터 검색용 (384 차원)
    @HnswIndex(dimensions = 384)
    var vector: FloatArray? = null
)