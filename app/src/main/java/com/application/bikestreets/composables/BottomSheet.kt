package com.application.bikestreets.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    bottomSheetScaffoldState: BottomSheetScaffoldState,
    sheetContent: @Composable () -> Unit,
    actionButtons: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {

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
        sheetPeekHeight = 120.dp,
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
            ) {
                actionButtons()
            }
        }
    }
}