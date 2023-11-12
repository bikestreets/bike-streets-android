package com.application.bikestreets.newstuff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the UI content of this activity
        setContent {
            MainUi()
        }
    }

    @Composable
    fun MainUi() {

        var showSettings by remember { mutableStateOf(false) }

        // TermsOfUse()

        if(showSettings){
            SettingsModal(onCloseSettingsClicked = { showSettings = false })
        }
        BottomSheetAndMap(
            onSettingsClicked = { showSettings = true }
        )
    }
}