package com.application.bikestreets.composables.dialogs

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun WelcomeDialog(onShareLocationClicked: () -> Unit) {
    BikeStreetsDialog(
        onCloseClicked = { onShareLocationClicked() },
        dialogContent = { Text("Get low-stress bike routes to any destination in Denver. Share your location so we can help you plan your route.") },
        confirmationText = "Share Location"
    )
}

@Preview
@Composable
fun WelcomeDialogPreview() {
    BikeStreetsDialog(
        onCloseClicked = { },
        dialogContent = { Text("Get low-stress bike routes to any destination in Denver. Share your location so we can help you plan your route.") },
        confirmationText = "Share Location"
    )
}
