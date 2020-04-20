package com.application.bikestreets

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_terms_of_use.*

class TermsOfUse : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val termsOfUseManager = TermsOfUseManager(this)

        // check to see if the user has unsigned terms before doing anything else
        if (termsOfUseManager.hasUnsignedTermsOfUse()) {
            // if so, display window and fill with Terms Of Use text
            setContentView(R.layout.activity_terms_of_use)
            setSupportActionBar(toolbar)

            // fill terms of use window
            val termsOfUseWindow = findViewById<TextView>(R.id.terms_of_use)
            termsOfUseWindow.text = termsOfUseManager.contractText()

            // activate the accept button
            findViewById<Button>(R.id.terms_of_use_accept)
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
