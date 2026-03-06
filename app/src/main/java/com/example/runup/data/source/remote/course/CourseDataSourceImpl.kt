package com.example.runup.data.source.remote.course

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.roundToInt

class CourseDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CourseDataSource {
    private val eps = 0.0003
    private val margin = eps
    private val minSamples = 2 // 최소 점 개수

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
}