package com.application.bikestreets.composables.bottomsheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.application.bikestreets.theme.Dimens

@Composable
fun TopRow(titleText: String, onCloseClicked: () -> Unit, isCollapsed: Boolean) {
    val iconOpacity = remember { Animatable(1f) }

    // Animate the visibility of the x button appearing
    LaunchedEffect(isCollapsed) {
        iconOpacity.animateTo(
            targetValue = if (isCollapsed) 0f else 1f,
            animationSpec = tween(durationMillis = 200)
        )
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(Dimens.closeSheetBtnSize)
            .padding(horizontal = Dimens.defaultPadding)
    ) {
        Text(
            text = titleText,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onCloseClicked,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .alpha(iconOpacity.value)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close Drawer",
                tint = Color.Gray
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun TopRowPreview() {
    TopRow(titleText = "Test", onCloseClicked = { }, isCollapsed = false)
}