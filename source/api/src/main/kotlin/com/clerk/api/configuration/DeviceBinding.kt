package com.clerk.api.configuration

import com.auth0.android.jwt.JWT
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.DeviceKeyState
import com.clerk.api.log.ClerkLogger
import java.util.concurrent.atomic.AtomicInteger
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
  if (binding is DeviceBinding.Signing && signFailure == null) {
    return ProofDecision.Sign(binding.holder)
  }
  val gap = transientGap(binding, signFailure)
  return if (tokenHasCnf && gap != null) {
    ProofDecision.FailRequest(gap)
  } else {
    ProofDecision.SendUnsigned
  }
}

private fun transientGap(binding: DeviceBinding, signFailure: SignFailure?): ProofDecision.Reason? =
  when (binding) {
    is DeviceBinding.Signing ->
      when (signFailure) {
        null,
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

internal const val TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED = 3

internal class DeviceBindingState(private val resolved: Deferred<DeviceBinding>) {
  private val transientFailuresInARow = AtomicInteger()

  suspend fun current(wait: Duration): DeviceBinding =
    if (stoppedSigning()) {
      DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)
    } else {
      withTimeoutOrNull(wait) { resolved.await() }
        ?: DeviceBinding.NotSigning(DeviceBinding.Reason.TIMEOUT)
    }

  fun recordSigned() {
    transientFailuresInARow.updateAndGet {
      if (it >= TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED) it else 0
    }
  }

  fun recordGap(binding: DeviceBinding, signFailure: SignFailure?) {
    if (transientGap(binding, signFailure) == null) return
    if (transientFailuresInARow.incrementAndGet() == TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED) {
      ClerkLogger.w(
        "Device binding: NotSigning(reason=KEY_UNAVAILABLE) after " +
          "$TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED transient key failures in a row"
      )
    }
  }

  private fun stoppedSigning(): Boolean =
    transientFailuresInARow.get() >= TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED
}

internal fun CoroutineScope.resolveDeviceBinding(
  sharedSessionSync: Boolean,
  holder: DeviceKeyHolder,
): DeviceBindingState {
  if (sharedSessionSync) {
    return DeviceBindingState(
      CompletableDeferred(
        logged(DeviceBinding.NotSigning(DeviceBinding.Reason.SHARED_SESSION_SYNC))
      )
    )
  }
  val keyState = async(Dispatchers.IO) { loadKeyState(holder) }
  return DeviceBindingState(
    async {
      logged(
        when (keyState.await()) {
          is DeviceKeyState.Unavailable ->
            DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)
          is DeviceKeyState.Ready,
          null -> DeviceBinding.Signing(holder)
        }
      )
    }
  )
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
