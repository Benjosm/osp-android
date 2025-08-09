package com.benjosm.ospandroid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName
import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.net.HttpURLConnection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.benjosm.ospandroid.R

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class SignInRequest(
    @SerializedName("provider") val provider: String,
    @SerializedName("id_token") val idToken: String
)

object AuthService {
    private const val BASE_URL = "https://api.osp.example.com"

    private val api: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    private val mutex = Mutex()

    suspend fun signInWithProvider(provider: String, idToken: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val request = SignInRequest(provider, idToken)
                    val response = api.signIn(request)

                    when {
                        response.isSuccessful && response.body() != null -> {
                            Result.success(response.body()!!)
                        }
                        response.code() == HttpURLConnection.HTTP_UNAUTHORIZED -> {
                            Result.failure(Exception("Invalid credentials"))
                        }
                        else -> {
                            Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthService", "Network error", e)
                    Result.failure(e)
                }
            }
        }
    }

    private interface AuthApi {
        @POST("/api/v1/auth/signin")
        suspend fun signIn(@Body request: SignInRequest): retrofit2.Response<AuthResponse>
    }
}
