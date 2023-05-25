package com.application.bikestreets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.application.bikestreets.databinding.ActivityTermsOfUseBinding

class TermsOfUse : AppCompatActivity() {
    private lateinit var binding: ActivityTermsOfUseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsOfUseBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val termsOfUseManager = TermsOfUseManager(this)

        // check to see if the user has unsigned terms before doing anything else
        if (termsOfUseManager.hasUnsignedTermsOfUse()) {
            // if so, display window and fill with Terms Of Use text
            setSupportActionBar(binding.toolbar)

            // fill terms of use window
            val termsOfUseWindow = binding.includeTermsOfUse.termsOfUse
            termsOfUseWindow.text = termsOfUseManager.contractText()

            // activate the accept button
            binding.includeTermsOfUse.termsOfUseAccept
                .setOnClickListener {
                    // accept terms and conditions
                    termsOfUseManager.accept()

                    // exit activity
                    finish()
                }
        } else {
            // no unsigned terms: bail!
            finish()
        }

    }
}
