package com.application.bikestreets

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import com.application.bikestreets.databinding.ActivityAboutBinding
import com.mapbox.android.core.permissions.PermissionsManager

class AboutFragment : Fragment() {
    private val feedbackUrl = Uri.parse("https://www.bikestreets.com/mobile-app-feedback")

    private var _binding: ActivityAboutBinding? = null
    private val binding get() = _binding!!

    private var buttonClickListener: OnPermissionButtonClickListener? = null


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

        setUpEnableLocationButton()

        return view
    }

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

    // If the user has not accepted location permission, give them another chance to enable
    private fun setUpEnableLocationButton() {
        if (!PermissionsManager.areLocationPermissionsGranted(activity)) {
            binding.enableLocationButton.setOnClickListener {
                buttonClickListener?.onPermissionButtonClicked()
            }

            binding.enableLocationButton.isVisible = true
        }
    }

    fun setOnPermissionRequested(listener: OnPermissionButtonClickListener) {
        buttonClickListener = listener
    }

    interface OnPermissionButtonClickListener {
        fun onPermissionButtonClicked()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
