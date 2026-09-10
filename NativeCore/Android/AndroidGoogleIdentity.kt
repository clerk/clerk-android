package com.clerk.api

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.UUID
import kotlinx.serialization.json.*

internal object AndroidGoogleIdentity {
  suspend fun get(
    arguments: JsonObject,
    request: suspend (GetCredentialRequest) -> Credential,
  ): JsonElement {
    val clientId =
      arguments.getValue("clientId").requireString().takeIf { it.isNotBlank() }
        ?: throw CoreException("invalid_credential_options")
    val option =
      GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(true)
        .setNonce(UUID.randomUUID().toString())
        .setServerClientId(clientId)
        .build()
    try {
      val credential = request(GetCredentialRequest(listOf(option)))
      if (
        credential !is CustomCredential ||
          credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
      )
        throw CoreException("invalid_credential_response")
      val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
      return buildJsonObject { put("token", token) }
    } catch (_: GetCredentialCancellationException) {
      throw CoreException("user_cancelled")
    } catch (_: NoCredentialException) {
      throw CoreException("google_account_unavailable")
    } catch (_: GetCredentialProviderConfigurationException) {
      throw CoreException("credential_provider_unavailable")
    }
  }
}
