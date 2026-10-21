package com.clerk.api.configuration

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object ProxyUrlValidator {
  fun requireSecure(proxyUrl: String): String {
    require(proxyUrl == proxyUrl.trim()) { "proxyUrl must not have leading or trailing whitespace" }
    val url =
      requireNotNull(proxyUrl.toHttpUrlOrNull()) { "proxyUrl must be a full URL with a host" }
    require(url.isHttps) { "proxyUrl must use https" }
    require(url.username.isEmpty() && url.password.isEmpty()) {
      "proxyUrl must not contain credentials"
    }
    require(url.query == null && url.fragment == null) {
      "proxyUrl must not contain a query or fragment"
    }
    return proxyUrl
  }
}
