package com.application.bikestreets

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.application.bikestreets.databinding.ActivityAboutBinding

class About : AppCompatActivity() {
    private val feedbackUrl = Uri.parse("https://www.bikestreets.com/mobile-app-feedback")
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // This layout is designed only for portrait mode, so force the user to that orientation
        // regardless of device settings
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

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

    private fun activateFeedbackButton() {
       binding.feedbackButton.setOnClickListener {
           val intent = Intent(Intent.ACTION_VIEW).setData(feedbackUrl)
           startActivity(intent)
       }
    }
}
