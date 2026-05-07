package com.runit.runup.ui.util.mapper

object DistanceMapper {
    fun formatDistance(distanceMeter: Double): String {
        return if (distanceMeter < 100.0) {
            "${distanceMeter.toInt()}m"
        } else {
            String.format("%.2fkm", distanceMeter / 1000.0)
        }
    }
}