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
      @Volatile var wasBackgrounded = false

      override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (Clerk.debugMode) {
          ClerkLog.d("AppLifecycleListener, onStart")
        }
        // Clear the flag before notifying: the callback restarts the token refresh loop, which
        // exits as soon as it sees isInBackground == true.
        val returningFromBackground = wasBackgrounded
        wasBackgrounded = false
        if (returningFromBackground) {
          callback()
        }
      }

      override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (Clerk.debugMode) {
          ClerkLog.d("AppLifecycleListener, onStop")
        }
        wasBackgrounded = true
      }
    }

  /**
   * True between the process lifecycle's ON_STOP and the next ON_START. A process that was launched
   * without ever reaching the foreground reports false, as before this flag existed.
   */
  val isInBackground: Boolean
    get() = listener.wasBackgrounded

  fun configure(callback: () -> Unit) {
    this.callback = callback
    listenerGeneration += 1
    val generation = listenerGeneration
    if (isListening) {
      return
    }

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
