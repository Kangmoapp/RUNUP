package com.example.runup.ui.util.mapper

import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.domain.model.Course
import com.example.runup.domain.model.Node
import com.example.runup.domain.model.Scores
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseMapper @Inject constructor(private val gson: Gson) {
    // 1. Firebase/UI 객체 -> DB 엔티티 (저장할 때)
    fun toEntity(course: Course, firebaseId: String, vector: FloatArray?, address: String?): CourseEntity {
        return CourseEntity(
            firebaseId = firebaseId,
            distance = course.distance,
            minLat = course.minLat,
            maxLat = course.maxLat,
            minLng = course.minLng,
            maxLng = course.maxLng,
            // 리스트와 복잡한 객체만 JSON으로 변환
            locationPointsJson = gson.toJson(course.locationPoints),
            scoresJson = gson.toJson(course.scores),
            vector = vector,
            address = address
        )
    }

    // 2. DB 엔티티 -> UI 객체 (불러올 때)
    fun toDomain(entity: CourseEntity): Course {
        val typeToken = object : TypeToken<List<Node>>() {}.type
        return Course(
            id = entity.firebaseId ?: "",
            distance = entity.distance,
            minLat = entity.minLat,
            maxLat = entity.maxLat,
            minLng = entity.minLng,
            maxLng = entity.maxLng,
            locationPoints = gson.fromJson(entity.locationPointsJson, typeToken) ?: listOf(),
            scores = gson.fromJson(entity.scoresJson, Scores::class.java) ?: Scores(0.0, 0.0, 0.0)
        )
    }
}