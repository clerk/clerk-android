package com.clerk.api.configuration

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

internal fun CoroutineScope.resolveDeviceBinding(
  sharedSessionSync: Boolean,
  holder: DeviceKeyHolder,
  keyTimeout: Duration = 5.seconds,
): Deferred<DeviceBinding> {
  if (sharedSessionSync) {
    return CompletableDeferred(
      logged(DeviceBinding.NotSigning(DeviceBinding.Reason.SHARED_SESSION_SYNC))
    )
  }
  val keyState = async(Dispatchers.IO) { loadKeyState(holder) }
  return async {
    logged(
      when (withTimeoutOrNull(keyTimeout) { keyState.await() }) {
        is DeviceKeyState.Ready -> DeviceBinding.Signing(holder)
        is DeviceKeyState.Unavailable ->
          DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)
        null -> DeviceBinding.NotSigning(DeviceBinding.Reason.TIMEOUT)
      }
    )
  }
}

private fun loadKeyState(holder: DeviceKeyHolder): DeviceKeyState =
  try {
    holder.state()
  } catch (e: Exception) {
    DeviceKeyState.Unavailable(e)
  }

private fun logged(binding: DeviceBinding): DeviceBinding = binding.also {
  ClerkLogger.i("Device binding: $it")
}
