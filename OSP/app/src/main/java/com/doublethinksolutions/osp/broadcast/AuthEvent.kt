package com.doublethinksolutions.osp.broadcast

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class AuthEvent {
    object TokenRefreshed : AuthEvent()
    object AuthFailure : AuthEvent()
}

object AuthEventBus {
    private val _events = MutableSharedFlow<AuthEvent>(replay = 1)
    val events = _events.asSharedFlow()

    suspend fun postEvent(event: AuthEvent) {
        _events.emit(event)
    }
}
