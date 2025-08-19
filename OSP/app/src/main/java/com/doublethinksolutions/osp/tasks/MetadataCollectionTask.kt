package com.doublethinksolutions.osp.tasks

import android.location.Location
import com.doublethinksolutions.osp.data.DeviceOrientation
import com.doublethinksolutions.osp.data.PhotoMetadata

/**
 * A task responsible for synchronously collecting all required metadata just before
 * a photo is taken.
 *
 * This class reads the latest available data from the active providers
 * (like LocationProvider and OrientationProvider). It assumes that these providers
 * have been started and are actively listening for updates.
 *
 * Instantiate and call `collect` immediately before an image capture.
 */
class MetadataCollectionTask {

    /**
     * Collects all metadata and returns it in a PhotoMetadata object.
     * It reads the latest values from the singleton providers.
     *
     * @return A populated [PhotoMetadata] object. The location and orientation
     *         may be null if the providers haven't received data yet.
     */
    fun collect(): PhotoMetadata {
        val timestamp = System.currentTimeMillis()
        val location = collectLocation()
        val orientation = collectDeviceOrientation()

        return PhotoMetadata(
            timestamp = timestamp,
            location = location,
            deviceOrientation = orientation
        )
    }

    /**
     * Retrieves the last known location from the LocationProvider.
     *
     * @return The last known [Location], or null if it's not available.
     */
    private fun collectLocation(): Location? {
        return LocationProvider.latestLocation
    }

    /**
     * Retrieves the latest device orientation from the OrientationProvider.
     * @return The most recent [DeviceOrientation], or null if not available.
     */
    private fun collectDeviceOrientation(): DeviceOrientation? {
        return OrientationProvider.latestOrientation
    }
}
