package com.clerk.api.locale

import com.clerk.api.log.ClerkLog
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A singleton object responsible for providing the device's current locale.
 *
 * This provider detects and stores the system's default locale as a BCP 47 language tag. It exposes
 * the locale as a `StateFlow` so that other parts of the application can react to locale changes,
 * for example, by requesting localized resources from an API.
 *
 * The locale is fetched upon initialization and can be manually refreshed. It also provides a
 * cleanup mechanism to reset the locale value.
 */
internal object LocaleProvider {

  private val _locale = MutableStateFlow<String?>(null)
  val locale = _locale.asStateFlow()

  /**
   * Initializes the LocaleProvider by fetching and setting the current device locale. This should
   * be called once when the application starts or when the locale-dependent components are first
   * needed.
   */
  fun initialize() {
    refresh()
  }

  /**
   * Refreshes the current locale.
   *
   * This function attempts to get the default system locale, convert it to an IETF BCP 47 language
   * tag, and then updates the internal locale state. If an error occurs during this process, it is
   * logged.
   */
  fun refresh() {
    try {
      val currentLocale = Locale.getDefault().toLanguageTag()
      _locale.value = currentLocale
    } catch (e: Exception) {
      ClerkLog.e("Failed to refresh locale, ${e.localizedMessage}")
    }
  }

  /**
   * Clears the stored locale information. This is typically called during a sign-out process to
   * reset the state.
   */
  fun cleanup() {
    _locale.value = null
  }
}
