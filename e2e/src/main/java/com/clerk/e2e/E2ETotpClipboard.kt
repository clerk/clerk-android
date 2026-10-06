package com.clerk.e2e

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.delay

internal sealed interface TotpClipboardState {
  data object Idle : TotpClipboardState

  data object Ready : TotpClipboardState

  data class Failed(val message: String) : TotpClipboardState
}

internal object E2ETotpClipboard {
  private const val MINIMUM_SECONDS_REMAINING = 8L
  private const val MILLIS_PER_SECOND = 1_000L

  suspend fun replaceSecretWithCode(context: Context): TotpClipboardState {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val clipboardText =
      clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)
    val secret =
      clipboardText?.toString()?.let(Totp::secretFrom)
        ?: return TotpClipboardState.Failed("Clipboard does not contain a TOTP secret.")

    val remaining = Totp.secondsRemaining(nowEpochSeconds())
    if (remaining < MINIMUM_SECONDS_REMAINING) {
      delay((remaining + 1) * MILLIS_PER_SECOND)
    }

    val code = Totp.code(secret, nowEpochSeconds())
    clipboard.setPrimaryClip(ClipData.newPlainText("TOTP code", code))
    return TotpClipboardState.Ready
  }

  private fun nowEpochSeconds(): Long = System.currentTimeMillis() / MILLIS_PER_SECOND
}
