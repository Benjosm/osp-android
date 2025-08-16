package com.doublethinksolutions.osp.network

import com.doublethinksolutions.osp.broadcast.AuthEvent
import com.doublethinksolutions.osp.broadcast.AuthEventBus
import com.doublethinksolutions.osp.managers.SessionManager
import com.doublethinksolutions.osp.network.NetworkClient
import com.doublethinksolutions.osp.network.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val sessionManager: SessionManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // We need to retrieve the current refresh token.
        // runBlocking is acceptable here because an Authenticator must be synchronous.
        val refreshToken = runBlocking {
            sessionManager.refreshTokenFlow.first()
        }

        // If we don't have a refresh token, we can't do anything. Give up.
        if (refreshToken.isNullOrBlank()) {
            // Optional: You could trigger a navigation to the login screen here if needed
            // by using a shared Flow or another event mechanism.
            return null
        }

        // Use a synchronized block to prevent multiple threads from trying to refresh the token at the same time.
        // This is crucial if multiple API calls fail at once.
        synchronized(this) {
            val newAccessToken = runBlocking { sessionManager.accessTokenFlow.first() }
            val oldAccessToken = response.request.header("Authorization")?.substringAfter("Bearer ")

            // Check if another thread has already refreshed the token while we were waiting.
            // If the token has changed, just retry the request with the new token.
            if (newAccessToken != oldAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            }

            // Time to make the synchronous refresh call.
            val call = NetworkClient.authApiService.refreshToken(RefreshTokenRequest(refreshToken))
            val refreshResponse = try {
                call.execute()
            } catch (e: Exception) {
                null
            }

            if (refreshResponse != null && refreshResponse.isSuccessful) {
                val newTokens = refreshResponse.body()
                if (newTokens != null) {
                    // Success! Save the new token.
                    runBlocking {
                        sessionManager.saveAccessToken(newTokens.accessToken)
                    }
                    // Check if the original request was a multipart upload.
                    val isMultipart = response.request.body?.contentType()?.type == "multipart"

                    if (isMultipart) {
                        // It's a non-retry-able request (like a file stream).
                        // Signal the app to retry it manually.
                        print("Multipart upload failed, retrying manually...")
                        runBlocking { AuthEventBus.postEvent(AuthEvent.TokenRefreshed) }
                        // Return null to CANCEL the automatic retry.
                        return null
                    } else {
                        // It's a simple, retry-able request (e.g., a simple GET or JSON POST).
                        // Proceed with the automatic retry as before.
                        print("Simple request failed, retrying automatically...")
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.accessToken}")
                            .build()
                    }
                }
            }
        }

        // If the refresh call failed (e.g., refresh token expired), clear the session and give up.
        runBlocking {
            sessionManager.clearSession()
        }
        return null // Giving up tells OkHttp to fail the original request.
    }
}
