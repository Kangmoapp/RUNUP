package com.example.runup.domain.model

enum class SortType(val label: String) {
    DISTANCE("거리순"),
    BRIGHT("밝기순"),
    PEOPLE("유동인구순"),
    DIFFICULTY("난이도순")
}

enum class RunFilter(val label: String) {
    TODAY("오늘"), WEEK("이번 주"), MONTH("이번 달"), ALL("전체")
}