package com.benjosm.ospandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.benjosm.ospandroid.ui.theme.OspAndroidTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.benjosm.ospandroid.R
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.gms.tasks.Task
import android.util.Log
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create Google Sign In Client
        createGoogleSignInClient()

        // Check for a valid session
        if (checkForValidSession()) {
            // Session is valid, proceed to main UI
            setContent {
                OspAndroidTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Greeting(
                            name = "Authorized User",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        } else {
            // No valid session, launch Google Sign-In
            val signInIntent = googleSignInClient.getSignInIntent()
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun createGoogleSignInClient(): GoogleSignInClient {
        // Configure sign-in to request an ID token and email address
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        return googleSignInClient
    }

    private fun checkForValidSession(): Boolean {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "secret_prefs",
                masterKeyAlias,
                applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val accessToken = sharedPreferences.getString("access_token", null)
            !accessToken.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    // Store the token securely
                    saveIdToken(idToken)
                    // Send the ID token to the backend
                    sendIdTokenToBackend(idToken)
                } else {
                    handleError("No ID token received")
                }
            } catch (e: ApiException) {
                handleError(e.statusCode)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error processing sign-in", e)
                handleError("An unexpected error occurred")
            }
        }
    }

    private fun saveIdToken(idToken: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "secret_prefs",
                masterKeyAlias,
                applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            with(sharedPreferences.edit()) {
                putString("google_id_token", idToken)
                apply()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to save ID token", e)
        }
    }

    private fun sendIdTokenToBackend(idToken: String) {
        Toast.makeText(this, "Authenticating with backend...", Toast.LENGTH_SHORT).show()
    
        lifecycleScope.launch {
            val result = AuthService.signInWithProvider("google", idToken)
            if (result.isSuccess) {
                val authResponse = result.getOrThrow()
                saveTokens(authResponse.accessToken, authResponse.refreshToken)
                handleBackendResponse(true)
            } else {
                val exception = result.exceptionOrNull()
                handleError(exception?.message ?: "Authentication failed")
            }
        }
    }
    
    private fun saveTokens(accessToken: String, refreshToken: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "secret_prefs",
                masterKeyAlias,
                applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            with(sharedPreferences.edit()) {
                putString("access_token", accessToken)
                putString("refresh_token", refreshToken)
                apply()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to save tokens", e)
        }
    }

    private fun handleBackendResponse(success: Boolean) {
        if (success) {
            // Update UI for authenticated state
            setContent {
                OspAndroidTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Greeting(
                            name = "Authorized User",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        } else {
            handleError("Authentication failed. Please try again.")
        }
    }

    private fun handleError(errorCode: Int) {
        val message = when (errorCode) {
            4 -> "Sign in cancelled"
            5 -> "Sign in failed. Network error"
            2 -> "Sign in failed. Invalid account"
            else -> "Sign in failed. Error code: $errorCode"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("MainActivity", "Sign-in error: $errorCode")
    }

    private fun handleError(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        Log.e("MainActivity", "Error: $errorMessage")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OspAndroidTheme {
        Greeting("Android")
    }
}
