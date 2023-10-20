package com.application.bikestreets.utils

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
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

fun hideKeyboard(activity: Activity) {
    val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    // Retrieve the currently focused view
    val view = activity.currentFocus
    if (view != null) {
        // Explicitly clear focus
        view.clearFocus()
        // Hide the soft keyboard
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    } else {
        // If there's no currently focused view, use the activity's root view to hide the soft keyboard
        inputMethodManager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
    }
}