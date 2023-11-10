package com.application.bikestreets.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
fun SafeStringResource(id: Int, default: String = "Preview String"): String {
    val context = LocalContext.current
    return if (LocalInspectionMode.current) {
        // Return a default string if in preview mode
        default
    } else {
        // Otherwise, load the actual string resource
        context.getString(id)
    }
}