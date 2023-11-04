package com.application.bikestreets.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.maps.MapView
import com.mapbox.maps.Style

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetUi() {
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
    )

    val peekHeight = 70.dp


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
            SheetContent()
        },
        // This is the height in collapsed state
        sheetPeekHeight = peekHeight
    ) {
        MainContent(bottomSheetScaffoldState.bottomSheetState, peekHeight)
    }
}

@Composable
private fun SheetContent() {

    Column(modifier = Modifier.heightIn(min = 100.dp, max = 500.dp)) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Places to Visit",
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = Color(0xffFDD835),
            fontSize = 24.sp,
            modifier = Modifier.padding(8.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(20) { place ->
                Text("$place")
            }
        }
    }
}


@ExperimentalMaterialApi
@Composable
private fun MainContent(bottomSheetState: BottomSheetState, peekheight: Dp) {


    val offset = bottomSheetState.requireOffset()

    val progress = bottomSheetState.progress

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xff6D4C41))
            .padding(top = 30.dp)
    ) {
        Text(
            color = Color.White,
            text =
            "isExpanded: ${bottomSheetState.isExpanded}\n" +
                    "isCollapsed: ${bottomSheetState.isCollapsed}\n"
        )

        Text(
            color = Color.White,
            text =
            "offset: $offset"
        )
        Text(
            color = Color.White,
            text =
            "offset dp: $offset.dp"
        )
        Text(
            color = Color.White,
            text =
            "bottom sheet peek hieght dp: $peekheight"
        )
        Text(
            color = Color.White,
            text =
            "peek height minus offset: ${offset.dp - peekheight}"
        )

        Text(
            color = Color.White,
            text = "progress: $progress\n"

        )
        Box(
            modifier = Modifier
                .size(50.dp, 50.dp)
                .absoluteOffset(20.dp, offset.dp - 70.dp)
                .background(Color.Red)
        )

        MapboxMapViewComposable()
    }
}


@Composable
fun MapboxMapViewComposable() {

    // Use AndroidView to place the MapView in your composable layout
    AndroidView(
        factory = { context ->
            MapView(context).also { mapView ->
                mapView.getMapboxMap().loadStyleUri(Style.TRAFFIC_DAY)
                // Initialization code if necessary, e.g., setting up listeners, starting loading, etc.
            }
        },
        update = { mapView ->
            // Here, you can update the MapView when the composable recomposes.
            // For instance, you can set the camera position, update the map style, etc.
        }
    )
}
