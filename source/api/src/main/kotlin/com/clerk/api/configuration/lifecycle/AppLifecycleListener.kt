package com.clerk.api.configuration.lifecycle

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog

internal object AppLifecycleListener {
  private var callback: () -> Unit = {}

  @Volatile private var isListening = false
  @Volatile private var listenerGeneration = 0

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
    listenerGeneration += 1
    val generation = listenerGeneration
    if (isListening) {
      return
    }

    // Ensure observer is added on the main thread
    if (Looper.myLooper() == Looper.getMainLooper()) {
      ProcessLifecycleOwner.get().lifecycle.addObserver(listener)
      isListening = true
    } else {
      Handler(Looper.getMainLooper()).post {
        if (listenerGeneration == generation) {
          ProcessLifecycleOwner.get().lifecycle.addObserver(listener)
          isListening = true
        }
      }
    }
  }

  fun stop() {
    callback = {}
    listener.wasBackgrounded = false
    listenerGeneration += 1
    val generation = listenerGeneration

    if (!isListening) {
      return
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
      ProcessLifecycleOwner.get().lifecycle.removeObserver(listener)
      isListening = false
    } else {
      Handler(Looper.getMainLooper()).post {
        if (listenerGeneration == generation) {
          ProcessLifecycleOwner.get().lifecycle.removeObserver(listener)
          isListening = false
        }
      }
    }
  }
}
