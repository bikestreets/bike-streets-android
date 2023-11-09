import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MyBottomSheetWithFloatingElements() {
    val bottomSheetScaffoldState =
        rememberBottomSheetScaffoldState(bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed))

    // This state is used to track the size of the bottom sheet
    var bottomSheetSize by remember { mutableStateOf(IntSize.Zero) }

    // A state to hold our offset value
    var offset by remember { mutableStateOf(0f) }

    BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        sheetContent = {
            // This is the content of your bottom sheet.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .onGloballyPositioned { layoutCoordinates ->
                        // Update the size of the bottom sheet when the layout is positioned.
                        bottomSheetSize = layoutCoordinates.size
                        offset = -bottomSheetSize.height.toFloat()
                    }
            ) {
                Text(text = "Bottom Sheet Content", Modifier.padding(16.dp))
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Floating element
            val floatingElementModifier =
                if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
                    Modifier.offset(y = offset.dp)
                } else Modifier

            FloatingElement(floatingElementModifier)

            Column {
                Text("${offset.dp}")
                Text("${bottomSheetSize.height}")
            }

        }
    }

    // Recalculate the offset when the bottom sheet's offset changes
    LaunchedEffect(bottomSheetScaffoldState.bottomSheetState) {
        snapshotFlow { bottomSheetScaffoldState.bottomSheetState.requireOffset() }
            .collect { pxOffset ->

                // LocalDensity provides the density of the screen, which you'll use to convert px to dp
                val density = LocalDensity

                // Using density to convert the screen height pixels to dp
                return@collect with(density) { pxOffset.dp }
                // Convert pixels to dp
            }
    }
}

@Composable
fun FloatingElement(modifier: Modifier) {
    // This is a placeholder for whatever content you want to float above the bottom sheet
    Box(
        modifier
            .background(Color.Red)
            .padding(64.dp)
    ) {
        Text(text = "Floating Element")
    }
}