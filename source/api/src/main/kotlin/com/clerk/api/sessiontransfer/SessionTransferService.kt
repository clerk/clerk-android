package com.clerk.api.sessiontransfer

import androidx.core.net.toUri
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.sessiontransfer.SessionTransferResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.network.serialization.localFailure

internal object SessionTransferService {
  suspend fun prepare(
    destination: String,
    presentation: SessionTransferPresentation,
  ): ClerkResult<SessionTransfer, ClerkErrorResponse> {
    val sessionId =
      Clerk.session?.id
        ?: return localFailure(
          code = LocalFailureCodes.SIGNED_OUT,
          longMessage = "A session transfer requires an active session.",
        )
    return when (
      val result =
        ClerkApi.session.createSessionTransfer(
          sessionId = sessionId,
          redirectUrl = destination,
          presentationMode = presentation.value,
        )
    ) {
      is ClerkResult.Failure -> result
      is ClerkResult.Success ->
        result.value.toSessionTransfer()?.let { ClerkResult.success(it) }
          ?: ClerkResult.unknownFailure(
            IllegalStateException("Session transfer creation returned an invalid response.")
          )
    }
  }
}

internal fun SessionTransferResource.toSessionTransfer(): SessionTransfer? {
  if (objectType != SESSION_TRANSFER_OBJECT) return null
  return runCatching { url.toUri() }
    .getOrNull()
    ?.takeIf { uri ->
      uri.isHierarchical &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null &&
        uri.scheme.equals("https", ignoreCase = true)
    }
    ?.let { uri -> SessionTransfer(url = uri, expiresAt = expiresAt) }
}

private const val SESSION_TRANSFER_OBJECT = "session_transfer"
