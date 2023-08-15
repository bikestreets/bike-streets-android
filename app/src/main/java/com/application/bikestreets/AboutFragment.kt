package com.application.bikestreets

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import com.application.bikestreets.databinding.ActivityAboutBinding

class AboutFragment : Fragment() {
    private val feedbackUrl = Uri.parse("https://www.bikestreets.com/mobile-app-feedback")

    private var _binding: ActivityAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAboutBinding.inflate(inflater, container, false)
        val view = binding.root

        childFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()

        showVersionNumber()

        activateFeedbackButton()

        return view
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityAboutBinding.inflate(layoutInflater)
//        val view = binding.root
//        setContentView(view)
//
//        // This layout is designed only for portrait mode, so force the user to that orientation
//        // regardless of device settings
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//
//        supportFragmentManager
//            .beginTransaction()
//            .replace(R.id.settings, SettingsFragment())
//            .commit()
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//
//        showVersionNumber()
//
//        activateFeedbackButton()
//    }

    private fun showVersionNumber() {
        binding.versionNumber.text = "v${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE}"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
