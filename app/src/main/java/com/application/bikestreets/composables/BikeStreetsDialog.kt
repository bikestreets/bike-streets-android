package com.application.bikestreets.composables

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable

@Composable
fun BikeStreetsDialog(
    onCloseClicked: () -> Unit,
    isDismissible: Boolean? = true
) {
    AlertDialog(
        title = {
            Text(text = "Settings")
        },
        onDismissRequest = {
            if (isDismissible == true) {
                onCloseClicked()
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCloseClicked()
                }
            ) {
                Text("Continue")
            }
        },
    )
}


