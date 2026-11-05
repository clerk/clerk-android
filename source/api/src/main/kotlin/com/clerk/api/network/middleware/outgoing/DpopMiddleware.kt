package com.clerk.api.network.middleware.outgoing

import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.configuration.DeviceBinding
import com.clerk.api.dpop.BoundRequest
import com.clerk.api.dpop.DpopProof
import com.clerk.api.dpop.ServerClock
import com.clerk.api.log.ClerkLogger
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal const val DPOP_HEADER = "DPoP"

/**
 * Signs every network attempt, so OkHttp's own retries and redirect follow-ups each carry a fresh
 * proof for the URL they actually reach.
 */
internal class DpopMiddleware(
  private val deviceBinding: Deferred<DeviceBinding>,
  private val serverClock: ServerClock,
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val proof = proofFor(request) ?: return chain.proceed(request)
    return chain.proceed(request.newBuilder().header(DPOP_HEADER, proof).build())
  }

  private fun proofFor(request: Request): String? {
    val holder = (awaitBinding() as? DeviceBinding.Signing)?.holder ?: return null
    val jti = UUID.randomUUID().toString()
    return try {
        holder.withKey { key ->
          DpopProof.build(
            key = key,
            request =
              BoundRequest(request.method, request.url, request.header(AUTHORIZATION_HEADER)),
            now = serverClock.now(),
            jti = jti,
          )
        }
      } catch (e: RuntimeException) {
        Result.failure(e)
      }
      .onSuccess { ClerkLogger.d("DPoP proof attached jti=${jti.take(JTI_LOG_LENGTH)}") }
      .onFailure { ClerkLogger.w("Sending without a DPoP proof: ${it.javaClass.name}") }
      .getOrNull()
  }

  private fun awaitBinding(): DeviceBinding =
    try {
      runBlocking { deviceBinding.await() }
    } catch (e: CancellationException) {
      throw IOException("Clerk was reset while this request waited for the device key", e)
    }
}

private const val JTI_LOG_LENGTH = 8
