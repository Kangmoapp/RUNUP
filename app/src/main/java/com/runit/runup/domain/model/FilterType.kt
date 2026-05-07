package com.runit.runup.domain.model

data class FilterState(
    val scope: ViewScope = ViewScope.ALL,
    val type: FilterType = FilterType.ALL,
    val city: String = "",
    val district: String = "",
    val dong: String = ""
)

enum class FilterType { ALL, MY_LOCATION, CUSTOM_LOCATION }
enum class ViewScope { ALL, FRIENDS, MINE }
