package com.clerk.e2e

import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import android.view.View
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay

internal sealed interface TotpTypingState {
  data object Idle : TotpTypingState

  data object Typed : TotpTypingState

  data class Failed(val message: String) : TotpTypingState
}

internal object E2ETotpTyper {
  private const val MINIMUM_SECONDS_REMAINING = 8L
  private const val MILLIS_PER_SECOND = 1_000L

  suspend fun typeCodeFromClipboardSecret(context: Context, rootView: View): TotpTypingState =
    try {
      typeCode(context, rootView)
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (error: Exception) {
      TotpTypingState.Failed(error.message ?: "Could not type the TOTP code.")
    }

  private suspend fun typeCode(context: Context, rootView: View): TotpTypingState {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val clipboardText =
      clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)
    val secret =
      clipboardText?.toString()?.let(Totp::secretFrom)
        ?: return TotpTypingState.Failed("Clipboard does not contain a TOTP secret.")

    val remaining = Totp.secondsRemaining(nowEpochSeconds())
    if (remaining < MINIMUM_SECONDS_REMAINING) {
      delay((remaining + 1) * MILLIS_PER_SECOND)
    }

    // Test runners can't read the app's clipboard on Android 10+, so the code is typed into the
    // focused field from inside the app instead of being handed back for pasting.
    Totp.code(secret, nowEpochSeconds()).forEach { digit ->
      val keyCode = KeyEvent.KEYCODE_0 + digit.digitToInt()
      rootView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
      rootView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
    return TotpTypingState.Typed
  }

  private fun nowEpochSeconds(): Long = System.currentTimeMillis() / MILLIS_PER_SECOND
}
