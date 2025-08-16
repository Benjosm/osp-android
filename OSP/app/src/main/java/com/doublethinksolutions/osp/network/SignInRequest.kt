package com.doublethinksolutions.osp.network

data class SignInRequest(
    val provider: String,
    val token: String
)
