package com.doublethinksolutions.osp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.doublethinksolutions.osp.managers.SessionManager
import com.doublethinksolutions.osp.network.AuthService
import com.doublethinksolutions.osp.ui.CameraScreen
import com.doublethinksolutions.osp.ui.OnboardingScreen
import com.doublethinksolutions.osp.ui.SignInScreen
import com.doublethinksolutions.osp.ui.theme.OSPTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    object NeedsSignIn : UiState()
    object NeedsOnboarding : UiState()
    object Authenticated : UiState()
    data class Error(val message: String) : UiState()
}

class MainActivity : ComponentActivity() {

    private val authService = AuthService()
    private lateinit var credentialManager: CredentialManager
    private lateinit var sessionManager: SessionManager

    private var uiState by mutableStateOf<UiState>(UiState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        credentialManager = CredentialManager.create(this)
        sessionManager = SessionManager(applicationContext)

        setContent {
            OSPTheme {
                // This single LaunchedEffect is now responsible for all navigation state changes.
                // It runs once and collects flows for the lifetime of the composition.
                LaunchedEffect(key1 = Unit) {
                    sessionManager.accessTokenFlow.combine(sessionManager.hasCompletedOnboardingFlow) { token, hasOnboarded ->
                        Pair(token, hasOnboarded)
                    }.collect { (token, hasOnboarded) ->
                        // This logic is now executed only when we have real, loaded data.
                        uiState = when {
                            token.isNullOrBlank() -> UiState.NeedsSignIn
                            !hasOnboarded -> UiState.NeedsOnboarding
                            else -> UiState.Authenticated
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is UiState.Loading -> {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator()
                            }
                        }
                        is UiState.NeedsSignIn -> {
                            SignInScreen(
                                onSignInClick = { launchSignIn() }
                            )
                        }
                        is UiState.NeedsOnboarding -> {
                            OnboardingScreen(
                                onContinueClick = { completeOnboarding() }
                            )
                        }
                        is UiState.Authenticated -> {
                            CameraScreen()
                        }
                        is UiState.Error -> {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(32.dp)) {
                                Text(
                                    text = "An error occurred:\n\n${state.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchSignIn() {
        lifecycleScope.launch {
            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.google_web_client_id))
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result: GetCredentialResponse = credentialManager.getCredential(this@MainActivity, request)
                handleSignInSuccess(result)
            } catch (e: GetCredentialException) {
                Log.e("MainActivity", "GetCredentialException", e)
                handleSignInError("Google Sign-In failed: ${e.message}")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during sign-in", e)
                handleSignInError("An unexpected error occurred during sign-in.")
            }
        }
    }

    private fun handleSignInSuccess(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                sendIdTokenToBackend(idToken)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to extract Google ID token", e)
                handleSignInError("Error processing Google credential.")
            }
        } else {
            handleSignInError("Sign-in failed: Unrecognized credential type.")
        }
    }

    private fun sendIdTokenToBackend(idToken: String) {
        Toast.makeText(this, "Authenticating with backend...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            sessionManager.saveGoogleIdToken(idToken)

            when (val result = authService.signInWithGoogle(idToken)) {
                is AuthService.Result.Success -> {
                    sessionManager.saveBackendTokens(result.accessToken, result.refreshToken)
                    Log.d("MainActivity", "Backend tokens saved successfully.")
                    // The reactive logic will automatically handle the transition to the onboarding screen
                    // because hasCompletedOnboardingFlow will be `false`. No need to set uiState here.
                }
                is AuthService.Result.Error -> handleSignInError("Authentication failed: ${result.message}")
                is AuthService.Result.InvalidToken -> handleSignInError("Authentication failed: Invalid token.")
                is AuthService.Result.NetworkError -> handleSignInError("Authentication failed: Network error.")
                is AuthService.Result.ProviderMismatch -> handleSignInError("Authentication failed: Provider mismatch.")
            }
        }
    }

    private fun completeOnboarding() {
        lifecycleScope.launch {
            sessionManager.saveOnboardingCompleted(true)
        }
    }

    private fun handleSignInError(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        Log.e("MainActivity", "Sign-in Error: $errorMessage")
        // Reset the state to the sign-in screen to allow the user to try again.
        uiState = UiState.NeedsSignIn
    }
}
