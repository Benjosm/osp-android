package com.doublethinksolutions.osp.network

import com.google.gson.annotations.SerializedName

// Request body for the /refresh-token endpoint
data class RefreshTokenRequest(
    val refreshToken: String
)

// Response body from a successful /refresh-token call
data class RefreshTokenResponse(
    @SerializedName("accessToken") // Matches the key in your FastAPI response
    val accessToken: String
)
