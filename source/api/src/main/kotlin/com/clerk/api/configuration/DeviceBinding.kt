package com.clerk.api.configuration

import com.auth0.android.jwt.JWT
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.DeviceKeyState
import com.clerk.api.log.ClerkLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull

internal sealed interface DeviceBinding {
  data class Signing(val holder: DeviceKeyHolder) : DeviceBinding

  data class NotSigning(val reason: Reason) : DeviceBinding

  enum class Reason {
    SHARED_SESSION_SYNC,
    KEY_UNAVAILABLE,
    TIMEOUT,
  }
}

internal val DEVICE_KEY_WAIT: Duration = 5.seconds

internal sealed interface ProofDecision {
  data class Sign(val holder: DeviceKeyHolder) : ProofDecision

  data object SendUnsigned : ProofDecision

  data class FailRequest(val reason: Reason) : ProofDecision

  enum class Reason {
    KEY_NOT_READY,
    KEY_ERROR,
  }
}

internal enum class SignFailure {
  KEY_LOST,
  NOT_A_LOSS,
}

internal fun proofDecision(
  tokenHasCnf: Boolean,
  binding: DeviceBinding,
  signFailure: SignFailure?,
): ProofDecision {
  val transientReason =
    when (binding) {
      is DeviceBinding.Signing ->
        when (signFailure) {
          null -> return ProofDecision.Sign(binding.holder)
          SignFailure.KEY_LOST -> null
          SignFailure.NOT_A_LOSS -> ProofDecision.Reason.KEY_ERROR
        }
      is DeviceBinding.NotSigning ->
        when (binding.reason) {
          DeviceBinding.Reason.SHARED_SESSION_SYNC,
          DeviceBinding.Reason.KEY_UNAVAILABLE -> null
          DeviceBinding.Reason.TIMEOUT -> ProofDecision.Reason.KEY_NOT_READY
        }
    }
  return if (tokenHasCnf && transientReason != null) {
    ProofDecision.FailRequest(transientReason)
  } else {
    ProofDecision.SendUnsigned
  }
}

internal class DeviceTokenCnfCheck {
  @Volatile private var last: CheckedToken? = null

  fun hasCnf(token: String?): Boolean {
    if (token == null) return false
    val checked =
      last?.takeIf { it.token == token }
        ?: CheckedToken(token, isDeviceBound(token)).also { last = it }
    return checked.hasCnf
  }

  private data class CheckedToken(val token: String, val hasCnf: Boolean)
}

internal fun isDeviceBound(token: String): Boolean = runCatching {
  CONFIRMATION_CLAIM in JWT(token).claims
}
  .getOrDefault(false)

internal suspend fun Deferred<DeviceBinding>.awaitWithin(wait: Duration): DeviceBinding =
  withTimeoutOrNull(wait) { await() } ?: DeviceBinding.NotSigning(DeviceBinding.Reason.TIMEOUT)

internal fun CoroutineScope.resolveDeviceBinding(
  sharedSessionSync: Boolean,
  holder: DeviceKeyHolder,
): Deferred<DeviceBinding> {
  if (sharedSessionSync) {
    return CompletableDeferred(
      logged(DeviceBinding.NotSigning(DeviceBinding.Reason.SHARED_SESSION_SYNC))
    )
  }
  val keyState = async(Dispatchers.IO) { loadKeyState(holder) }
  return async {
    logged(
      when (keyState.await()) {
        is DeviceKeyState.Unavailable ->
          DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)
        is DeviceKeyState.Ready,
        null -> DeviceBinding.Signing(holder)
      }
    )
  }
}

private fun loadKeyState(holder: DeviceKeyHolder): DeviceKeyState? =
  try {
    holder.state()
  } catch (e: Exception) {
    ClerkLogger.w("Device key load failed, each request retries it: ${e.javaClass.name}")
    null
  }

private fun logged(binding: DeviceBinding): DeviceBinding = binding.also {
  ClerkLogger.i("Device binding: $it")
}

private const val CONFIRMATION_CLAIM = "cnf"
