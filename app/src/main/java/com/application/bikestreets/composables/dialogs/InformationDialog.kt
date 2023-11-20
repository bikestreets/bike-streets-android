package com.application.bikestreets.composables.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.application.bikestreets.theme.Colors

@Composable
fun InformationDialog(onCloseInformationClicked: () -> Unit) {
    BikeStreetsDialog(
        onCloseClicked = { onCloseInformationClicked() },
        title = "Info",
        dialogContent = {
            Column {
                LegendRow(legendText = "On-Street Biking", color = Colors.vamosBlue)
                LegendRow(legendText = "Walk Your Bike", color = Colors.sidewalk)
                LegendRow(legendText = "Trail Biking", color = Colors.vamosTrail)
            }
        }
    )
}

@Composable
fun LegendRow(legendText: String, color: Color) {
    Row(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .height(4.dp)
                .width(12.dp)
                .background(color = color, shape = RoundedCornerShape(2.dp))
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.padding(6.dp))
        Text(legendText)

    }
}