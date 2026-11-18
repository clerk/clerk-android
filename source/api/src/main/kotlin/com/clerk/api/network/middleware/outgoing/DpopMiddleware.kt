package com.clerk.api.network.middleware.outgoing

import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.configuration.DeviceBindingState
import com.clerk.api.dpop.DpopSigner
import com.clerk.api.dpop.ServerClock
import kotlin.time.Duration
import okhttp3.Interceptor
import okhttp3.Response

internal const val DPOP_HEADER = "DPoP"

/** A network interceptor, because OkHttp runs those once per attempt, retries and redirects too. */
internal class DpopMiddleware(
  deviceBinding: DeviceBindingState,
  serverClock: ServerClock,
  deviceKeyWait: Duration,
) : Interceptor {
  private val signer = DpopSigner(deviceBinding, serverClock, deviceKeyWait)

  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val proof =
      signer.proof(request.method, request.url, request.header(AUTHORIZATION_HEADER))
        ?: return chain.proceed(request)
    return chain.proceed(request.newBuilder().header(DPOP_HEADER, proof).build())
  }
}
