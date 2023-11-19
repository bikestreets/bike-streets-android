package com.application.bikestreets.composables.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun BikeStreetsDialog(
    onCloseClicked: () -> Unit,
    title: String? = "",
    dialogContent: @Composable () -> Unit,
    confirmationText: String? = "Continue",
    isDismissible: Boolean? = true,
) {
    // Parent Dialog is used to provide full screen black overlay
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
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
        }
    )
}


