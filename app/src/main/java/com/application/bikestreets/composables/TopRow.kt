package com.application.bikestreets.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopRow(titleText: String, onCloseClicked: () -> Unit) {
    //TODO: Fix Row rendering the IconButton pre-offset, may need to set a height and enforce clipping
    Row(
        Modifier
            .fillMaxWidth()
    ) {
        Text(text = titleText, fontSize = 18.sp)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onCloseClicked,
            modifier = Modifier
                .absoluteOffset(y = (-14).dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close Drawer",
                tint = Color.Gray
            )
        }
    }
}