package com.doublethinksolutions.osp

import android.app.Application
import com.doublethinksolutions.osp.network.NetworkClient

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the network client with the application context
        NetworkClient.initialize(this)
    }
}
