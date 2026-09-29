package com.clerk.api.configuration

import android.util.Base64
import com.clerk.api.Constants.Prefixes.TOKEN_PREFIX_LIVE
import com.clerk.api.Constants.Prefixes.TOKEN_PREFIX_TEST
import com.clerk.api.Constants.Prefixes.URL_SSL_PREFIX

internal class PublishableKeyHelper {
  internal fun extractApiUrl(publishableKey: String): String {
    val prefixRemoved =
      publishableKey
        .removePrefix(TOKEN_PREFIX_TEST)
        .removePrefix(TOKEN_PREFIX_LIVE)

    val decodedBytes = Base64.decode(prefixRemoved, Base64.DEFAULT)
    val decodedString = String(decodedBytes)

    return if (decodedString.isNotEmpty()) {
      "${URL_SSL_PREFIX}${decodedString.dropLast(1)}"
    } else {
      error("Invalid publishable key")
    }
  }

  internal fun isLive(publishableKey: String?): Boolean {
    return publishableKey != null && publishableKey.startsWith(TOKEN_PREFIX_LIVE)
  }
}
