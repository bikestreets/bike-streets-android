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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.application.bikestreets.BuildConfig
import com.application.bikestreets.api.modals.LegendItem
import com.application.bikestreets.theme.Colors

val legendItems = listOf(
    LegendItem("Neighborhood Streets & Protected Bike Lanes", Colors.route_neighborhood_and_bikelane),
    LegendItem("Trails & Parks", Colors.route_trail),
    LegendItem("Unprotected Bike Lanes, Sharrows, Busier Streets", Colors.busy_and_sharrow),
    LegendItem("Ride Your Bike on the Sidewalk", Colors.sidewalk_bike),
    LegendItem("Walk Your Bike on the Sidewalk", Colors.sidewalk_walk)
)

@Composable
fun InformationDialog(onCloseInformationClicked: () -> Unit) {
    BikeStreetsDialog(
        onCloseClicked = { onCloseInformationClicked() },
        title = "Info",
        dialogContent = {
            DialogContent()
        }
    )
}

@Composable
fun DialogContent() {
    Column {
        for (item in legendItems) {
            LegendRow(legendText = item.legendText, color = item.color)
            Spacer(modifier = Modifier.padding(2.dp))
        }
        Spacer(modifier = Modifier.padding(10.dp))
        Text(showVersionNumber())
    }
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

@Preview
@Composable
private fun LegendPreview() {
    BikeStreetsDialog(
        onCloseClicked = { },
        title = "Info",
        dialogContent = {
            DialogContent()
        }
    )
}

private fun showVersionNumber(): String {
    // Show a d if is a debug build
    val buildType = if (BuildConfig.DEBUG) "d" else ""
    return "v${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}${buildType}"
}