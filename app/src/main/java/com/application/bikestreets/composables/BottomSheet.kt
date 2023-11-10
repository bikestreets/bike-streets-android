package com.application.bikestreets.composables

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

enum class ExpandedType {
    EXPANDED, COLLAPSED
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    sheetContent: @Composable () -> Unit

) {

    var expandedType by remember {
        mutableStateOf(ExpandedType.COLLAPSED)
    }
    val height by animateIntAsState(
        when (expandedType) {
            ExpandedType.EXPANDED -> 300
            ExpandedType.COLLAPSED -> 120
        }, label = "Drawer Animation"
    )

    @OptIn(ExperimentalMaterialApi::class)
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
    )

    BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        sheetElevation = 8.dp,
        sheetShape = RoundedCornerShape(
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
            topStart = 12.dp,
            topEnd = 12.dp
        ),
        sheetContent = {
            var isUpdated = false
            // Parent box handles swipes
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (!isUpdated) {
                                    expandedType = when {
                                        dragAmount < 0 && expandedType == ExpandedType.COLLAPSED -> {
                                            ExpandedType.EXPANDED
                                        }

                                        dragAmount > 0 && expandedType == ExpandedType.EXPANDED -> {
                                            ExpandedType.COLLAPSED
                                        }

                                        else -> {
                                            ExpandedType.EXPANDED
                                        }
                                    }
                                    isUpdated = true
                                }
                            },
                            onDragEnd = {
                                isUpdated = false
                            }
                        )
                    }
                    .background(Color.White)
            ) {
                sheetContent()
            }
        },
        sheetPeekHeight = height.dp,
        backgroundColor = Color.Transparent
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart) // Aligns the child Box to the bottom end of the parent Box
                    .size(100.dp) // Sets the size of the child Box
                    .offset(y = -height.dp)
                    .background(Color.Cyan)
            )
        }

    }
}