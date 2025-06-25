package com.clerk.passkeys

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.NoCredentialException
import com.clerk.Clerk
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import com.clerk.signin.SignIn
import com.clerk.signin.attemptFirstFactor
import com.clerk.sso.GoogleSignInService
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Service responsible for authenticating users with passkeys.
 *
 * This service handles the complete passkey authentication flow including:
 * - Creating a sign-in session with passkey strategy
 * - Requesting credentials from Android Credential Manager
 * - Processing different types of credentials (passkey, password, Google)
 * - Completing the authentication with the Clerk API
 */
internal object PasskeyAuthenticationService {

  /**
   * Initiates and completes a sign-in flow using passkeys.
   *
   * This method orchestrates the entire passkey authentication process:
   * 1. Creates a new sign-in session with passkey strategy
   * 2. Requests available credentials from the system
   * 3. Handles the selected credential based on its type
   * 4. Completes the authentication with the Clerk API
   *
   * @param allowedCredentialIds Optional list of specific credential IDs to allow. If provided,
   *   only these credentials will be available for selection. If empty, all available passkeys will
   *   be presented.
   * @return [ClerkResult] containing either a successful [SignIn] or a [ClerkErrorResponse]
   */
  suspend fun signInWithPasskey(
    allowedCredentialIds: List<String> = emptyList()
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLog.d("Starting passkey sign-in")

    val context = Clerk.applicationContext!!.get()!!
    return when (val createResult = createSignIn()) {
      is ClerkResult.Success -> {
        val signIn = createResult.value
        try {
          val credential = getCredentialFromManager(context, signIn, allowedCredentialIds)
          handleCredential(credential, signIn)
        } catch (e: Exception) {
          ClerkLog.e("Passkey sign-in failed: ${e.message}")
          ClerkResult.unknownFailure(e)
        }
      }
      is ClerkResult.Failure -> {
        ClerkLog.e("Failed to create SignIn: ${createResult.error}")
        createResult
      }
    }
  }

  /**
   * Creates a new sign-in session configured for passkey authentication.
   *
   * This method initiates a sign-in session with the Clerk API using the passkey strategy. The
   * returned sign-in object contains the challenge and other data needed for WebAuthn.
   *
   * @return [ClerkResult] containing either a [SignIn] session or an error
   */
  private suspend fun createSignIn(): ClerkResult<SignIn, ClerkErrorResponse> {
    return ClerkApi.signIn.createSignIn(mapOf(STRATEGY_KEY to PASSKEY_STRATEGY))
  }

  /**
   * Retrieves available credentials from the Android Credential Manager.
   *
   * This method builds a credential request containing the WebAuthn challenge and other
   * authentication parameters, then requests credentials from the system. The user will be
   * presented with available credentials to choose from.
   *
   * @param context Android context for the credential manager
   * @param signIn The sign-in session containing authentication challenge
   * @param allowedCredentialIds Optional list of allowed credential IDs to filter results
   * @return The selected [Credential] from the user
   * @throws NoCredentialException if no credentials are available
   * @throws Exception for other credential retrieval errors
   */
  private suspend fun getCredentialFromManager(
    context: Context,
    signIn: SignIn,
    allowedCredentialIds: List<String> = emptyList(),
  ): Credential {
    val credentialManager = CredentialManager.create(context)
    val credentialRequest = buildCredentialRequest(signIn, allowedCredentialIds)

    val result =
      try {
        credentialManager.getCredential(context, credentialRequest)
      } catch (e: NoCredentialException) {
        ClerkLog.e("No credential available: ${e.message}")
        throw e
      } catch (e: Exception) {
        ClerkLog.e("Error getting credential: ${e.message}")
        throw e
      }

    return result.credential
  }

  /**
   * Builds a credential request for the Android Credential Manager.
   *
   * This method constructs a request that can handle multiple credential types:
   * - Public key credentials (passkeys)
   * - Password credentials (currently commented out)
   * - Google ID credentials (currently commented out)
   *
   * @param signIn The sign-in session containing the WebAuthn challenge
   * @param allowedCredentialIds Optional list of credential IDs to restrict selection
   * @return [GetCredentialRequest] configured with the appropriate options
   */
  private fun buildCredentialRequest(
    signIn: SignIn,
    allowedCredentialIds: List<String> = emptyList(),
  ): GetCredentialRequest {

    val webAuthnRequest =
      createWebAuthnRequest(signIn.firstFactorVerification?.nonce, allowedCredentialIds)
    //    val getPasswordOption = GetPasswordOption()
    val getPublicKeyCredentialOption = buildPublicKeyCredentialOption(webAuthnRequest)
    //    val googleIdOption = GoogleCredentialManagerImpl().getGoogleIdOption()

    return GetCredentialRequest(
      listOf(
        //        getPasswordOption,
        getPublicKeyCredentialOption
        //        googleIdOption,
      )
    )
  }

  /**
   * Creates a WebAuthn request object from the sign-in challenge.
   *
   * This method parses the nonce from the sign-in session to extract the challenge and builds a
   * properly formatted WebAuthn request for passkey authentication.
   *
   * @param nonce The JSON-encoded challenge from the sign-in session
   * @param allowedCredentialIds Optional list of credential IDs to allow
   * @return [GetPasskeyRequest] containing the WebAuthn parameters
   */
  private fun createWebAuthnRequest(
    nonce: String?,
    allowedCredentialIds: List<String> = emptyList(),
  ): GetPasskeyRequest {
    val requestJson = JSONObject(requireNotNull(nonce))
    val challenge = requestJson.get("challenge") as String

    val allowCredentials =
      allowedCredentialIds.map { credentialId ->
        mapOf("type" to "public-key", "id" to credentialId)
      }

    return GetPasskeyRequest(
      challenge = challenge,
      allowCredentials = allowCredentials,
      timeout = 1800000,
      userVerification = "required",
      rpId = PasskeyHelper.getDomain(),
    )
  }

  /**
   * Builds a public key credential option for the credential request.
   *
   * This method serializes the WebAuthn request into JSON format and wraps it in a
   * [GetPublicKeyCredentialOption] for use with the Credential Manager.
   *
   * @param webAuthnRequest The WebAuthn request parameters
   * @return [GetPublicKeyCredentialOption] containing the serialized request
   */
  private fun buildPublicKeyCredentialOption(
    webAuthnRequest: GetPasskeyRequest
  ): GetPublicKeyCredentialOption {

    val jsonString = Json.encodeToString(webAuthnRequest)
    return GetPublicKeyCredentialOption(requestJson = jsonString)
  }

  /**
   * Handles the selected credential based on its type.
   *
   * This method processes different types of credentials returned by the system:
   * - [PublicKeyCredential]: Handles passkey authentication
   * - [PasswordCredential]: Handles password-based authentication (currently returns success)
   * - [CustomCredential]: Handles custom credentials like Google Sign-In
   *
   * @param credential The credential selected by the user
   * @param signIn The sign-in session to complete authentication with
   * @return [ClerkResult] containing the result of the authentication attempt
   */
  private suspend fun handleCredential(
    credential: Credential,
    signIn: SignIn,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLog.d("Handling credential type: ${credential::class.simpleName}")

    return when (credential) {
      is PublicKeyCredential -> {
        handlePublicKeyCredential(credential, signIn)
      }

      is PasswordCredential -> {
        ClerkLog.d("Handling password credential")
        ClerkResult.success(signIn)
      }

      is CustomCredential -> {
        handleCustomCredential(credential)
      }

      else -> {
        ClerkResult.unknownFailure(IllegalStateException("Unknown credential type"))
      }
    }
  }

  /**
   * Handles authentication with a public key credential (passkey).
   *
   * This method extracts the authentication response from the passkey credential and attempts to
   * complete the first factor authentication with the Clerk API.
   *
   * @param credential The public key credential containing the authentication response
   * @param signIn The sign-in session to authenticate
   * @return [ClerkResult] containing the result of the passkey authentication
   */
  private suspend fun handlePublicKeyCredential(
    credential: PublicKeyCredential,
    signIn: SignIn,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    val responseJson = credential.authenticationResponseJson
    ClerkLog.d("Attempting passkey authentication")

    val result = signIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.Passkey(responseJson))

    if (result is ClerkResult.Failure) {
      ClerkLog.e("Passkey authentication failed: ${result.error}")
    }

    return result
  }

  /**
   * Handles authentication with a custom credential.
   *
   * This method currently handles Google ID token credentials by delegating to the
   * GoogleSignInService. Other custom credential types could be added here.
   *
   * @param credential The custom credential to process
   * @return [ClerkResult] containing the result of the custom credential authentication
   */
  private suspend fun handleCustomCredential(
    credential: CustomCredential
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      ClerkLog.d("Processing Google ID token credential")
    }

    return when (val result = GoogleSignInService().handleSignInResult(credential)) {
      is ClerkResult.Success -> {
        ClerkResult.success(result.value.signIn!!)
      }
      is ClerkResult.Failure -> {
        ClerkLog.e("Google sign-in failed: ${result.error}")
        result
      }
    }
  }
}
