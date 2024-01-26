package com.application.bikestreets.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.application.bikestreets.R
import com.application.bikestreets.theme.Colors
import com.application.bikestreets.theme.Dimens

@Composable
fun ActionButtonsContainer(
    onInfoButtonClicked: (() -> Unit),
    onLocationButtonClicked: (() -> Unit)
) {

    Column {
        BlueActionButtons(
            icon = Icons.Rounded.NearMe,
            contentDescription = stringResource(R.string.update_camera_location_button),
            onClick = { onLocationButtonClicked() }
        )
        Spacer(modifier = Modifier.padding(6.dp))
        BlueActionButtons(
            icon = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.info_button),
            onClick = { onInfoButtonClicked() }
        )
    }
}

@Composable
fun BlueActionButtons(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .background(
                color = Colors.vamosBlue,
                shape = CircleShape
            ) // Change the color as needed
            .size(
                Dimens.tappableIconSize
            )
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White
        )
    }
}

@Preview
@Composable

fun BlueActionButtonsPreview() {
    BlueActionButtons(
        icon = Icons.Outlined.NearMe,
        contentDescription = stringResource(R.string.update_camera_location_button),
        onClick = { })
}
