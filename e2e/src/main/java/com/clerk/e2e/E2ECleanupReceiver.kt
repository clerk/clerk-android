package com.clerk.e2e

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clerk.api.Clerk
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class E2ECleanupReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_CLEANUP_ACCOUNT) return
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
      var status: CleanupStatus = CleanupStatus.Failed("Account cleanup was cancelled.")
      try {
        status = cleanUpWithinTimeout()
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (error: Exception) {
        status = failed(error.message ?: error.toString())
      } finally {
        pendingResult.resultCode =
          if (status == CleanupStatus.Complete) Activity.RESULT_OK else Activity.RESULT_CANCELED
        pendingResult.resultData = status.toString()
        pendingResult.finish()
      }
    }
  }

  private suspend fun cleanUpWithinTimeout(): CleanupStatus =
    withTimeoutOrNull(CLEANUP_TIMEOUT_MS) {
      Clerk.isInitialized.first { it }
      E2EAccountCleanup.deleteCurrentAccount()
    } ?: failed("Timed out waiting for account cleanup.")

  private fun failed(message: String): CleanupStatus.Failed =
    CleanupStatus.Failed(message).also { E2EAccountCleanup.reportFailure(it.message) }

  companion object {
    const val ACTION_CLEANUP_ACCOUNT = "com.clerk.e2e.action.CLEANUP_ACCOUNT"
    // Background broadcasts are ANR'd after 60s, so cleanup has to finish well before that.
    private const val CLEANUP_TIMEOUT_MS = 45_000L
  }
}
