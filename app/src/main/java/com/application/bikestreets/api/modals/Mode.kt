package com.application.bikestreets.api.modals

enum class Mode {
    CYCLE,
    PUSHING_BIKE;

    companion object {
        fun getMode(modeString: String): Mode {
            return when (modeString) {
                "pushing bike" -> PUSHING_BIKE
                else -> CYCLE
            }
        }
    }
}