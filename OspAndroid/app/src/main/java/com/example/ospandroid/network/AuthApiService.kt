package com.example.ospandroid.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("/api/v1/auth/signin")
    suspend fun signIn(@Body request: SignInRequest): Response<SignInResponse>
}
