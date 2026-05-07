package com.runit.runup.ui.navigation

enum class CourseProgress {
    NONE,           // 코스 없음 혹은 진행 중
    REACHED_END,    // 편도 완주
    RETURNING,      // 왕복 중 반환점 통과 (회항 중)
    BACK_AT_START   // 왕복 완주 (시작점으로 복귀)
}