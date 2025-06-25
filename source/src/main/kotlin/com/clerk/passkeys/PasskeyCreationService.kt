package com.clerk.passkeys

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import com.clerk.Clerk
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.serialization.ClerkResult
import com.clerk.network.serialization.onFailure
import com.clerk.network.serialization.onSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

/**
 * Service responsible for creating new passkeys for users.
 *
 * This service handles the complete passkey creation flow including:
 * - Initiating passkey creation with the Clerk API
 * - Using Android Credential Manager to create the actual credential
 * - Verifying the created passkey with the Clerk API
 */
internal object PasskeyCreationService {

  /**
   * Creates a new passkey for the current user.
   *
   * This method performs the following steps:
   * 1. Requests passkey creation from the Clerk API to get the challenge
   * 2. Uses Android's CredentialManager to create the passkey credential
   * 3. Parses the credential response from the system
   * 4. Sends the credential data back to Clerk for verification and storage
   *
   * The method handles both success and failure cases, logging appropriate messages. On failure
   * during the initial API request, the method returns early. On success, it proceeds with the full
   * credential creation and verification flow.
   */
  @SuppressLint("PublicKeyCredential")
  suspend fun createPasskey() {
    val context = Clerk.applicationContext!!.get()!!
    val credentialManager = CredentialManager.create(context)
    when (val createPasskeyResult = ClerkApi.user.createPasskey()) {
      is ClerkResult.Failure -> {}
      is ClerkResult.Success -> {
        val createPublicKeyCredentialRequest =
          CreatePublicKeyCredentialRequest(
            requestJson = createPasskeyResult.value.verification?.nonce!!
          )
        val result =
          credentialManager.createCredential(
            context = context,
            request = createPublicKeyCredentialRequest,
          )
        val passkeyData = parsePasskeyDataDirectFromBundle(result.data)
        ClerkApi.user
          .attemptPasskeyVerification(
            passkeyId = createPasskeyResult.value.id,
            publicKeyCredential = ClerkApi.json.encodeToString(passkeyData),
          )
          .onSuccess { ClerkLog.d("Passkey created successfully: ${it}") }
          .onFailure { ClerkLog.e("Passkey creation failed: ${it}") }
        ClerkLog.e("Passkey creation result: ${result.data}")
      }
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
    // Extract JSON string from the Bundle
    val jsonString =
      result.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
        ?: throw IllegalArgumentException("No registration response JSON found in bundle")

    val json = Json { ignoreUnknownKeys = true }
    val fullResponse = json.decodeFromString<FullPasskeyResponse>(jsonString)

    // Convert response JsonObject to Map<String, String> with only the required fields
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
