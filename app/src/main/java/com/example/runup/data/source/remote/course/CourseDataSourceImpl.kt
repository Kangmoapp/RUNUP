package com.example.runup.data.source.remote.course

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class CourseDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CourseDataSource {
    private val eps = 0.0003
    private val margin = eps
    private val minSamples = 2 // 최소 점 개수

    // #1. [코스 병합 및 저장하는 함수]
    override suspend fun saveCourse(course: Course): AuthResult<Boolean> {
        return try {
            mergeAndSaveCourse(course)
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("러닝 기록 저장 실패", e)
        }
    }

    private suspend fun mergeAndSaveCourse(newCourse: Course){
        // 1. Bounding Box 계산 및 1차/2차 필터링 (이전 로직 동일)
        val newMinLat = newCourse.locationPoints.minOf { it.latitude }
        val newMaxLat = newCourse.locationPoints.maxOf { it.latitude }
        val newMinLng = newCourse.locationPoints.minOf { it.longitude }
        val newMaxLng = newCourse.locationPoints.maxOf { it.longitude }

        //위도로 1차필터링
        val candidates = firestore.collection("Course")
            .whereGreaterThanOrEqualTo("maxLat", newMinLat - margin)
            .whereLessThanOrEqualTo("minLat", newMaxLat + margin)
            .get().await().toObjects(Course::class.java)

        //경도로 2차필터링
        val targetClusters = candidates.filter { existing ->
            existing.maxLng >= (newMinLng - margin) && existing.minLng <= (newMaxLng + margin)
        }

        // 2. [DBSCAN 핵심] 모든 좌표를 하나로 모아 클러스터링 준비
        val allPoints = mutableListOf<GeoPoint>().apply {
            addAll(newCourse.locationPoints)
            targetClusters.forEach { addAll(it.locationPoints) }
        }

        // 3. Kotlin 버전 DBSCAN 실행
        val clusters = performDBSCAN(allPoints, eps, minSamples)

        // 4. Firestore 업데이트 (클러스터 단위로 문서 생성/삭제)
        firestore.runTransaction { transaction ->
            // 1. 카운터 문서 참조 및 현재 번호 읽기
            val metaRef = firestore.collection("Metadata").document("courseInfo")
            val metaSnap = transaction.get(metaRef)

            // 문서가 없으면 0부터 시작
            var lastNum = if (metaSnap.exists()) metaSnap.getLong("lastCourseNumber") ?: 0L else 0L

            // 2. 기존 병합 대상 문서 삭제
            targetClusters.forEach { oldCourse ->
                transaction.delete(firestore.collection("Course").document(oldCourse.id))
            }

            // 3. DBSCAN 결과로 나온 각 그룹을 새 번호로 저장
            clusters.forEach { clusterPoints ->
                if (clusterPoints.isNotEmpty()) {
                    lastNum++ // 번호 증가
                    val customId = "course$lastNum" // 예: course3

                    val simplifiedPoints = gridSimplify(clusterPoints, 0.00005)
                    val newDocRef = firestore.collection("Course").document(customId)

                    // finalCourse 객체 생성 시 id도 customId로 전달
                    val finalCourse = createCourseFromPoints(customId, simplifiedPoints)
                    transaction.set(newDocRef, finalCourse)
                }
            }

            // 4. 업데이트된 마지막 번호를 다시 저장
            transaction.set(metaRef, mapOf("lastCourseNumber" to lastNum))
        }.await()
    }

    private fun performDBSCAN(points: List<GeoPoint>, eps: Double, minSamples: Int): List<List<GeoPoint>> {
        val visited = mutableSetOf<Int>()
        val clusters = mutableListOf<List<GeoPoint>>()
        val noise = mutableSetOf<Int>()

        for (i in points.indices) {
            if (i in visited) continue //방문한 좌표이면 스킵,
            visited.add(i) //방문하지 않은 좌표면 방문한 좌표에 추가

            val neighbors = findNeighbors(i, points, eps)
            if (neighbors.size < minSamples) { //이웃이 두개가 안된다면(자기 자신) noise(혼자 있는 그룹) 에 추가
                noise.add(i)
            } else {
                val cluster = mutableListOf<Int>() //하나의 그룹에 들어가는 인덱스들 모음 리스트
                expandCluster(i, neighbors, points, cluster, visited, eps, minSamples)
                clusters.add(cluster.map { points[it] })
            }
        }
        return clusters
    }

    private fun expandCluster(root: Int, neighbors: List<Int>, points: List<GeoPoint>, cluster: MutableList<Int>, visited: MutableSet<Int>, eps: Double, minSamples: Int) {
        cluster.add(root)
        val queue = neighbors.toMutableList()
        var idx = 0
        while (idx < queue.size) {
            val nextPointIdx = queue[idx++]
            if (nextPointIdx !in visited) { // 방문한 좌표 아니라면
                visited.add(nextPointIdx) //방문좌표에 더해주고
                val nextNeighbors = findNeighbors(nextPointIdx, points, eps)
                if (nextNeighbors.size >= minSamples) {
                    queue.addAll(nextNeighbors.filter { it !in queue })
                }
            }
            if (cluster.none { it == nextPointIdx }) {
                cluster.add(nextPointIdx)
            }
        }
    }

    private fun findNeighbors(index: Int, points: List<GeoPoint>, eps: Double): List<Int> {
        val neighbors = mutableListOf<Int>() //index 좌표 주변에 있는 이웃좌표 목록
        val p1 = points[index] //현재좌표
        for (i in points.indices) {
            val p2 = points[i]
            val dist = Math.sqrt(Math.pow(p1.latitude - p2.latitude, 2.0) + Math.pow(p1.longitude - p2.longitude, 2.0))
            if (dist <= eps) neighbors.add(i) //거리가 eps 이내면 이웃에 추가
        }
        return neighbors
    }

    private fun gridSimplify(points: List<GeoPoint>, gridSize: Double): List<GeoPoint> {
        return points.groupBy {
            val latGrid = (it.latitude / gridSize).roundToInt()
            val lngGrid = (it.longitude / gridSize).roundToInt()
            Pair(latGrid, lngGrid)
        }.map { (_, group) ->
            GeoPoint(
                group.map { it.latitude }.average(),
                group.map { it.longitude }.average()
            )
        }
    }

    private fun createCourseFromPoints(id: String, points: List<GeoPoint>): Course {
        if (points.isEmpty()) return Course(id = id, locationPoints = emptyList()) // 예외 처리

        // 1. [순서 정렬] 가장 가까운 점을 찾아가며 경로 순서 재구성 (Greedy 정렬)
        val sortedPoints = mutableListOf<GeoPoint>()
        val remaining = points.toMutableList()
        var current = remaining.removeAt(0)
        sortedPoints.add(current)

        var totalDistance = 0.0
        val radius = 6371000.0 // 지구 반지름

        while (remaining.isNotEmpty()) {
            // 현재 점(current)에서 가장 가까운 다음 점 찾기
            val next = remaining.minByOrNull { p ->
                val dLat = Math.toRadians(p.latitude - current.latitude)
                val dLon = Math.toRadians(p.longitude - current.longitude)
                // 성능을 위해 여기서는 단순 피타고라스 근사치로 비교 (정렬용)
                Math.pow(dLat, 2.0) + Math.pow(dLon, 2.0)
            }!!

            // [거리 계산] 찾은 '다음 점'과의 하버사인 거리 계산 (실제 거리용)
            val lat1 = Math.toRadians(current.latitude)
            val lat2 = Math.toRadians(next.latitude)
            val dLat = Math.toRadians(next.latitude - current.latitude)
            val dLon = Math.toRadians(next.longitude - current.longitude) // 여기서 한 번에 계산

            val a = Math.sin(dLat / 2).let { it * it } +
                    Math.cos(lat1) * Math.cos(lat2) *
                    Math.sin(dLon / 2).let { it * it }

            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            totalDistance += radius * c

            // 다음 스텝 준비
            remaining.remove(next)
            sortedPoints.add(next)
            current = next
        }

        // 3. 최종 객체 반환
        return Course(
            id = id,
            locationPoints = sortedPoints, // 정렬된 좌표 저장
            minLat = sortedPoints.minOf { it.latitude },
            maxLat = sortedPoints.maxOf { it.latitude },
            minLng = sortedPoints.minOf { it.longitude },
            maxLng = sortedPoints.maxOf { it.longitude },
            distance = Math.round(totalDistance).toInt()
        )
    }

    // #2. [거리에 맞게 가까운 코스 가져오는 함수]
    override suspend fun getNearCourse(
        courseDistance: Int,
        currentLocation: GeoPoint
    ): AuthResult<List<Pair<Int, List<GeoPoint>>>> {
        return try {
            // 가장 가까운 시작점 찾기
            val nearestData = findNearestStartPoint(currentLocation)
                ?: return AuthResult.Fail("주변에 이용 가능한 코스가 없습니다.")

            val (courseDocument, startPoint) = nearestData
            val allPaths = mutableListOf<Pair<Double, List<GeoPoint>>>()
            val visited = mutableSetOf<Pair<Double, Double>>()
            visited.add(startPoint.latitude to startPoint.longitude)

            // DFS 경로 탐색 시작
            searchRecursive(
                currentPath = mutableListOf(startPoint),
                currentDist = 0.0,
                targetDist = courseDistance.toDouble(),
                pointsPool = courseDocument.locationPoints,
                visited = visited,
                results = allPaths
            )

            if (allPaths.isEmpty()) { // 탐색되어 나온 적절한 코스가 없는 경우
                AuthResult.Fail("조건에 맞는 코스를 생성할 수 없습니다.")
            } else {
                // Double 거리를 Int로 변환하여 리스트 생성
                val recommendedResult = allPaths.map { (dist, path) ->
                    dist.toInt() to path
                }

                // 모든 코스 파이어베이스 'test' 컬렉션에 저장
                recommendedResult.forEach { (distance, path) ->
                    saveToTestCollection(distance, path)
                }

                // 탐색된 모든 코스 리스트를 Success에 담아 반환 (최대 5개)
                return AuthResult.Success(recommendedResult)
            }
        } catch (e: Exception) {
            AuthResult.Fail("코스 탐색 중 오류 발생: ${e.message}")
        }
    }

    // 사용자 위치와 가장 가까운 시작점 찾기
    private suspend fun findNearestStartPoint(userLoc: GeoPoint): Pair<Course, GeoPoint>? {
        var radius = 0.0005 // 50m
        while (radius <= 0.005) { // 최대 500m까지 확인
            val docs = firestore.collection("Course")
                .whereGreaterThanOrEqualTo("maxLat", userLoc.latitude - radius)
                .whereLessThanOrEqualTo("minLat", userLoc.latitude + radius)
                .get().await()

            val candidates = docs.toObjects(Course::class.java).filter {
                it.maxLng >= userLoc.longitude - radius && it.minLng <= userLoc.longitude + radius
            }
            // 사용자 위치 기준으로 radius 정사각형 범위안에 코스의 부분이 조금이라도 겹치면 candidates에 넣음

            if (candidates.isNotEmpty()) {
                var minDistance = Double.MAX_VALUE
                var bestStartPoint: Pair<Course, GeoPoint>? = null
                candidates.forEach { candidate ->
                    candidate.locationPoints.forEach { point ->
                        val distance = calculateDistance(userLoc, point)
                        if (distance < minDistance) { minDistance = distance; bestStartPoint = candidate to point }
                    }
                }
                if (bestStartPoint != null) return bestStartPoint
            }
            radius += 0.0005 // 범위 안에 들어온 코스가 없었을 시 50m 늘려서 재탐색
        }
        return null
    }

    // --- 내부 알고리즘 함수 ---

    private fun searchRecursive(
        currentPath: MutableList<GeoPoint>, // 현재 위치
        currentDist: Double, // 현재까지의 누적 거리
        targetDist: Double, // 사용자가 원하는 거리
        pointsPool: List<GeoPoint>, // 해당 코스에 포함되는 모든 좌표
        visited: MutableSet<Pair<Double, Double>>, // 방문한 좌표
        results: MutableList<Pair<Double, List<GeoPoint>>> // Double(거리) 와 좌표 목록 을 리스트로 담음
    ) {
        if (currentDist >= targetDist) {
            // 도달 시점의 누적 거리(currentDist)를 경로와 함께 저장
            results.add(currentDist to currentPath.toList())
            return
        }

        val lastPt = currentPath.last() // 현재 위치
        val parentPt = if (currentPath.size >= 2) currentPath[currentPath.size - 2] else null // 부모 위치

        // 주변 이웃 찾기
        val allNeighbors = pointsPool.filter { pt ->
            val key = pt.latitude to pt.longitude
            if (key in visited) return@filter false
            val d = calculateDistance(lastPt, pt)
            d in 1.0..7.9 // 1미터에서 7.9미터 사이의 이웃 좌표들 탐색
        }.map { it to calculateDistance(lastPt, it) }

        if (allNeighbors.isEmpty()) {
            if (currentDist > targetDist * 0.95) { // 더 이상 주변 좌표가 없을 경우, 현재까지의 거리가 목표거리의 95퍼센트가 넘으면 코스로 인정
                results.add(currentDist to currentPath.toList())
            }
            return
        }

        // 필터링 및 정렬
        val scoredNeighbors = mutableListOf<Triple<GeoPoint, Double, Int>>()
        if (allNeighbors.size == 1) { // 이웃이 한개밖에 없으면 우선순위 1
            scoredNeighbors.add(Triple(allNeighbors[0].first, allNeighbors[0].second, 0)) // 좌표, 거리, 우선순위
        } else { // 이웃 좌표가 2개 이상 있는 경우
            for ((pt, d) in allNeighbors) {
                val angleToNext = if (parentPt != null) calculateAngleDiff(parentPt, lastPt, pt) else 0.0
                var isValid = false
                var priority = 1

                if (angleToNext < 20.0) { //부모 좌표와 나의 좌표, 나의 좌표와 다음 이웃 좌표의 각도차이가 20도 미만(거의 직선) 이라면
                    isValid = true
                    priority = 0 // 우선순위 0
                } else { // 20도를 벗어나는 경우에는 그 다음 좌표까지 확인
                    val hasContinuingPath = pointsPool.any { nNext ->
                        val nNextKey = nNext.latitude to nNext.longitude
                        if (nNextKey in visited || nNextKey == (lastPt.latitude to lastPt.longitude)) false
                        else {
                            val dNext = calculateDistance(pt, nNext)
                            dNext in 1.0..7.9 && calculateAngleDiff(lastPt, pt, nNext) < 20.0
                        }
                    }
                    if (hasContinuingPath) {
                        isValid = true
                        priority = 1 // 직선 이웃좌표보다는 우선순위 낮게 설정
                    }
                }
                if (isValid) scoredNeighbors.add(Triple(pt, d, priority))
            }
        }
        // 우선순위 먼저 확인 후, 거리 짧은 순 정렬
        val sortedNeighbors = scoredNeighbors.sortedWith(compareBy({ it.third }, { it.second }))

        // 탐색 진행
        for ((nextPt, d, _) in sortedNeighbors) {
            if (results.size >= 5) break // 코스를 5개 이상 찾으면 종료
            visited.add(nextPt.latitude to nextPt.longitude)
            currentPath.add(nextPt)
            // 누적 거리(currentDist + d)를 넘겨줌
            searchRecursive(currentPath, currentDist + d, targetDist, pointsPool, visited, results)
            currentPath.removeAt(currentPath.size - 1)
        }
    }

    // --- 유틸리티 함수 ---
    // 파이어 베이스 테스트콜렉션에 저장
    private suspend fun saveToTestCollection(distance: Int, path: List<GeoPoint>) {
        val testData = hashMapOf(
            "distance" to distance,
            "locationPoints" to path,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        firestore.collection("test").add(testData).await()
    }
    // 두 좌표의 거리 계산 (지구 반지름 기반)
    private fun calculateDistance(p1: GeoPoint, p2: GeoPoint): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) * sin(dLon / 2).pow(2)
        return 2 * atan2(sqrt(a), sqrt(1 - a)) * r
    }
    // 3좌표의 각도 차이 계산
    private fun calculateAngleDiff(p1: GeoPoint, p2: GeoPoint, p3: GeoPoint): Double {
        val v1 = Pair(p1.latitude - p2.latitude, p1.longitude - p2.longitude)
        val v2 = Pair(p3.latitude - p2.latitude, p3.longitude - p2.longitude)
        val dot = v1.first * v2.first + v1.second * v2.second
        val mag1 = sqrt(v1.first.pow(2) + v1.second.pow(2))
        val mag2 = sqrt(v2.first.pow(2) + v2.second.pow(2))
        if (mag1 == 0.0 || mag2 == 0.0) return 180.0
        return 180.0 - Math.toDegrees(acos((dot / (mag1 * mag2)).coerceIn(-1.0, 1.0)))
    }
}