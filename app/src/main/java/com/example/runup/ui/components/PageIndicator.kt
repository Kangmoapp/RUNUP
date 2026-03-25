package com.example.runup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.runup.ui.theme.DarkGray
import com.example.runup.ui.theme.Gray

@Composable
fun PageIndicator(
    pagerState: PagerState,
    pageCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->

            val color =
                if (pagerState.currentPage == index)
                    Gray
                else
                    DarkGray

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )

            if (index != pageCount - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}