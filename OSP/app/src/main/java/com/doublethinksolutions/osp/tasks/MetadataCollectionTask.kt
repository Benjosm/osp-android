package com.doublethinksolutions.osp.tasks

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.doublethinksolutions.osp.data.PhotoMetadata

/**
 * A task responsible for synchronously collecting all required metadata just before
 * a photo is taken.
 *
 * This class should be instantiated and its `collect` method called immediately
 * before initiating an image capture to get the most accurate snapshot of the device's state.
 *
 * @param context The application or activity context, used to access system services.
 */
class MetadataCollectionTask(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Collects all metadata and returns it in a PhotoMetadata object.
     *
     * @return A populated [PhotoMetadata] object.
     * @throws SecurityException if location permissions are required but not granted.
     */
    fun collect(): PhotoMetadata {
        val timestamp = System.currentTimeMillis()
        val location = collectLocation()
        val orientation = collectDeviceOrientation()

        return PhotoMetadata(
            timestamp = timestamp,
            location = location,
            deviceOrientationDegrees = orientation
        )
    }

    /**
     * Retrieves the last known location from the system's Fused Location Provider.
     *
     * It performs a permission check and will throw a SecurityException if permissions
     * are not granted, as location is a critical piece of metadata for this app.
     *
     * @return The last known [Location], or null if it's not available.
     */
    @SuppressLint("MissingPermission") // Permissions are checked before this is called.
    private fun collectLocation(): Location? {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocationPermission && !hasCoarseLocationPermission) {
            // This exception will be caught by the try-catch block in the Fragment.
            Log.w("MetadataCollectionTask", "Location permission not granted. Cannot collect location.")
            // Returning null instead of crashing, as the fragment is designed to handle this.
            return null
        }

        // Prefer the Fused Location Provider for better battery and accuracy.
        // Fallback to GPS provider if needed.
        return try {
            locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            Log.e("MetadataCollectionTask", "Could not get last known location", e)
            null
        }
    }

    /**
     * Determines the physical orientation of the device.
     *
     * @return The orientation in degrees (0, 90, 180, 270).
     */
    @Suppress("DEPRECATION")
    private fun collectDeviceOrientation(): Int {
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }

        return when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0 // Default to 0 degrees if rotation is null or unknown
        }
    }
}
