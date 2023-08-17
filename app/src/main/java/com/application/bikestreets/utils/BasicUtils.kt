package com.application.bikestreets.utils

import android.content.Context
import android.widget.Toast

object ToastUtils {

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
}