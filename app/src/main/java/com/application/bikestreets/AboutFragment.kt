package com.application.bikestreets

import android.content.Intent
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

    private fun showVersionNumber() {
        binding.versionNumber.text = "v${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE}"
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        // TODO: Broken
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
