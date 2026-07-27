package com.clerk.api.hostedauth

import android.net.Uri
import androidx.core.net.toUri
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.hostedauth.HostedAuthResource
import com.clerk.api.network.serialization.ClerkResult

internal data class HostedAuthCallback(
  val rotatingTokenNonce: String,
  val createdSessionId: String,
)

internal fun validateHostedAuthCallback(
  uri: Uri,
  redirectUrl: String,
  expectedState: String,
): ClerkResult<HostedAuthCallback, ClerkErrorResponse> {
  val callbackState = uri.singleQueryParameter("state")
  val rotatingTokenNonce = uri.nonEmptySingleQueryParameter("rotating_token_nonce")
  val createdSessionId = uri.nonEmptySingleQueryParameter("created_session_id")
  return when {
    !uri.matchesHostedAuthRedirectUrl(redirectUrl) ->
      ClerkResult.unknownFailure(
        IllegalArgumentException(
          "Hosted auth callback URL did not match the initiated redirect URL."
        )
      )
    callbackState != expectedState ->
      ClerkResult.unknownFailure(
        IllegalArgumentException("Hosted auth callback state did not match the initiated state.")
      )
    rotatingTokenNonce == null ->
      ClerkResult.unknownFailure(
        IllegalArgumentException("Hosted auth callback did not include a rotating token nonce.")
      )
    createdSessionId == null ->
      ClerkResult.unknownFailure(
        IllegalArgumentException("Hosted auth callback did not include the created session.")
      )
    else ->
      ClerkResult.success(
        HostedAuthCallback(
          rotatingTokenNonce = rotatingTokenNonce,
          createdSessionId = createdSessionId,
        )
      )
  }
}

internal fun HostedAuthResource.authenticationUri(): Uri? {
  if (objectType != HOSTED_AUTH_OBJECT) return null
  return runCatching { url.toUri() }
    .getOrNull()
    ?.takeIf { uri ->
      uri.isHierarchical &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null &&
        uri.scheme.equals("https", ignoreCase = true)
    }
}

internal fun Uri.matchesHostedAuthRedirectUrl(redirectUrl: String): Boolean =
  runCatching {
      // Comparing the encoded authority and path subsumes their decoded counterparts
      // (authority, host, port, and path), so only the encoded forms are compared.
      val expected = redirectUrl.toUri()
      !scheme.isNullOrBlank() &&
        !expected.scheme.isNullOrBlank() &&
        scheme.equals(expected.scheme, ignoreCase = true) &&
        encodedAuthority.equals(expected.encodedAuthority, ignoreCase = true) &&
        encodedPath == expected.encodedPath
    }
    .getOrDefault(false)

private fun Uri.singleQueryParameter(name: String): String? =
  runCatching { getQueryParameters(name).singleOrNull() }.getOrNull()

private fun Uri.nonEmptySingleQueryParameter(name: String): String? =
  singleQueryParameter(name)?.takeIf { it.isNotBlank() }

private const val HOSTED_AUTH_OBJECT = "hosted_auth"
