package com.application.bikestreets.utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.application.bikestreets.R
import com.application.bikestreets.constants.PreferenceConstants

fun mapTypeFromPreferences(context: Context): String? {
    val sharedPreferences =
        context.getSharedPreferences(getDefaultPackageName(context), AppCompatActivity.MODE_PRIVATE)
    return sharedPreferences.getString(
        PreferenceConstants.MAP_TYPE_PREFERENCE_KEY,
        context.getString(R.string.preference_street)
    )
}

// Carry over from previous versions using getDefaultSharedPreferences()
fun getDefaultPackageName(context: Context): String {
    return "${context.packageName}_preferences"
}