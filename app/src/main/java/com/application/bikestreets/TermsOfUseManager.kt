package com.application.bikestreets

import android.app.Activity
import android.content.Context

class TermsOfUseManager(activity: Activity) {
    val termsUrl: String = "https://www.bikestreets.com/terms"

    // Constant value describing the most current terms of use version.
    // *Please Note* Incrementing this value will force all active users to re-accept their terms of use, and
    // that it should only be done in conjunction with changes to the terms of use document.
    private val termsOfUseVersion: Int = 1

    // Key that the most recently accepted Terms of Use version is stored under in SharedPreferences
    private val termsOfUseVersionKey = "accepted_terms_of_use_version"

    // shared preferences from parent activity to be used for version read/write
    private val mSharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)

    // the user's most recently accepted version of the terms of use, defaulted to 0 in the event
    // that this is their first time opening up the app
    private val mostRecentlyAcceptedVersion = mSharedPreferences.getInt(termsOfUseVersionKey, 0)

    fun hasUnsignedTermsOfUse(): Boolean {
        // is the stored version less than the most recent version?
        return mostRecentlyAcceptedVersion < termsOfUseVersion
    }

    fun accept() {
        // save off most recently accepted terms of use version in shared preferences
        with(mSharedPreferences.edit()) {
            putInt(termsOfUseVersionKey, termsOfUseVersion)
            apply()
        }
    }
}