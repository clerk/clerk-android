package com.clerk.api.configuration

import com.auth0.android.jwt.JWT
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.DeviceKeyState
import com.clerk.api.log.ClerkLogger
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

internal val KEY_LOAD_GIVE_UP: Duration = 15.seconds

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
  TIMEOUT,
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
        SignFailure.TIMEOUT -> ProofDecision.Reason.KEY_NOT_READY
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

internal class DeviceKeyLoadTimeoutException(wait: Duration) :
  IOException("The device key did not load within $wait")

private sealed interface KeyLoad {
  data class Pending(val startedAt: TimeMark, val timedOutOnce: Boolean) : KeyLoad

  data object GaveUp : KeyLoad

  data object Ready : KeyLoad
}

internal class DeviceBindingState(
  private val resolved: Deferred<DeviceBinding>,
  private val timeSource: TimeSource = TimeSource.Monotonic,
  ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val transientFailuresInARow = AtomicInteger()
  private val keyLoads = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val bindingLoad = WatchedLoad(resolved)
  private var requestTimeLoad: WatchedLoad<DeviceKeyState>? = null

  suspend fun current(wait: Duration): DeviceBinding {
    val binding = if (!stoppedSigning() && bindingLoad.awaitWithin(wait)) resolved.await() else null
    return when {
      stoppedSigning() -> DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)
      binding == null -> DeviceBinding.NotSigning(DeviceBinding.Reason.TIMEOUT)
      binding is DeviceBinding.Signing &&
        binding.holder.loadedState() is DeviceKeyState.Unavailable ->
        DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)
      else -> binding
    }
  }

  suspend fun loadKey(holder: DeviceKeyHolder, wait: Duration) {
    if (holder.loadedState() != null) return
    val load = requestTimeLoadOf(holder)
    if (!load.awaitWithin(wait)) throw DeviceKeyLoadTimeoutException(wait)
    load.result.await()
  }

  fun recordSigned() = resetTransientFailures()

  fun recordSignFailure(failure: SignFailure) {
    if (failure == SignFailure.NOT_A_LOSS) countTransientFailure()
  }

  private fun resetTransientFailures() {
    if (transientFailuresInARow.getAndSet(0) >= TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED) {
      ClerkLogger.i("Device binding: the device key loaded, signing again")
    }
  }

  private fun countTransientFailure() {
    if (transientFailuresInARow.incrementAndGet() == TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED) {
      ClerkLogger.w(
        "Device binding: NotSigning(reason=KEY_UNAVAILABLE) after " +
          "$TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED transient key failures in a row"
      )
    }
  }

  private fun giveUpOnKeyLoad() {
    val before = transientFailuresInARow.getAndUpdate {
      maxOf(it, TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED)
    }
    if (before < TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED) {
      ClerkLogger.w(
        "Device binding: NotSigning(reason=KEY_UNAVAILABLE) after a key load " +
          "still pending at $KEY_LOAD_GIVE_UP"
      )
    }
  }

  private fun stoppedSigning(): Boolean =
    transientFailuresInARow.get() >= TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED

  private fun requestTimeLoadOf(holder: DeviceKeyHolder): WatchedLoad<DeviceKeyState> =
    synchronized(this) {
      requestTimeLoad?.takeUnless { it.result.isCompleted }
        ?: WatchedLoad(keyLoads.async { holder.state() }).also { requestTimeLoad = it }
    }

  private inner class WatchedLoad<T>(val result: Deferred<T>) {
    private var load: KeyLoad = KeyLoad.Pending(timeSource.markNow(), timedOutOnce = false)

    init {
      result.invokeOnCompletion { advance(afterWait = false) }
    }

    suspend fun awaitWithin(wait: Duration): Boolean {
      val before = advance(afterWait = false)
      if (before is KeyLoad.Pending && !before.timedOutOnce) {
        withTimeoutOrNull(wait) { result.join() }
      }
      return advance(afterWait = true) == KeyLoad.Ready
    }

    private fun advance(afterWait: Boolean): KeyLoad =
      synchronized(this) {
        val current = load
        load =
          when {
            current == KeyLoad.Ready -> current
            result.isCompleted ->
              KeyLoad.Ready.also { if (!result.isCancelled) resetTransientFailures() }
            current !is KeyLoad.Pending -> current
            current.startedAt.elapsedNow() >= KEY_LOAD_GIVE_UP ->
              KeyLoad.GaveUp.also { giveUpOnKeyLoad() }
            afterWait && !current.timedOutOnce ->
              current.copy(timedOutOnce = true).also { countTransientFailure() }
            else -> current
          }
        load
      }
  }
}

internal fun CoroutineScope.resolveDeviceBinding(
  sharedSessionSync: Boolean,
  holder: DeviceKeyHolder,
  ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): DeviceBindingState {
  if (sharedSessionSync) {
    return DeviceBindingState(
      CompletableDeferred(
        logged(DeviceBinding.NotSigning(DeviceBinding.Reason.SHARED_SESSION_SYNC))
      ),
      ioDispatcher = ioDispatcher,
    )
  }
  val keyState = async(ioDispatcher) { holder.state() }
  return DeviceBindingState(
    async {
      logged(
        when (keyState.await()) {
          is DeviceKeyState.Unavailable ->
            DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)
          is DeviceKeyState.Ready -> DeviceBinding.Signing(holder)
        }
      )
    },
    ioDispatcher = ioDispatcher,
  )
}

private fun logged(binding: DeviceBinding): DeviceBinding = binding.also {
  ClerkLogger.i("Device binding: $it")
}

private const val CONFIRMATION_CLAIM = "cnf"
