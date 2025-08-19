package com.doublethinksolutions.osp.data

data class DeviceOrientation(
    val azimuth: Float, // Degrees from North (0-360)
    val pitch: Float,   // Degrees of tilt up/down (-90 to 90)
    val roll: Float     // Degrees of tilt left/right (-180 to 180)
)
