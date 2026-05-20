package com.clerk.api.credentials

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.signin.SignIn

internal sealed class CredentialFlowException(
  message: String,
  internal open val userMessage: String? = null,
  internal open val suppressUserFacingError: Boolean = false,
) : IllegalStateException(message) {

  internal class NoGoogleAccount :
    CredentialFlowException(
      message = "No Google account is available on this device.",
      userMessage =
        "No Google account available. Add one in Settings or use another sign-in method.",
    )

  internal class NoSavedCredential :
    CredentialFlowException(
      message = "No saved credentials are available on this device.",
      userMessage = "No saved credentials are available on this device. Try another sign-in method.",
    )

  internal class UserCancelled :
    CredentialFlowException(
      message = "The credential flow was cancelled by the user.",
      suppressUserFacingError = true,
    )

  internal class MissingActivity :
    CredentialFlowException(
      message = "Credential Manager requires an active Activity context.",
      userMessage = "Authentication requires an active screen. Try again from the app.",
    )

  internal class ProviderUnavailable :
    CredentialFlowException(
      message = "Credential Manager is not available for this flow.",
      userMessage =
        "This sign-in method is not available on this device right now. Try another sign-in method.",
    )
}

internal fun classifyGetCredentialFailure(
  exception: GetCredentialException,
  credentialTypes: List<SignIn.CredentialType>,
): ClerkResult.Failure<ClerkErrorResponse> {
  val classified =
    when {
      exception is GetCredentialCancellationException -> CredentialFlowException.UserCancelled()
      exception is NoCredentialException &&
        credentialTypes == listOf(SignIn.CredentialType.GOOGLE) ->
        CredentialFlowException.NoGoogleAccount()
      exception is NoCredentialException -> CredentialFlowException.NoSavedCredential()
      exception is GetCredentialProviderConfigurationException ->
        CredentialFlowException.ProviderUnavailable()
      exception.isActivityContextFailure() -> CredentialFlowException.MissingActivity()
      else -> exception
    }

  return ClerkResult.unknownFailure(classified)
}

internal fun classifyCreateCredentialFailure(
  exception: CreateCredentialException
): ClerkResult.Failure<ClerkErrorResponse> {
  val classified =
    when {
      exception is CreateCredentialCancellationException -> CredentialFlowException.UserCancelled()
      exception is CreateCredentialProviderConfigurationException ->
        CredentialFlowException.ProviderUnavailable()
      exception.isActivityContextFailure() -> CredentialFlowException.MissingActivity()
      else -> exception
    }

  return ClerkResult.unknownFailure(classified)
}

private val ClerkResult.Failure<ClerkErrorResponse>.credentialFlowUiMessage: String?
  get() =
    (throwable as? CredentialFlowException)?.userMessage
      ?: error?.errors?.firstOrNull()?.longMessage
      ?: error?.errors?.firstOrNull()?.message

val ClerkResult.Failure<ClerkErrorResponse>.shouldSuppressCredentialFlowError: Boolean
  get() = (throwable as? CredentialFlowException)?.suppressUserFacingError == true

val ClerkResult.Failure<ClerkErrorResponse>.resolvedCredentialFlowMessage: String
  get() = credentialFlowUiMessage ?: errorMessage

private fun Throwable.isActivityContextFailure(): Boolean {
  val detail = message.orEmpty()
  return detail.contains("Activity-based context", ignoreCase = true) ||
    detail.contains("selector UI", ignoreCase = true)
}
