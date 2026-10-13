package com.clerk.api.locale

import com.clerk.api.log.ClerkLog
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object LocaleProvider {

  private val _locale = MutableStateFlow<String?>(null)
  val locale = _locale.asStateFlow()

  fun initialize() {
    refresh()
  }

  fun refresh() {
    try {
      val currentLocale = Locale.getDefault().toLanguageTag()
      _locale.value = currentLocale
    } catch (e: Exception) {
      ClerkLog.e("Failed to refresh locale, ${e.localizedMessage}")
    }
  }

  fun cleanup() {
    _locale.value = null
  }
}
