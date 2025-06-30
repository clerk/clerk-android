package com.clerk.configuration

import android.util.Base64
import com.clerk.Constants.Prefixes.TOKEN_PREFIX_LIVE
import com.clerk.Constants.Prefixes.TOKEN_PREFIX_TEST
import com.clerk.Constants.Prefixes.URL_SSL_PREFIX

internal class PublishableKeyHelper {
  /**
   * Helper function that extracts the API URL from the publishable key. This is used to initialize
   * the Clerk API and check if the API is reachable. This is done by making a request to the client
   * and environment endpoints.
   */
  internal fun extractApiUrl(publishableKey: String): String {
    val prefixRemoved =
      publishableKey
        .removePrefix(TOKEN_PREFIX_TEST)
        .removePrefix(TOKEN_PREFIX_LIVE) // Handles both test and live

    val decodedBytes = Base64.decode(prefixRemoved, Base64.DEFAULT)
    val decodedString = String(decodedBytes)

    return if (decodedString.isNotEmpty()) {
      "${URL_SSL_PREFIX}${decodedString.dropLast(1)}"
    } else {
      error("Invalid publishable key")
    }
  }
}
