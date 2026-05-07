package com.runit.runup.domain.usecase

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.model.CoursePathGroup
import com.runit.runup.domain.model.CourseRecommendation
import com.runit.runup.domain.model.SortDirection
import com.runit.runup.domain.model.SortType
import com.runit.runup.domain.repository.CourseRepository
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
        count: Int,
        maxSearchDistance: Int, // 📍 추가
        sortDirection: SortDirection // 📍 추가
    ): AuthResult<List<CourseRecommendation>> {
        val result = courseRepository.getCourse(courseDistance, currentLocation, isLoop, sortType, maxSearchDistance, sortDirection)

        return when (result) {
            is AuthResult.Success -> AuthResult.Success(distributeCourses(result.data, count))
            is AuthResult.Fail -> AuthResult.Fail(result.message)
        }
    }

    // #2. AI 추천 호출
    suspend operator fun invoke(
        courseDistance: Int,
        currentLocation: GeoPoint,
        currentAddress: String,
        isLoop: Boolean,
        userPrompt: String,
        count: Int,
        maxSearchDistance: Int
    ): AuthResult<List<CourseRecommendation>> {
        val result = courseRepository.getCourseFromAI(courseDistance, currentLocation, currentAddress, isLoop, userPrompt, maxSearchDistance)

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

    private fun flattenCourses(
        groups: List<CoursePathGroup>,
        targetCount: Int
    ): List<CourseRecommendation> {
        return groups.flatMap { group ->
            // 각 그룹(originCourse)에서 생성된 갈래 중 '최대 2개'만 선택 📍
            group.generatedPaths.take(2).map { path ->
                CourseRecommendation(
                    originCourse = group.originCourse,
                    reason = group.reason,
                    path = path
                )
            }
        }.take(targetCount) // 전체 결과 중 최종적으로 필요한 개수(3개)만큼만 반환
    }
}