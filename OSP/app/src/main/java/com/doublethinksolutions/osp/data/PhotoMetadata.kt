package com.doublethinksolutions.osp.data

import android.location.Location

/**
 * A data class to hold all metadata collected at the time a photo is captured.
 *
 * @property timestamp The Unix timestamp (in milliseconds) of when the capture was initiated.
 * @property location The last known device location. This can be null if location is unavailable
 *                    or permissions are not granted.
 * @property deviceOrientationDegrees The physical orientation of the device in degrees (0, 90, 180, 270).
 */
data class PhotoMetadata(
    val timestamp: Long,
    val location: Location?,
    val deviceOrientationDegrees: Int
)
