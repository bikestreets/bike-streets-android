package com.application.bikestreets.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomSheetState
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@ExperimentalMaterialApi
@Composable
fun SheetContent(bottomSheetState: BottomSheetState) {

    val offsetWrapper = remember { OffsetWrapper() }

    if (offsetWrapper.offset == 0f) {
        val offset = bottomSheetState.requireOffset()
        offsetWrapper.offset = offset
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .offset {
                    IntOffset(
                        0,
                        (-bottomSheetState.requireOffset() + offsetWrapper.offset).toInt()
                    )
                },
            onClick = { /*TODO*/ }) {
            Text("Button")
        }
    }
}

class OffsetWrapper(var offset: Float = 0f)
