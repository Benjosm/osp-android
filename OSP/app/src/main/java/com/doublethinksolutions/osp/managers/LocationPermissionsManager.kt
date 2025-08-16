package com.doublethinksolutions.osp.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * A singleton object to manage location permission checking and requesting.
 * This encapsulates the logic to keep Fragments and Activities cleaner.
 */
object LocationPermissionsManager {

    // The request code must match the one handled in the Fragment's onRequestPermissionsResult
    private const val LOCATION_PERMISSION_REQUEST_CODE = 1002

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * Checks if all required location permissions have been granted.
     *
     * @param context The context to use for checking permissions.
     * @return True if all location permissions are granted, false otherwise.
     */
    fun isLocationPermissionGranted(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Requests the necessary location permissions from the user.
     * The result of this request will be delivered to the Fragment's
     * onRequestPermissionsResult callback.
     *
     * @param fragment The fragment that is requesting the permissions. The result will be
     * delivered to this fragment.
     */
    fun requestLocationPermissions(fragment: Fragment) {
        fragment.requestPermissions(
            REQUIRED_PERMISSIONS,
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
}
