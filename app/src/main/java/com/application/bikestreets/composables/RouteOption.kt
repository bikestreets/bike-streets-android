package com.application.bikestreets.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.application.bikestreets.utils.metersToMiles

@Composable
fun RouteOption(index: Int, distance: Double, onGoClicked: (() -> Unit)) {

    // Convert from Meters to Miles
    val distanceInMiles =  String.format("%.1f", metersToMiles(distance))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp), color = Color(0xFFDBDBDB)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Route $index")
                Text("$distanceInMiles mi")
            }
            Button(
                onClick = onGoClicked,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF4CAF50),
                    contentColor = Color.Black // Change the text color to white
                )
            ) {
                Text("GO")
            }
        }
    }
}

@Preview
@Composable
fun RouteOptionPreview() {
    RouteOption(index = 1, distance = 6872.7, onGoClicked = {})
}