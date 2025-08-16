package com.doublethinksolutions.osp.network

import com.doublethinksolutions.osp.managers.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * An OkHttp Interceptor that adds the Authorization header to requests.
 * It retrieves the access token from the SessionManager and attaches it.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. Get the original request from the chain
        val originalRequest = chain.request()

        // 2. Get the access token from the SessionManager.
        // Since the interceptor's 'intercept' method is synchronous, and our token
        // is stored in a Flow, we use 'runBlocking' to bridge the gap.
        // We use .first() to get the current value of the token.
        val token = runBlocking {
            sessionManager.accessTokenFlow.first()
        }

        // 3. If the token exists, build a new request with the Authorization header.
        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // 4. Build the new request
        val newRequest = requestBuilder.build()

        // 5. Let the modified request proceed through the chain
        return chain.proceed(newRequest)
    }
}
