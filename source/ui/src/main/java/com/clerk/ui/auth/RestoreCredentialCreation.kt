package com.clerk.ui.auth

import androidx.annotation.VisibleForTesting
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.ui.core.log.ClerkLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal object RestoreCredentialCreation {

  @VisibleForTesting internal var scope: CoroutineScope = backgroundScope()

  private fun backgroundScope(dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope =
    CoroutineScope(SupervisorJob() + dispatcher)

  private val logUnexpectedFailure = CoroutineExceptionHandler { _, throwable ->
    ClerkLog.w("Restore credential creation failed: ${throwable.message}")
  }

  fun createInBackground() {
    scope.launch(logUnexpectedFailure) {
      val result = Clerk.restoreCredentials.createIfMissing()
      if (result is ClerkResult.Failure) {
        ClerkLog.w("Restore credential creation failed: ${result.errorMessage}")
      }
    }
  }
}
