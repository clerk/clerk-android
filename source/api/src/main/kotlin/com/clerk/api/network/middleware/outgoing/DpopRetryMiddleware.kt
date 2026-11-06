package com.clerk.api.network.middleware.outgoing

import com.clerk.api.log.ClerkLogger
import com.clerk.api.network.middleware.DeviceBindingError
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response

internal class DpopRetryMiddleware(private val json: Json) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val response = chain.proceed(request)
    if (!response.rejectsSignedProofWithServerTime()) return response
    response.close()
    ClerkLogger.d("Retrying with a DPoP proof dated by the server clock")
    return chain.proceed(request)
  }

  private fun Response.rejectsSignedProofWithServerTime(): Boolean =
    request.body?.isOneShot() != true &&
      networkResponse?.request?.header(DPOP_HEADER) != null &&
      headers.getDate(DATE_HEADER) != null &&
      DeviceBindingError.of(this, json)?.recovery ==
        DeviceBindingError.Recovery.RETRY_WITH_SERVER_CLOCK
}

private const val DATE_HEADER = "Date"
