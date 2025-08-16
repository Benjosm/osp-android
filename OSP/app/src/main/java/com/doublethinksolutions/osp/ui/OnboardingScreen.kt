package com.doublethinksolutions.osp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onContinueClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome to OSP!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Here's how to secure your content:",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))

            // Feature list
            FeatureItem(
                icon = Icons.Default.Camera,
                title = "Capture Evidence",
                description = "Take photos and videos directly through the OSP app to begin the verification process."
            )
            FeatureItem(
                icon = Icons.Default.CheckCircle,
                title = "Instant Verification",
                description = "Your captures are instantly watermarked and time-stamped for undeniable authenticity."
            )
            FeatureItem(
                icon = Icons.Default.CloudUpload,
                title = "Secure Upload",
                description = "Content is sent to our secure database and given a unique Trust Score based on upload metrics."
            )
            FeatureItem(
                icon = Icons.Default.Search,
                title = "Review & Search",
                description = "Access and manage your verified content anytime on the OSP website."
            )

            Spacer(modifier = Modifier.weight(1f)) // Pushes button to the bottom

            Button(
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Get Started", fontSize = 16.sp)
            }
        }
    }
}
