package com.doublethinksolutions.osp.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/signin")
    suspend fun signIn(@Body request: SignInRequest): Response<SignInResponse>

    // Synchronous call for use within the Authenticator
    @POST("auth/refresh-token")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>
}
