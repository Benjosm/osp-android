package com.doublethinksolutions.osp.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Defines the API endpoints for media-related operations, such as uploads.
 */
interface MediaApiService {

    /**
     * Uploads a media file along with its associated metadata.
     * This uses a multipart request to send both the binary file data and
     * the JSON metadata in a single call.
     *
     * @param file The media file part of the request. The name of this part should be "file".
     * @param metadata The JSON metadata part of the request. The name of this part is "metadata".
     * @return A Retrofit Response. A successful upload should return a 2xx status code.
     *         We use Response<Unit> because we only care about the success status, not a response body.
     */
    @Multipart
    @POST("media")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody?
    ): Response<Unit>
}
