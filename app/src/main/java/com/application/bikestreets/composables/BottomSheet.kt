package com.application.bikestreets.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.application.bikestreets.R
import com.application.bikestreets.bottomsheet.BottomSheetContentState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    bottomSheetScaffoldState: BottomSheetScaffoldState,
    sheetContent: @Composable () -> Unit,
    actionButtons: @Composable () -> Unit,
    bottomSheetContentState: BottomSheetContentState,
    content: @Composable () -> Unit,
) {

    @Composable
    fun updateBottomSheetPeekHeight(): Dp {
        // Set the peek height to only show search bar
        val dimenPeekIndicator =
            dimensionResource(R.dimen.draggable_indicator_height) + dimensionResource(R.dimen.draggable_indicator_top_margin)
        val dimenClose =
            dimensionResource(R.dimen.close_icon_height) + dimensionResource(R.dimen.close_padding) * 2
        val dimenSearchEntry =
            dimensionResource(R.dimen.search_icon_height) + dimensionResource(R.dimen.edit_text_padding) * 2 +
                    dimensionResource(R.dimen.toolbar_vertical_margin)


        return when (bottomSheetContentState) {
            BottomSheetContentState.INITIAL -> (dimenPeekIndicator + dimenClose + dimenSearchEntry)
            BottomSheetContentState.DIRECTIONS -> (dimenPeekIndicator + dimenClose + dimenSearchEntry * 2)
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