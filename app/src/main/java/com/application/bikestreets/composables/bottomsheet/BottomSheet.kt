package com.application.bikestreets.composables.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.application.bikestreets.BottomSheetContentState
import com.application.bikestreets.theme.Dimens

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    bottomSheetScaffoldState: BottomSheetScaffoldState,
    sheetContent: @Composable () -> Unit,
    actionButtons: @Composable () -> Unit,
    bottomSheetContentState: BottomSheetContentState,
    content: @Composable () -> Unit,
) {
    /**
     * Peek height changes based on what content is being shown
     */
    @Composable
    fun updateBottomSheetPeekHeight(): Dp {

        val dimenPeekIndicator =
            Dimens.draggableIndicatorHeight + Dimens.draggableIndicatorTopMargin
        val dimenClose =
            Dimens.closeSheetBtnSize
        val dimenEditText =
            TextFieldDefaults.MinHeight + Dimens.editTextVerticalPadding * 2

        return when (bottomSheetContentState) {
            BottomSheetContentState.INITIAL -> (dimenPeekIndicator + dimenClose + dimenEditText)
            BottomSheetContentState.DIRECTIONS -> (dimenPeekIndicator + dimenClose + dimenEditText * 2)
        }
    }

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
            sheetContent()
        },
        sheetPeekHeight = updateBottomSheetPeekHeight(),
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
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .offset(y = 70.dp)
            ) {
                actionButtons()
            }
        }
    }
}