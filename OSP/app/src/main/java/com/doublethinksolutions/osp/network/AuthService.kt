package com.doublethinksolutions.osp.network

import retrofit2.HttpException
import java.io.IOException

class AuthService {
    private val apiService = NetworkClient.authApiService

    sealed class Result {
        data class Success(val accessToken: String, val refreshToken: String) : Result()
        object InvalidToken : Result()
        object ProviderMismatch : Result()
        data class Error(val message: String) : Result()
        object NetworkError : Result()
    }

    suspend fun signInWithGoogle(idToken: String): Result {
        return try {
            val request = SignInRequest(provider = "google", token = idToken)
            val response = apiService.signIn(request)

            when (response.code()) {
                200 -> {
                    val body = response.body()
                    if (body != null) {
                        Result.Success(body.accessToken, body.refreshToken)
                    } else {
                        Result.Error("Empty response body")
                    }
                }
                400 -> {
                    // Check if it's a provider mismatch or invalid token
                    // Assuming backend returns specific message; in practice, you might check error details
                    val errorBody = response.errorBody()?.string()
                    if (errorBody?.contains("provider mismatch", ignoreCase = true) == true) {
                        Result.ProviderMismatch
                    } else {
                        Result.InvalidToken
                    }
                }
                401 -> Result.InvalidToken
                else -> Result.Error("Unexpected error: ${response.code()}")
            }
        } catch (e: HttpException) {
            Result.Error("HTTP error: ${e.code()}")
        } catch (e: IOException) {
            Result.NetworkError
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message ?: "Unknown error"}")
        }
    }
}
