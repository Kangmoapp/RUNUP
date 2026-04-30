package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.CoursePathGroup
import com.example.runup.domain.model.CourseRecommendation
import com.example.runup.domain.model.SortType
import com.example.runup.domain.repository.CourseRepository
import com.google.firebase.firestore.GeoPoint
import javax.inject.Inject

class GetRecommendedCourseUseCase @Inject constructor(
    private val courseRepository: CourseRepository
) {
    // #1. 일반 추천 호출
    suspend operator fun invoke(
        courseDistance: Int,
        currentLocation: GeoPoint,
        isLoop: Boolean,
        sortType: SortType,
        count: Int
    ): AuthResult<List<CourseRecommendation>> {
        val result = courseRepository.getCourse(courseDistance, currentLocation, isLoop, sortType)

        return when (result) {
            is AuthResult.Success -> AuthResult.Success(distributeCourses(result.data, count))
            is AuthResult.Fail -> AuthResult.Fail(result.message)
        }
    }

    // #2. AI 추천 호출
    suspend operator fun invoke(
        courseDistance: Int,
        currentLocation: GeoPoint,
        isLoop: Boolean,
        userPrompt: String,
        count: Int
    ): AuthResult<List<CourseRecommendation>> {
        val result = courseRepository.getCourseFromAI(courseDistance, currentLocation, isLoop, userPrompt)

        return when (result) {
            is AuthResult.Success -> AuthResult.Success(distributeCourses(result.data, count))
            is AuthResult.Fail -> AuthResult.Fail(result.message)
        }
    }

    /**
     * ── 🔹 [핵심] 코스 분배 로직 (Round-Robin) ── 📍
     * 여러 그룹에서 번갈아가며 코스를 추출합니다.
     */
    private fun distributeCourses(
        groups: List<CoursePathGroup>,
        targetCount: Int
    ): List<CourseRecommendation> {
        val resultList = mutableListOf<CourseRecommendation>()
        if (groups.isEmpty()) return resultList

        // 가장 많이 가진 그룹의 갈래 수만큼 반복 루프를 돌립니다.
        val maxPaths = groups.maxOfOrNull { it.generatedPaths.size } ?: 0

        for (pathIdx in 0 until maxPaths) {
            for (group in groups) {
                // 현재 인덱스(0번, 1번...)에 해당하는 갈래가 있다면 결과 리스트에 추가
                if (pathIdx < group.generatedPaths.size) {
                    resultList.add(
                        CourseRecommendation(
                            originCourse = group.originCourse,
                            reason = group.reason,
                            path = group.generatedPaths[pathIdx]
                        )
                    )
                }

                // 목표 개수(count)를 다 채우면 즉시 반환
                if (resultList.size == targetCount) return resultList
            }
        }

        return resultList
    }
}