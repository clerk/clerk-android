package com.clerk.api.log

import android.net.Uri

internal object SafeUriLog {
  fun describe(uri: Uri?): String {
    if (uri == null) return "uri=null"

    val queryKeys = queryParamKeys(uri)
    val fragmentKeys = fragmentParamKeys(uri.encodedFragment)
    val allKeys = (queryKeys + fragmentKeys).sorted()

    val port = if (uri.port == -1) "-" else uri.port.toString()
    val path = uri.path ?: "-"
    val scheme = uri.scheme ?: "-"
    val host = uri.host ?: "-"

    return buildString {
      append("scheme=$scheme")
      append(", host=$host")
      append(", port=$port")
      append(", path=$path")
      append(", query_keys=")
      append(queryKeys.sorted())
      append(", fragment_keys=")
      append(fragmentKeys.sorted())
      append(", has_flow_id=")
      append("flow_id" in allKeys)
      append(", has_approval_token=")
      append("approval_token" in allKeys)
      append(", has_token=")
      append("token" in allKeys)
      append(", has_rotating_token_nonce=")
      append("rotating_token_nonce" in allKeys)
    }
  }

  private fun queryParamKeys(uri: Uri): Set<String> =
    runCatching { uri.queryParameterNames.map { it.trim() }.filter { it.isNotEmpty() }.toSet() }
      .getOrDefault(emptySet())

  private fun fragmentParamKeys(fragment: String?): Set<String> {
    if (fragment.isNullOrBlank()) return emptySet()

    return fragment
      .split("&")
      .mapNotNull { entry ->
        val separator = entry.indexOf("=")
        val rawKey = if (separator >= 0) entry.substring(0, separator) else entry
        val key = Uri.decode(rawKey).trim()
        key.takeIf { it.isNotEmpty() }
      }
      .toSet()
  }
}
