package com.application.bikestreets.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetUi() {
    ModalDrawerSheet(
    ) {
        // Sheet content
        Button(onClick = {

        }) {
            Text("Hide bottom sheet")
        }
    }

}