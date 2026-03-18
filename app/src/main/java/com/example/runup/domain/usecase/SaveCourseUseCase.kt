package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.example.runup.domain.model.Node
import com.example.runup.domain.model.Scores
import com.example.runup.domain.repository.CourseRepository // 해당 인터페이스가 있다고 가정
import javax.inject.Inject

class SaveCourseUseCase @Inject constructor(
    private val courseRepository: CourseRepository
) {
    // 인자에 totalDistance를 추가로 받습니다.
    suspend operator fun invoke(
        recordedNodes: List<Node>,
        totalDistance: Int // 리포지토리에서 넘어온 누적 거리
    ): AuthResult<Boolean> {
        if (recordedNodes.isEmpty()) {
            return AuthResult.Fail("기록된 위치 정보가 없습니다.")
        }

        val newCourse = Course(
            id = "",
            distance = totalDistance, // 이미 계산된 값을 그대로 사용
            locationPoints = recordedNodes,
            minLat = recordedNodes.minOf { it.locationPoint.latitude },
            maxLat = recordedNodes.maxOf { it.locationPoint.latitude },
            minLng = recordedNodes.minOf { it.locationPoint.longitude },
            maxLng = recordedNodes.maxOf { it.locationPoint.longitude },
            scores = Scores(0.0, 0.0, 0.0)
        )

        return courseRepository.saveCourse(newCourse)
    }
}