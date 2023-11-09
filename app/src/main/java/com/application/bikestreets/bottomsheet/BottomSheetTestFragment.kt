package com.application.bikestreets.bottomsheet

import MyBottomSheetWithFloatingElements
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.application.bikestreets.composables.BottomSheet
import com.application.bikestreets.composables.BottomSheetTestUi
import com.application.bikestreets.composables.SheetContent

class BottomSheetTestFragment : Fragment() {

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BottomSheet()
            }
        }
    }
}