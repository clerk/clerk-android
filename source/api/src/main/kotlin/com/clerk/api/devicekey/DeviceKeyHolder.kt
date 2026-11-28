package com.clerk.api.devicekey

import com.clerk.api.log.ClerkLogger

internal class DeviceKeyHolder(private val loadKey: () -> DeviceKey) {
  @Volatile private var state: DeviceKeyState? = null

  fun state(): DeviceKeyState = state ?: synchronized(this) { state ?: load() }

  fun <T> withKey(operation: (DeviceKey) -> T): Result<T> {
    val current = state()
    val result = current.attempt(operation)
    if (current !is DeviceKeyState.Ready || result.exceptionOrNull() !is DeviceKeyLostException) {
      return result
    }
    return replace(current.key).attempt(operation).onFailure {
      if (it is DeviceKeyLostException) {
        giveUp(it)
      }
    }
  }

  private fun replace(lost: DeviceKey): DeviceKeyState =
    synchronized(this) {
      val current = state
      if (current is DeviceKeyState.Ready && current.key === lost) {
        catchingKeystoreFailures { lost.delete() }
          .fold(onSuccess = { load() }, onFailure = { giveUp(it) })
      } else {
        current ?: load()
      }
    }

  private fun load(): DeviceKeyState =
    record(
      catchingKeystoreFailures(loadKey)
        .fold(
          onSuccess = { DeviceKeyState.Ready(it) },
          onFailure = { DeviceKeyState.Unavailable(it) },
        )
    )

  private fun giveUp(cause: Throwable): DeviceKeyState =
    synchronized(this) {
      state as? DeviceKeyState.Unavailable ?: record(DeviceKeyState.Unavailable(cause))
    }

  private fun record(next: DeviceKeyState): DeviceKeyState {
    when (next) {
      is DeviceKeyState.Ready ->
        ClerkLogger.i("Device key ready level=${next.key.securityLevel.wireName}")
      is DeviceKeyState.Unavailable -> ClerkLogger.w("Device key unavailable: ${next.cause}")
    }
    state = next
    return next
  }
}

private fun <T> DeviceKeyState.attempt(operation: (DeviceKey) -> T): Result<T> =
  when (this) {
    is DeviceKeyState.Ready -> catchingKeystoreFailures { operation(key) }
    is DeviceKeyState.Unavailable -> Result.failure(cause)
  }
