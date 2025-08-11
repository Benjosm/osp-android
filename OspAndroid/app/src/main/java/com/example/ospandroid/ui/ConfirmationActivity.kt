package com.example.ospandroid.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ospandroid.R

class ConfirmationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        // Retrieve data passed from the upload process
        val trustScore = intent.getDoubleExtra("trust_score", -1.0)
        val uploadTimeMs = intent.getLongExtra("upload_time_ms", -1)

        val trustScoreTextView = findViewById<TextView>(R.id.trustScoreTextView)
        val uploadTimeTextView = findViewById<TextView>(R.id.uploadTimeTextView)

        // Format trust score
        trustScoreTextView.text = if (trustScore >= 0) "Trust Score: ${"%.2f".format(trustScore)}" else "Trust Score: N/A"

        // Format upload time
        if (uploadTimeMs >= 0) {
            val seconds = uploadTimeMs / 1000.0  // Convert milliseconds to seconds for display
            uploadTimeTextView.text = "Upload Time: ${"%.2f".format(seconds)} seconds"
        } else {
            uploadTimeTextView.text = "Upload Time: N/A"
        }
    }
}
