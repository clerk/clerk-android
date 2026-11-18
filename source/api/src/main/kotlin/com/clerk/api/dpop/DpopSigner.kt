package com.clerk.api.dpop

import com.clerk.api.configuration.DeviceBindingState
import com.clerk.api.configuration.DeviceKeyLoadTimeoutException
import com.clerk.api.configuration.DeviceTokenCnfCheck
import com.clerk.api.configuration.ProofDecision
import com.clerk.api.configuration.SignFailure
import com.clerk.api.configuration.proofDecision
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.DeviceKeyState
import com.clerk.api.log.ClerkLogger
import java.io.IOException
import java.util.UUID
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl

/** Thrown when a request must carry a proof that the device key cannot sign yet. */
internal class DeviceBindingProofUnavailableException(
  reason: ProofDecision.Reason,
  cause: Throwable?,
) : IOException("A device-bound request needs a DPoP proof: $reason", cause)

internal class DpopSigner(
  private val deviceBinding: DeviceBindingState,
  private val serverClock: ServerClock,
  private val deviceKeyWait: Duration,
) {
  private val cnfCheck = DeviceTokenCnfCheck()

  /**
   * Returns a fresh proof for a request, or null when the request goes unsigned. Blocks while the
   * device key loads.
   *
   * @throws DeviceBindingProofUnavailableException when the token is bound and no proof can be
   *   signed.
   */
  fun proof(method: String, url: HttpUrl, authorization: String?): String? {
    val tokenHasCnf = cnfCheck.hasCnf(authorization)
    val binding = blocking { deviceBinding.current(deviceKeyWait) }
    var signFailure: SignFailure? = null
    var cause: Throwable? = null
    while (true) {
      signFailure?.let(deviceBinding::recordSignFailure)
      when (val decision = proofDecision(tokenHasCnf, binding, signFailure)) {
        ProofDecision.SendUnsigned -> return null
        is ProofDecision.FailRequest ->
          throw DeviceBindingProofUnavailableException(decision.reason, cause)
        is ProofDecision.Sign ->
          sign(decision.holder, BoundRequest(method, url, authorization))
            .onSuccess {
              deviceBinding.recordSigned()
              return it
            }
            .onFailure {
              signFailure = decision.holder.signFailure(it)
              cause = it
            }
      }
    }
  }

  private fun sign(holder: DeviceKeyHolder, request: BoundRequest): Result<String> {
    val jti = UUID.randomUUID().toString()
    return try {
        blocking { deviceBinding.loadKey(holder, deviceKeyWait) }
        holder.withKey { key ->
          DpopProof.build(key = key, request = request, now = serverClock.now(), jti = jti)
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
