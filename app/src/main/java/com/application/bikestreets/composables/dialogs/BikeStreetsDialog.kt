package com.application.bikestreets.composables.dialogs

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable

@Composable
fun BikeStreetsDialog(
    onCloseClicked: () -> Unit,
    title: String? = "",
    dialogContent: @Composable () -> Unit,
    confirmationText: String? = "Continue",
    isDismissible: Boolean? = true,
) {
    AlertDialog(
        title = {
            Text(text = title.toString())
        },
        text = {
            dialogContent()
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
                Text(confirmationText.toString())
            }
        },
    )
}


