package com.example.runup.data.source.remote.course

import android.util.Log
import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.example.runup.domain.model.CoursePathGroup
import com.example.runup.domain.model.Node
import com.example.runup.domain.model.Path
import com.example.runup.domain.model.SortType
import com.example.runup.service.GeminiHelper
import com.example.runup.ui.util.calculateDistance
import com.example.runup.domain.model.GeoPoint
import io.objectbox.Box
import javax.inject.Inject
import kotlin.math.*

class CourseDataSourceImpl @Inject constructor(
    private val apiService: CourseApiService,
    private val courseBox: Box<CourseEntity>,
    private val geminiHelper: GeminiHelper
) : CourseDataSource {

    // ---------------------------------------------------------------------------------------------
    // #1. 코스 저장
    // ---------------------------------------------------------------------------------------------
    override suspend fun saveCourse(course: Course): AuthResult<Boolean> {
        return try {
            val response = apiService.saveCourse(course)
            if (response.isSuccessful && response.body() == true) {
                AuthResult.Success(true)
            } else {
                AuthResult.Fail("서버에 코스를 저장하지 못했습니다.")
            }
        } catch (e: Exception) {
            AuthResult.Fail("코스 저장 중 통신 오류 발생", e)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // #2. 일반 조건 기반 코스 탐색 (DFS 사용)
    // ---------------------------------------------------------------------------------------------
    override suspend fun getCourse(
        courseDistance: Int,
        currentLocation: GeoPoint,
        isLoop: Boolean,
        sortType: SortType
    ): AuthResult<List<CoursePathGroup>> {
        return try {
            // 1. 내 주변 코스 가져오기 (반경 5km로 넉넉하게)
            val response = apiService.getNearbyCourses(
                lat = currentLocation.latitude,
                lng = currentLocation.longitude,
                radius = 5000.0,
                sortType = sortType.name
            )

            val sourceCourses = response.body() ?: emptyList()
            if (sourceCourses.isEmpty()) return AuthResult.Fail("주변에 이용 가능한 코스가 없습니다.")

            // 2. 추천 사유(Reason) 및 시작점 매핑
            val sourcesWithReason = sourceCourses.map { course ->
                val reason = when (sortType) {
                    SortType.DISTANCE -> "현재 위치에서 가장 가까운 추천 코스입니다."
                    SortType.BRIGHT -> "야간에도 밝고 안전한 코스입니다."
                    SortType.PEOPLE -> "사람들이 많이 찾는 활기찬 코스입니다."
                    SortType.DIFFICULTY -> "운동 효과가 좋은 난이도 있는 코스입니다."
                }
                val startPoint = course.locationPoints
                    .minByOrNull { calculateDistance(currentLocation, it.locationPoint) }
                    ?.locationPoint ?: currentLocation

                Triple(course, startPoint, reason)
            }

            // 3. DFS 알고리즘을 통한 경로 생성
            val targetDist = if (isLoop) courseDistance / 2.0 else courseDistance.toDouble()
            val recommendedResult = generatePathsFromSources(currentLocation, sourcesWithReason, targetDist)

            if (recommendedResult.isEmpty()) {
                AuthResult.Fail("근처에 조건에 맞는 코스가 없습니다. (알고리즘 필터링 됨)")
            } else {
                AuthResult.Success(recommendedResult)
            }
        } catch (e: Exception) {
            AuthResult.Fail("코스 탐색 중 오류 발생: ${e.message}")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // #3. AI 기반 코스 탐색 (DFS 사용)
    // ---------------------------------------------------------------------------------------------
    override suspend fun getCourseFromAI(
        courseDistance: Int,
        currentLocation: GeoPoint,
        currentAddress: String,
        isLoop: Boolean,
        userPrompt: String
    ): AuthResult<List<CoursePathGroup>> {
        return try {
            val request = mapOf(
                "prompt" to userPrompt,
                "address" to currentAddress,
                "lat" to currentLocation.latitude,
                "lng" to currentLocation.longitude
            )
            val response = apiService.getCoursesFromAI(request)
            val aiData = response.body() ?: emptyList()

            // AiCourseResponse 객체를 이용해 안전하게 매핑
            val sources = aiData.mapNotNull { data ->
                val course = data.course
                val reason = data.reason

                val startPoint = course.locationPoints
                    .minByOrNull { calculateDistance(currentLocation, it.locationPoint) }
                    ?.locationPoint ?: currentLocation

                Triple(course, startPoint, reason)
            }

            if (sources.isEmpty()) return AuthResult.Fail("AI가 적절한 코스를 찾지 못했습니다.")

            val targetDist = if (isLoop) courseDistance / 2.0 else courseDistance.toDouble()
            val recommendedResult = generatePathsFromSources(currentLocation, sources, targetDist)

            if (recommendedResult.isEmpty()) return AuthResult.Fail("AI 코스를 찾았으나, 경로 알고리즘이 생성에 실패했습니다.")

            AuthResult.Success(recommendedResult)
        } catch (e: Exception) {
            AuthResult.Fail("AI 코스 탐색 오류: ${e.message}")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // [핵심 알고리즘] DFS 경로 생성 엔진 (민우님 오리지널 코드 기반)
    // ---------------------------------------------------------------------------------------------
    private suspend fun generatePathsFromSources(
        currentLocation: GeoPoint,
        sources: List<Triple<Course, GeoPoint, String>>,
        targetDist: Double
    ): List<CoursePathGroup> {
        val allGroupsResult = mutableListOf<CoursePathGroup>()

        sources.forEachIndexed { index, (course, originalStartPoint, reason) ->
            val courseResults = mutableListOf<Pair<Double, List<GeoPoint>>>()

            // 🌟 [핵심 1] 시작점의 함정 돌파!
            // 단 하나의 시작점만 믿지 말고, 코스 내에서 유저와 가까운 '시작점 후보 3개'를 뽑아서 다 시도해봅니다!
            val topStartPoints = course.locationPoints
                .sortedBy { calculateDistance(currentLocation, it.locationPoint) }
                .take(3)
                .map { it.locationPoint }

            for (startPoint in topStartPoints) {
                if (courseResults.size >= 3) break // 한 코스에서 3갈래 찾았으면 다음 코스로 패스

                val visited = mutableSetOf<Pair<Double, Double>>()
                visited.add(startPoint.latitude to startPoint.longitude)

                searchRecursive(
                    currentPath = mutableListOf(startPoint),
                    currentDist = 0.0,
                    targetDist = targetDist,
                    pointsPool = course.locationPoints,
                    visited = visited,
                    results = courseResults,
                    maxPerSource = 3
                )
            }

            // 시작점이 달라서 생긴 중복된 경로들 정리
            val uniqueResults = courseResults.distinctBy { it.second.last().latitude }

            if (uniqueResults.isNotEmpty()) {
                val subGroup = uniqueResults.mapIndexed { pathIndex, (dist, path) ->
                    val distanceInt = dist.toInt()
                    Path(distance = distanceInt, points = path, centerPoint = calculateCenterPoint(path, currentLocation))
                }
                allGroupsResult.add(CoursePathGroup(course, reason, subGroup))
            }
        }
        return allGroupsResult
    }

    private fun searchRecursive(
        currentPath: MutableList<GeoPoint>,
        currentDist: Double,
        targetDist: Double,
        pointsPool: List<Node>,
        visited: MutableSet<Pair<Double, Double>>,
        results: MutableList<Pair<Double, List<GeoPoint>>>,
        maxPerSource: Int
    ) {
        if (results.size >= maxPerSource) return

        if (currentDist >= targetDist) {
            results.add(currentDist to currentPath.toList())
            return
        }

        val lastPt = currentPath.last()
        val parentPt = if (currentPath.size >= 2) currentPath[currentPath.size - 2] else null

        val allNeighbors = pointsPool.filter { pt ->
            val key = pt.locationPoint.latitude to pt.locationPoint.longitude
            if (key in visited) return@filter false
            val d = calculateDistance(lastPt, pt.locationPoint)
            d in 1.0..15.0 // 점 간격 15m 이내 허용
        }.map { it to calculateDistance(lastPt, it.locationPoint) }

        if (allNeighbors.isEmpty()) {
            // 🌟 [핵심 2] 95% 깐깐한 조건 폐지!
            // 1. 코스가 짧아서 끊겼지만 최소 50m 이상의 의미 있는 경로가 생성된 경우
            // 2. 목표 거리의 70% 이상 도달한 경우
            // 이 중 하나라도 만족하면 훌륭한 슬라이싱 코스로 인정합니다!
            if (currentDist > targetDist * 0.7 || currentDist > 50.0) {
                results.add(currentDist to currentPath.toList())
            }
            return
        }

        // --- (아래의 후보군 탐색, 각도 계산, 정렬 로직은 민우님 원본과 100% 동일하게 유지합니다) ---
        data class NeighborCandidate(val point: GeoPoint, val distance: Double, val priority: Int, val angle: Double)
        val candidates = mutableListOf<NeighborCandidate>()

        if (allNeighbors.size == 1) {
            candidates.add(NeighborCandidate(allNeighbors[0].first.locationPoint, allNeighbors[0].second, 0, 0.0))
        } else {
            for ((pt, d) in allNeighbors) {
                val angleToNext = if (parentPt != null) calculateAngleDiff(parentPt, lastPt, pt.locationPoint) else 0.0
                var isValid = false
                var priority = 1

                if (angleToNext < 20.0) {
                    isValid = true
                    priority = 0
                } else {
                    val hasContinuingPath = pointsPool.any { nNext ->
                        val nNextKey = nNext.locationPoint.latitude to nNext.locationPoint.longitude
                        if (nNextKey in visited || nNextKey == (lastPt.latitude to lastPt.longitude)) false
                        else {
                            val dNext = calculateDistance(pt.locationPoint, nNext.locationPoint)
                            dNext in 1.0..7.9 && calculateAngleDiff(lastPt, pt.locationPoint, nNext.locationPoint) < 30.0
                        }
                    }
                    if (hasContinuingPath) {
                        isValid = true
                        priority = 1
                    }
                }
                if (isValid) candidates.add(NeighborCandidate(pt.locationPoint, d, priority, angleToNext))
            }
        }

        val filteredNeighbors = mutableListOf<NeighborCandidate>()
        candidates.sortBy { it.distance }

        for (candidate in candidates) {
            val isDuplicateDirection = filteredNeighbors.any { existing ->
                Math.abs(existing.angle - candidate.angle) <= 10.0
            }
            if (!isDuplicateDirection) filteredNeighbors.add(candidate)
        }

        val sortedNeighbors = filteredNeighbors.sortedWith(compareBy({ it.priority }, { it.distance }))

        for (neighbor in sortedNeighbors) {
            if (results.size >= maxPerSource) break
            val nextPt = neighbor.point
            val d = neighbor.distance

            visited.add(nextPt.latitude to nextPt.longitude)
            currentPath.add(nextPt)
            searchRecursive(currentPath, currentDist + d, targetDist, pointsPool, visited, results, maxPerSource)
            currentPath.removeAt(currentPath.size - 1)
            visited.remove(nextPt.latitude to nextPt.longitude)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 유틸리티 함수
    // ---------------------------------------------------------------------------------------------
    private fun calculateAngleDiff(p1: GeoPoint, p2: GeoPoint, p3: GeoPoint): Double {
        val v1 = Pair(p1.latitude - p2.latitude, p1.longitude - p2.longitude)
        val v2 = Pair(p3.latitude - p2.latitude, p3.longitude - p2.longitude)
        val dot = v1.first * v2.first + v1.second * v2.second
        val mag1 = sqrt(v1.first.pow(2) + v1.second.pow(2))
        val mag2 = sqrt(v2.first.pow(2) + v2.second.pow(2))
        if (mag1 == 0.0 || mag2 == 0.0) return 180.0
        return 180.0 - Math.toDegrees(acos((dot / (mag1 * mag2)).coerceIn(-1.0, 1.0)))
    }

    private fun calculateCenterPoint(path: List<GeoPoint>, currentLocation: GeoPoint): GeoPoint {
        if (path.isEmpty()) return currentLocation
        val avgLat = path.map { it.latitude }.average()
        val avgLng = path.map { it.longitude }.average()
        return GeoPoint((avgLat + currentLocation.latitude) / 2.0, (avgLng + currentLocation.longitude) / 2.0)
    }
}