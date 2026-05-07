package com.runit.runup.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun Modifier.MapSize() =
    this
        .fillMaxWidth()
        .height(450.dp)

fun Modifier.MapSpaceSize() =
    this
        .height(90.dp)