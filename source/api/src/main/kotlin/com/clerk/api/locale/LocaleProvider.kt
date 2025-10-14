package com.clerk.api.locale

import android.content.Context
import com.clerk.api.log.ClerkLog
import java.lang.ref.WeakReference
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the user's locale for the Clerk SDK.
 *
 * This class provides access to the current device locale in BCP-47 format, which is used for
 * sending localized emails (such as verification codes) during authentication flows.
 *
 * The locale is automatically initialized when the SDK is configured and refreshed when the app
 * starts, ensuring that locale changes are picked up.
 */
internal object LocaleProvider {

  /** Weak reference to application context to prevent memory leaks. */
  private var context: WeakReference<Context>? = null

  /** Internal mutable state flow for locale changes. */
  private val _localeFlow = MutableStateFlow<String?>(null)

  /**
   * Reactive state for the current device locale in BCP-47 format.
   *
   * Observe this StateFlow to react to locale changes. The locale is automatically updated when
   * the SDK is initialized and when the app starts.
   *
   * Examples of BCP-47 format: "en-US", "pt-BR", "es-ES", "fr-FR"
   */
  val localeFlow: StateFlow<String?> = _localeFlow.asStateFlow()

  /**
   * The current device locale in BCP-47 format.
   *
   * Returns the locale as a string in BCP-47 format (e.g., "en-US", "pt-BR"), or null if the
   * locale has not been initialized yet.
   */
  val locale: String?
    get() = _localeFlow.value

  /**
   * Initializes the LocaleProvider with the application context.
   *
   * This should be called once during SDK initialization. It captures the current device locale
   * and makes it available for authentication requests.
   *
   * @param context The application context used to access system resources and locale information.
   */
  fun initialize(context: Context) {
    this.context = WeakReference(context.applicationContext)
    refresh()
  }

  /**
   * Refreshes the current locale from the device.
   *
   * This method should be called when the app starts to pick up any locale changes that may have
   * occurred while the app was not running. It's automatically called during SDK initialization and
   * on app lifecycle events.
   */
  fun refresh() {
    try {
      val currentLocale = getDeviceLocale()
      _localeFlow.value = currentLocale
      ClerkLog.d("Locale refreshed: $currentLocale")
    } catch (e: Exception) {
      ClerkLog.e("Failed to refresh locale: ${e.message}")
    }
  }

  /**
   * Gets the current device locale in BCP-47 format.
   *
   * This method uses the device's default locale and converts it to BCP-47 format using
   * [Locale.toLanguageTag]. The BCP-47 format is the standard format for locale identifiers and
   * includes language and region information (e.g., "en-US" for English in the United States).
   *
   * @return The device locale in BCP-47 format, or null if unable to determine.
   */
  private fun getDeviceLocale(): String? {
    return try {
      val locale = Locale.getDefault()
      locale.toLanguageTag()
    } catch (e: Exception) {
      ClerkLog.e("Failed to get device locale: ${e.message}")
      null
    }
  }

  /**
   * Clears the locale state and releases the context reference.
   *
   * This should be called when the SDK is being cleaned up or re-initialized.
   */
  fun cleanup() {
    _localeFlow.value = null
    context = null
    ClerkLog.d("LocaleProvider cleaned up")
  }
}
