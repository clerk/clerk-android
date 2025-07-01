package com.clerk.configuration.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.clerk.Clerk
import com.clerk.log.ClerkLog

/**
 * Internal utility for monitoring application lifecycle events.
 *
 * This object listens to the application's lifecycle events (foreground/background transitions)
 * and executes a callback when the app returns to the foreground after being backgrounded.
 * This is useful for triggering operations like token refresh or session validation when
 * the user returns to the app.
 *
 * The listener uses Android's ProcessLifecycleOwner to monitor the entire application's
 * lifecycle rather than individual activity lifecycles.
 *
 * This is an internal utility and should not be used outside the Clerk SDK.
 */
internal object AppLifecycleListener {
  /** The callback function to execute when the app returns to foreground after being backgrounded */
  private var callback: () -> Unit = {}

  /**
   * The lifecycle observer that monitors app foreground/background state changes.
   *
   * This observer tracks whether the app was previously backgrounded and executes
   * the configured callback when the app returns to the foreground.
   */
  private val listener =
    object : DefaultLifecycleObserver {
      /** Flag to track if the app was previously in the background */
      var wasBackgrounded = false

      /**
       * Called when the application moves to the foreground.
       *
       * If the app was previously backgrounded, this method executes the configured
       * callback to handle any necessary operations for the foreground transition.
       *
       * @param owner The lifecycle owner (typically the process)
       */
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

      /**
       * Called when the application moves to the background.
       *
       * This method sets the backgrounded flag to track that the app has been
       * moved to the background state.
       *
       * @param owner The lifecycle owner (typically the process)
       */
      override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (Clerk.debugMode) {
          ClerkLog.d("AppLifecycleListener, onStop")
        }
        wasBackgrounded = true
      }
    }

  /**
   * Configures the lifecycle listener with a callback function.
   *
   * This method sets up the lifecycle observer to monitor the application's lifecycle
   * and executes the provided callback when the app returns to the foreground after
   * being backgrounded.
   *
   * @param callback The function to execute when the app returns to foreground after being backgrounded
   */
  fun configure(callback: () -> Unit) {
    this.callback = callback
    ProcessLifecycleOwner.get().lifecycle.addObserver(listener)
  }
}
