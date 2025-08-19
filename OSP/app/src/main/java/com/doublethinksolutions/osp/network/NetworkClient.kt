package com.doublethinksolutions.osp.network

import android.content.Context
import com.doublethinksolutions.osp.managers.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val BASE_URL = "https:api.doublethinksolutions.com/api/v1/"

    // Hold the Retrofit instance
    private lateinit var retrofit: Retrofit

    // Lazily initialize services to ensure Retrofit is configured first
    val authApiService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
    val mediaApiService: MediaApiService by lazy { retrofit.create(MediaApiService::class.java) }

    // An init function to set up the client with a context
    fun initialize(context: Context) {
        if (::retrofit.isInitialized) {
            // Already initialized, do nothing
            return
        }

        // Create SessionManager and AuthInterceptor
        val sessionManager = SessionManager(context)
        val authInterceptor = AuthInterceptor(sessionManager)
        val tokenAuthenticator = TokenAuthenticator(sessionManager)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .authenticator(tokenAuthenticator)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor) // Logger should be last to see final request
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
