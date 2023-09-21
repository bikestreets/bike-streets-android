package com.application.bikestreets.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, message, duration).show()
}

fun getColorHexString(context: Context, @ColorRes colorResId: Int): String {
    val colorInt = ContextCompat.getColor(context, colorResId)
    return String.format("#%06X", (0xFFFFFF and colorInt))
}