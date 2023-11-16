package com.application.bikestreets.composables.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.application.bikestreets.theme.Colors
import com.application.bikestreets.theme.Dimens

@Composable
@Preview(showBackground = true)
fun DragIndicator() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = Dimens.draggableIndicatorTopMargin)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(8.dp))
                .width(32.dp)
                .height(Dimens.draggableIndicatorHeight)
                .background(Colors.dragHandle)
        ) {}
    }
}