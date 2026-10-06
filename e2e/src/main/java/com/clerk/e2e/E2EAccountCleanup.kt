package com.clerk.e2e

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.user.delete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal sealed interface CleanupStatus {
  data object Idle : CleanupStatus

  data object InProgress : CleanupStatus

  data object Complete : CleanupStatus

  data class Failed(val message: String) : CleanupStatus
}

internal object E2EAccountCleanup {
  private const val MAX_FAILURE_DETAIL_LENGTH = 240

  private val _status = MutableStateFlow<CleanupStatus>(CleanupStatus.Idle)
  val status = _status.asStateFlow()

  suspend fun deleteCurrentAccount(): CleanupStatus {
    _status.value = CleanupStatus.InProgress
    val user = Clerk.user
    val result =
      if (user == null) {
        CleanupStatus.Complete
      } else {
        when (val deleteResult = user.delete()) {
          is ClerkResult.Success -> {
            Clerk.auth.signOut()
            CleanupStatus.Complete
          }
          is ClerkResult.Failure -> CleanupStatus.Failed(failureMessage(deleteResult.errorMessage))
        }
      }
    _status.value = result
    return result
  }

  fun reportFailure(message: String) {
    _status.value = CleanupStatus.Failed(failureMessage(message))
  }

  private fun failureMessage(detail: String?): String {
    val trimmed = detail?.trim().orEmpty()
    if (trimmed.isEmpty()) return "Cleanup failed while deleting the current account."
    val truncated =
      if (trimmed.length > MAX_FAILURE_DETAIL_LENGTH) {
        trimmed.take(MAX_FAILURE_DETAIL_LENGTH) + "..."
      } else {
        trimmed
      }
    return "Cleanup failed while deleting the current account: $truncated"
  }
}
