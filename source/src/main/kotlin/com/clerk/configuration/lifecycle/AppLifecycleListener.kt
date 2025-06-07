package com.clerk.configuration.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.clerk.Clerk
import com.clerk.log.ClerkLog

internal object AppLifecycleListener {
  private var callback: () -> Unit = {}

  private val listener =
    object : DefaultLifecycleObserver {
      var wasBackgrounded = false

      override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (Clerk.debugMode) {
          ClerkLog.d("AppLifecycleListener, onStart")
        }
        if (wasBackgrounded) {
          callback()
        }
        wasBackgrounded = false
      }

      override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (Clerk.debugMode) {
          ClerkLog.d("AppLifecycleListener, onStop")
        }
        wasBackgrounded = true
      }
    }

  fun configure(callback: () -> Unit) {
    this.callback = callback
    ProcessLifecycleOwner.get().lifecycle.addObserver(listener)
  }
}
