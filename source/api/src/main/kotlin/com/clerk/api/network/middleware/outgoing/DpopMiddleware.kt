package com.clerk.api.network.middleware.outgoing

import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.configuration.DeviceBindingState
import com.clerk.api.configuration.DeviceKeyLoadTimeoutException
import com.clerk.api.configuration.DeviceTokenCnfCheck
import com.clerk.api.configuration.ProofDecision
import com.clerk.api.configuration.SignFailure
import com.clerk.api.configuration.proofDecision
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.DeviceKeyState
import com.clerk.api.dpop.BoundRequest
import com.clerk.api.dpop.DpopProof
import com.clerk.api.dpop.ServerClock
import com.clerk.api.log.ClerkLogger
import java.io.IOException
import java.util.UUID
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal const val DPOP_HEADER = "DPoP"

/** A network interceptor, because OkHttp runs those once per attempt, retries and redirects too. */
internal class DpopMiddleware(
  private val deviceBinding: DeviceBindingState,
  private val serverClock: ServerClock,
  private val deviceKeyWait: Duration,
) : Interceptor {
  private val cnfCheck = DeviceTokenCnfCheck()

  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val tokenHasCnf = cnfCheck.hasCnf(request.header(AUTHORIZATION_HEADER))
    val binding = blocking { deviceBinding.current(deviceKeyWait) }

    fun act(signFailure: SignFailure?, cause: Throwable?): Response {
      signFailure?.let(deviceBinding::recordSignFailure)
      return when (val decision = proofDecision(tokenHasCnf, binding, signFailure)) {
        ProofDecision.SendUnsigned -> chain.proceed(request)
        is ProofDecision.FailRequest ->
          throw IOException("A device-bound request needs a DPoP proof: ${decision.reason}", cause)
        is ProofDecision.Sign ->
          proofFor(decision.holder, request)
            .fold(
              onSuccess = {
                deviceBinding.recordSigned()
                chain.proceed(request.newBuilder().header(DPOP_HEADER, it).build())
              },
              onFailure = { act(decision.holder.signFailure(it), it) },
            )
      }
    }

    return act(signFailure = null, cause = null)
  }

  private fun proofFor(holder: DeviceKeyHolder, request: Request): Result<String> {
    val jti = UUID.randomUUID().toString()
    return try {
        blocking { deviceBinding.loadKey(holder, deviceKeyWait) }
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

  private fun <T> blocking(block: suspend () -> T): T =
    try {
      runBlocking { block() }
    } catch (e: CancellationException) {
      throw IOException("Clerk was reset while this request waited for the device key", e)
    }
}

private fun DeviceKeyHolder.signFailure(cause: Throwable): SignFailure =
  when {
    cause is DeviceKeyLoadTimeoutException -> SignFailure.TIMEOUT
    loadedState() is DeviceKeyState.Unavailable -> SignFailure.KEY_LOST
    else -> SignFailure.NOT_A_LOSS
  }

private const val JTI_LOG_LENGTH = 8
