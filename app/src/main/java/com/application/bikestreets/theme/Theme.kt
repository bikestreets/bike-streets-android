package com.application.bikestreets.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

@Composable
fun BikeStreetsTheme(
    content: @Composable () -> Unit
) {
    val colors = lightColors(
        primary = Colors.vamosBlue,
        primaryVariant = Colors.vamosLightBlue,
    )
    MaterialTheme(
        colors = colors,
        content = content
    )
}