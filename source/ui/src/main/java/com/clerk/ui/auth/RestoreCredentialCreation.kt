package com.clerk.ui.auth

import androidx.annotation.VisibleForTesting
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.ui.core.log.ClerkLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal object RestoreCredentialCreation {

  @VisibleForTesting
  internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun createInBackground() {
    scope.launch {
      val result = Clerk.restoreCredentials.createIfMissing()
      if (result is ClerkResult.Failure) {
        ClerkLog.w("Restore credential creation failed: ${result.errorMessage}")
      }
    }
  }
}
