package com.example.runup.domain.model
// 🌟 서버의 RunRecordResponse와 형식을 완벽히 맞춰야 합니다.
data class RunRecordResponse(
    val recordId: Long,
    val recordDate: Long,
    val time: Int,
    val distance: Int,
    val courseName: String,
    val nodes: List<RemoteNode> // 🌟 지도 좌표 리스트
)
// RunRecordResponse.kt

// RunRecordResponse.kt (안드로이드)

fun RunRecordResponse.toDomain(): RunRecord {
    return RunRecord(
        recordDate = this.recordDate,
        time = this.time,
        course = Course(
            id = this.recordId.toString(),
            distance = this.distance,
            landmark = this.courseName,
            // 🌟 이 부분이 핵심입니다!
            // 서버의 nodes(List<RemoteNode>)를 앱의 locationPoints(List<Node>)로 변환합니다.
            locationPoints = this.nodes.map { remoteNode ->
                Node(
                    locationPoint = GeoPoint(remoteNode.lat, remoteNode.lng)
                )
            }
        )
    )
}
data class RemoteNode(
    val lat: Double,
    val lng: Double
)