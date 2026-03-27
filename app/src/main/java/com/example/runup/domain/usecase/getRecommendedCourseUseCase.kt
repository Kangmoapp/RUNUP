package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.CourseRecommendation
import com.example.runup.domain.model.SortType
import com.example.runup.domain.repository.CourseRepository
import com.google.firebase.firestore.GeoPoint
import javax.inject.Inject

class GetRecommendedCourseUseCase @Inject constructor(
    private val courseRepository: CourseRepository
) {
    // sortType 별로 가져오기
    suspend operator fun invoke(
        courseDistance: Int,
        currentLocation: GeoPoint,
        isLoop: Boolean,
        sortType: SortType,
        count: Int
    ): AuthResult<List<CourseRecommendation>> { // 반환 타입 변경
        val result = courseRepository.getCourse(courseDistance, currentLocation, isLoop, sortType)

        return when (result) {
            is AuthResult.Success -> {
                // 1. 모든 그룹의 경로들을 평평하게(Flatten) 펼침
                val flattenedList = result.data.flatMap { group ->
                    group.generatedPaths.map { path ->
                        CourseRecommendation(
                            originCourse = group.originCourse,
                            reason = group.reason,
                            path = path
                        )
                    }
                }

                // 2. 요청한 개수(count)만큼만 잘라서 반환
                AuthResult.Success(flattenedList.take(count))
            }
            is AuthResult.Fail -> {
                // AuthResult의 제네릭 타입이 바뀌었으므로 새로 생성해서 반환
                AuthResult.Fail(result.message)
            }
        }
    }

    //AI 추천으로 가져오기
    suspend operator fun invoke(
        courseDistance: Int,
        currentLocation: GeoPoint,
        isLoop: Boolean,
        userPrompt : String,
        count: Int
    ): AuthResult<List<CourseRecommendation>> { // 반환 타입 변경
        val result = courseRepository.getCourseFromAI(courseDistance, currentLocation, isLoop, userPrompt)

        return when (result) {
            is AuthResult.Success -> {
                // 1. 모든 그룹의 경로들을 평평하게(Flatten) 펼침
                val flattenedList = result.data.flatMap { group ->
                    group.generatedPaths.map { path ->
                        CourseRecommendation(
                            originCourse = group.originCourse,
                            reason = group.reason,
                            path = path
                        )
                    }
                }

                // 2. 요청한 개수(count)만큼만 잘라서 반환
                AuthResult.Success(flattenedList.take(count))
            }
            is AuthResult.Fail -> {
                // AuthResult의 제네릭 타입이 바뀌었으므로 새로 생성해서 반환
                AuthResult.Fail(result.message)
            }
        }
    }
}