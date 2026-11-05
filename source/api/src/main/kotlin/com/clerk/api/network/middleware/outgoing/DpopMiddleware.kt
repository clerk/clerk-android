package com.clerk.api.network.middleware.outgoing

import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.configuration.DeviceBinding
import com.clerk.api.configuration.DeviceTokenCnfCheck
import com.clerk.api.configuration.ProofDecision
import com.clerk.api.configuration.SignFailure
import com.clerk.api.configuration.awaitWithin
import com.clerk.api.configuration.proofDecision
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.dpop.BoundRequest
import com.clerk.api.dpop.DpopProof
import com.clerk.api.dpop.ServerClock
import com.clerk.api.log.ClerkLogger
import java.io.IOException
import java.util.UUID
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal const val DPOP_HEADER = "DPoP"

/** A network interceptor, because OkHttp runs those once per attempt, retries and redirects too. */
internal class DpopMiddleware(
  private val deviceBinding: Deferred<DeviceBinding>,
  private val serverClock: ServerClock,
  private val deviceKeyWait: Duration,
) : Interceptor {
  private val cnfCheck = DeviceTokenCnfCheck()

  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val tokenHasCnf = cnfCheck.hasCnf(request.header(AUTHORIZATION_HEADER))
    val binding = awaitBinding()

    fun act(decision: ProofDecision, cause: Throwable?): Response =
      when (decision) {
        ProofDecision.SendUnsigned -> chain.proceed(request)
        is ProofDecision.FailRequest ->
          throw IOException("A device-bound request needs a DPoP proof: ${decision.reason}", cause)
        is ProofDecision.Sign ->
          proofFor(decision.holder, request)
            .fold(
              onSuccess = { chain.proceed(request.newBuilder().header(DPOP_HEADER, it).build()) },
              onFailure = {
                act(proofDecision(tokenHasCnf, binding, decision.holder.signFailure()), it)
              },
            )
      }

    return act(proofDecision(tokenHasCnf, binding, signFailure = null), cause = null)
  }

  private fun proofFor(holder: DeviceKeyHolder, request: Request): Result<String> {
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
      } catch (e: Exception) {
        Result.failure(e)
      }
      .onSuccess { ClerkLogger.d("DPoP proof attached jti=${jti.take(JTI_LOG_LENGTH)}") }
      .onFailure { ClerkLogger.w("DPoP proof failed: ${it.javaClass.name}") }
  }

  private fun awaitBinding(): DeviceBinding =
    try {
      runBlocking { deviceBinding.awaitWithin(deviceKeyWait) }
    } catch (e: CancellationException) {
      throw IOException("Clerk was reset while this request waited for the device key", e)
    }
}

private fun DeviceKeyHolder.signFailure(): SignFailure =
  if (isUnavailable()) SignFailure.KEY_LOST else SignFailure.NOT_A_LOSS

private const val JTI_LOG_LENGTH = 8
