package com.application.bikestreets

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.preference.PreferenceFragmentCompat

class About : AppCompatActivity() {
    private val feedbackUrl = Uri.parse("https://www.bikestreets.com/mobile-app-feedback")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // This layout is designed only for portriat mode, so force the user to that orientation
        // regardless of device settings
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        activateFeedbackButton()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    fun activateFeedbackButton() {
       findViewById<Button>(R.id.feedback_button).setOnClickListener {
           val intent = Intent(Intent.ACTION_VIEW).setData(feedbackUrl)
           startActivity(intent)
       }
    }
}
