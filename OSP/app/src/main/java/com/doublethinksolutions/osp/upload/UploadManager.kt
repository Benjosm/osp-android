package com.doublethinksolutions.osp.upload

import android.content.Context
import android.os.Build
import android.util.Log
import com.doublethinksolutions.osp.broadcast.AuthEvent
import com.doublethinksolutions.osp.broadcast.AuthEventBus
import com.doublethinksolutions.osp.data.PhotoMetadata
import com.doublethinksolutions.osp.network.NetworkClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object UploadManager {
    private const val TAG = "UploadManager"
    private val gson = Gson()

    /**
     * A private DTO that exactly matches the JSON structure expected by the FastAPI backend.
     */
    private data class UploadMetadata(
        val capture_time: String,
        val lat: Double,
        val lng: Double,
        val orientation: Int
    )

    suspend fun upload(
        context: Context,
        file: File,
        metadata: PhotoMetadata?,
        onProgress: (Float) -> Unit,
        onSuccess: (Long) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // We'll wrap the core logic to allow for a single retry.
        var attempt = 0
        val maxAttempts = 2

        while (attempt < maxAttempts) {
            attempt++
            try {
                // Call the actual upload logic
                performUpload(context, file, metadata, onProgress, onSuccess, onFailure)
                // If it succeeds, we're done.
                return
            } catch (e: IOException) {
                // Check if this exception is a 401 Unauthorized
                // We need to parse the message as Retrofit wraps the HTTP status in the exception.
                val isAuthError = e.message?.contains("code: 401") == true

                if (isAuthError && attempt < maxAttempts) {
                    Log.d(TAG, "Upload failed with 401, waiting for token refresh event...")

                    // Wait for the authenticator to do its job.
                    // Use a timeout in case something goes wrong with the event bus.
                    val refreshEvent = withTimeoutOrNull(5000) { // 5-second timeout
                        AuthEventBus.events.filterIsInstance<AuthEvent.TokenRefreshed>().first()
                    }

                    if (refreshEvent != null) {
                        Log.d(TAG, "Token refreshed event received. Retrying upload...")
                        // The loop will now continue for the next attempt.
                        continue
                    } else {
                        Log.w(TAG, "Timed out waiting for token refresh. Failing upload.")
                        // Fall through to the failure case.
                    }
                }

                // If it's not an auth error or we've exhausted retries, fail.
                Log.e(TAG, "Upload failed permanently for ${file.name}", e)
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
                return
            }
        }
    }

    private suspend fun performUpload(
        context: Context,
        file: File,
        metadata: PhotoMetadata?,
        onProgress: (Float) -> Unit,
        onSuccess: (Long) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uploadStartTimestamp = System.currentTimeMillis()
        if (!file.exists()) {
            throw IOException("File does not exist at path: ${file.path}")
        }

        // 2. Prepare metadata part
        val metadataRequestBody = metadata?.let { photoMetadata ->

            // Convert milliseconds timestamp to ISO 8601 UTC string (e.g., "2025-08-14T17:30:41.043Z")
            val captureTimeIso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26+ uses java.time.Instant
                Instant.ofEpochMilli(photoMetadata.timestamp).toString()
            } else {
                // Older devices: format manually to UTC ISO 8601
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.format(Date(photoMetadata.timestamp))
            }

            // Create the DTO that matches the server's expected format
            val uploadMetadata = UploadMetadata(
                capture_time = captureTimeIso,
                // Extract lat/lng, providing 0.0 as a fallback if location is null
                lat = photoMetadata.location?.latitude ?: 0.0,
                lng = photoMetadata.location?.longitude ?: 0.0,
                // Map the orientation field name
                orientation = photoMetadata.deviceOrientationDegrees
            )

            // Serialize the *new* DTO object, not the original PhotoMetadata
            gson.toJson(uploadMetadata).toRequestBody("application/json".toMediaTypeOrNull())
        }

        val fileRequestBody = file.asProgressRequestBody("image/jpeg".toMediaTypeOrNull()) { progress ->
            onProgress(progress)
        }
        val filePart = MultipartBody.Part.createFormData("file", file.name, fileRequestBody)

        Log.d(TAG, "Starting upload for ${file.name}...")
        val response = NetworkClient.mediaApiService.uploadMedia(filePart, metadataRequestBody)

        if (response.isSuccessful) {
            val uploadDurationMs = System.currentTimeMillis() - uploadStartTimestamp
            Log.d(TAG, "Upload successful for ${file.name} in $uploadDurationMs ms.")
            withContext(Dispatchers.Main) {
                onSuccess(uploadDurationMs)
            }
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            // This is how we bubble up the error to the calling function's catch block
            throw IOException("Upload failed with code: ${response.code()}. Body: $errorBody")
        }
    }
}

// Extension function to wrap a File in our ProgressRequestBody
private fun File.asProgressRequestBody(
    contentType: MediaType?,
    onProgress: (Float) -> Unit
): RequestBody {
    return object : RequestBody() {
        override fun contentType() = contentType

        override fun contentLength() = length()

        override fun writeTo(sink: BufferedSink) {
            val source = source().buffer()
            var totalBytesWritten = 0L
            val fileLength = contentLength()
            var bytesRead: Long

            while (source.read(sink.buffer, 8192L).also { bytesRead = it } != -1L) {
                totalBytesWritten += bytesRead
                sink.flush()
                onProgress(totalBytesWritten.toFloat() / fileLength)
            }
        }
    }
}
