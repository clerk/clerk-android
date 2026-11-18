package com.clerk.api.dpop

import kotlin.time.Instant
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Signs the Frontend API requests a framework host sends from a runtime of its own. */
internal class HostSigning(private val signer: DpopSigner, private val serverClock: ServerClock) {
  fun proof(method: String, url: String, authorization: String?): String? {
    val httpUrl = requireNotNull(url.toHttpUrlOrNull()) { "The request URL is not an http(s) URL" }
    return signer.proof(method.uppercase(), httpUrl, authorization)
  }

  fun recordServerDate(date: String) {
    val serverDate =
      runCatching { Headers.Builder().add(DATE_HEADER, date).build().getDate(DATE_HEADER) }
        .getOrNull() ?: return
    serverClock.record(Instant.fromEpochMilliseconds(serverDate.time))
  }
}

private const val DATE_HEADER = "Date"
