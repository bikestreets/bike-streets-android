package com.application.bikestreets.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.maps.MapView
import com.mapbox.maps.Style

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetTestUi() {

    var sheetHeight by remember { mutableStateOf(IntSize.Zero) }

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
            SheetContent(onHeightChange = { newSize ->
                sheetHeight = newSize
            })
        },
        // This is the height in collapsed state
        sheetPeekHeight = peekHeight,
        sheetBackgroundColor = Color.Transparent
    ) {
        MainContent(bottomSheetScaffoldState.bottomSheetState, peekHeight, sheetHeight)
    }
}

@Composable
fun SheetContent(onHeightChange: (IntSize) -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier
            .heightIn(min = 100.dp, max = 500.dp)
            .onGloballyPositioned { coordinates ->
                // This will be called every time the Column is laid out
                // and will update the size state
                val newSize = coordinates.size
                if (size != newSize) {
                    size = newSize
                    onHeightChange(newSize) // Notifying the change to the parent
                }
            }
    ) {
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
private fun MainContent(bottomSheetState: BottomSheetState, peekheight: Dp, sheetHeight: IntSize) {


    val offset = bottomSheetState.requireOffset()

    val progress = bottomSheetState.progress

//    Box(
//        modifier = Modifier
//            .size(50.dp, 50.dp)
//            .absoluteOffset(0.dp, 0.dp)
//            .background(Color.Red)
//    )

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
        Text(
            color = Color.White,
            text = "Sheet Height: $sheetHeight\n"

        )
        getScreenHeightDp()
        MapboxMapViewComposable()
    }
}

@Composable
fun getScreenHeightDp(): Dp {
    // Configuration gives you access to the screen dimensions, among other things
    val configuration = LocalConfiguration.current
    // LocalDensity provides the density of the screen, which you'll use to convert px to dp
    val density = LocalDensity.current

    // Using density to convert the screen height pixels to dp
    return with(density) { configuration.screenHeightDp.dp }
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
