package com.clerk.prebuiltui

import android.app.Activity
import android.app.Application
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfiguration
import com.clerk.api.connect
import java.lang.ref.WeakReference
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrebuiltUiApplication : Application() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val _connection = MutableStateFlow<Result<Clerk>?>(null)
  val connection = _connection.asStateFlow()
  private val _callbackError = MutableStateFlow<String?>(null)
  val callbackError = _callbackError.asStateFlow()

  fun clearCallbackError() { _callbackError.value = null }

  fun handleCallback(url: String) {
    scope.launch {
      try {
        val clerk = connection.filterNotNull().first { it.isSuccess }.getOrThrow()
        clerk.handleAuthCallback(URI(url))
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        _callbackError.value = error.localizedMessage ?: "Unable to complete authentication"
      }
    }
  }

  private var connecting = false
  private var activity = WeakReference<Activity>(null)

  override fun onCreate() {
    super.onCreate()
    connect()
  }

  fun setActivity(value: Activity) { activity = WeakReference(value) }
  fun clearActivity(value: Activity) {
    if (activity.get() === value) activity.clear()
  }

  fun connect() {
    if (connecting || _connection.value?.isSuccess == true) return
    connecting = true
    _connection.value = null
    scope.launch {
      try {
        _connection.value = Result.success(
          Clerk.connect(
            this@PrebuiltUiApplication,
            ClerkConfiguration(
              BuildConfig.PREBUILT_UI_CLERK_PUBLISHABLE_KEY,
              "$packageName.clerk://oauth/callback",
            ),
            activity = { activity.get() },
          )
        )
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        _connection.value = Result.failure(error)
      } finally {
        connecting = false
      }
    }
  }
}
