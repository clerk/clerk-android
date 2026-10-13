package com.clerk.api.passkeys

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.exceptions.CreateCredentialException
import com.clerk.api.Clerk
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.credentials.classifyCreateCredentialFailure
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.network.serialization.catchingClerkResult
import com.clerk.api.network.serialization.localFailure
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

internal object PasskeyCreationService {

  private var credentialManager: PasskeyCredentialManager = PasskeyCredentialManagerImpl()

  @VisibleForTesting
  internal fun setCredentialManager(manager: PasskeyCredentialManager) {
    credentialManager = manager
  }

  @SuppressLint("PublicKeyCredential")
  suspend fun createPasskey(): ClerkResult<Passkey, ClerkErrorResponse> {
    val activity = Clerk.credentialActivity()
    if (activity == null) {
      ClerkLog.e("Passkey creation requires an active Activity")
      return ClerkResult.unknownFailure(CredentialFlowException.MissingActivity())
    }

    return when (val createPasskeyResult = ClerkApi.user.createPasskey()) {
      is ClerkResult.Failure -> {
        ClerkLog.e("Passkey creation failed: ${createPasskeyResult.error}")
        createPasskeyResult
      }
      is ClerkResult.Success -> {
        val nonce = createPasskeyResult.value.verification?.nonce
        if (nonce == null) {
          ClerkLog.e("Passkey creation failed: missing verification nonce")
          localFailure(
            code = LocalFailureCodes.MISSING_RESOURCE_DATA,
            longMessage = "Passkey creation response is missing the nonce",
          )
        } else {
          catchingClerkResult(
            onException = { ClerkLog.e("Passkey creation failed with exception: ${it.message}") }
          ) {
            registerPasskey(activity, createPasskeyResult.value.id, nonce)
          }
        }
      }
    }
  }

  @SuppressLint("PublicKeyCredential")
  private suspend fun registerPasskey(
    activity: android.app.Activity,
    passkeyId: String,
    nonce: String,
  ): ClerkResult<Passkey, ClerkErrorResponse> {
    return try {
      val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(requestJson = nonce)
      val result =
        credentialManager.createCredential(
          context = activity,
          request = createPublicKeyCredentialRequest,
        )
      val passkeyData = parsePasskeyDataDirectFromBundle(result.data)
      val verificationResult =
        ClerkApi.user.attemptPasskeyVerification(
          passkeyId = passkeyId,
          publicKeyCredential = ClerkApi.json.encodeToString(passkeyData),
        )
      verificationResult
        .onSuccess { ClerkLog.d("Passkey created successfully: ${it}") }
        .onFailure { ClerkLog.e("Passkey creation failed: ${it}") }
      ClerkLog.d("Passkey creation result: ${result.data}")
      verificationResult
    } catch (e: CreateCredentialException) {
      ClerkLog.e("Passkey creation failed with exception: ${e.message}")
      classifyCreateCredentialFailure(e)
    } catch (e: CredentialFlowException) {
      ClerkLog.e("Passkey creation cannot start: ${e.message}")
      ClerkResult.unknownFailure(e)
    }
  }

  /**
   * Parses passkey credential data directly from the Android system's Bundle response.
   *
   * This method extracts the JSON response from the credential creation result, deserializes it
   * into a structured format, and converts it to the simplified [PublicKeyCredentialData] format
   * expected by the Clerk API.
   *
   * The method specifically extracts:
   * - Credential ID and raw ID
   * - Credential type
   * - Essential response fields (attestationObject, clientDataJSON)
   *
   * @param result The Bundle containing the credential creation response from Android
   * @return [PublicKeyCredentialData] containing the parsed credential information
   * @throws IllegalArgumentException if the registration response JSON is not found in the bundle
   */
  private fun parsePasskeyDataDirectFromBundle(result: Bundle): PublicKeyCredentialData {
    val jsonString =
      result.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
        ?: throw IllegalArgumentException("No registration response JSON found in bundle")

    val json = Json { ignoreUnknownKeys = true }
    val fullResponse = json.decodeFromString<FullPasskeyResponse>(jsonString)

    val responseMap =
      mapOf(
        "attestationObject" to
          fullResponse.response["attestationObject"]?.jsonPrimitive?.content.orEmpty(),
        "clientDataJSON" to
          fullResponse.response["clientDataJSON"]?.jsonPrimitive?.content.orEmpty(),
      )

    return PublicKeyCredentialData(
      id = fullResponse.id,
      rawId = fullResponse.rawId,
      type = fullResponse.type,
      response = responseMap,
    )
  }
}
