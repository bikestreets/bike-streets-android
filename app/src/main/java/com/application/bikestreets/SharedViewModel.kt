package com.application.bikestreets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.application.bikestreets.api.modals.Route

class SharedViewModel : ViewModel() {
    val route = MutableLiveData<List<Route>>()
}