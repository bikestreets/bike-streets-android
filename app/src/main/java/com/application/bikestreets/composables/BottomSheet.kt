package com.application.bikestreets.composables

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Creating our own Expanded and Collapsed state,
 * this is done to track the height of bottom sheet.
 * Using the default scaffold state does not give us a way
 * to track the height, which is required to position our buttons.
 * Unfortunately this causes worse swiping behavior
 */
enum class ExpandedType {
    EXPANDED, COLLAPSED
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    expandedState: ExpandedType,
    onExpandedTypeChange: (ExpandedType) -> Unit,
    sheetContent: @Composable () -> Unit,
    actionButtons: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {

    val height by animateIntAsState(
        when (expandedState) {
            ExpandedType.EXPANDED -> 350
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
                                    when {
                                        dragAmount < 0 && expandedState == ExpandedType.COLLAPSED -> {
                                            onExpandedTypeChange(ExpandedType.EXPANDED)
                                        }

                                        dragAmount > 0 && expandedState == ExpandedType.EXPANDED -> {
                                            onExpandedTypeChange(ExpandedType.COLLAPSED)
                                        }

                                        else -> {
                                            onExpandedTypeChange(ExpandedType.EXPANDED)
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
            content()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .offset(y = -height.dp)
            ) {
                // Buttons that will appear anchored to the bottomsheet
                actionButtons()
            }
        }
    }
}