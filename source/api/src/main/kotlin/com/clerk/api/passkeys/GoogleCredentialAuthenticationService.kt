package com.clerk.api.passkeys

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.credentials.Credential
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.NoCredentialException
import com.clerk.api.Clerk
import com.clerk.api.Constants.Passkey.STRATEGY_KEY
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.sso.GoogleCredentialManagerImpl
import com.clerk.api.sso.GoogleSignInService
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Service responsible for authenticating users with Google credentials and passkeys.
 *
 * This service handles the complete authentication flow for multiple credential types including:
 * - **Passkey authentication**: WebAuthn-based authentication using biometric or device credentials
 * - **Password authentication**: Traditional username/password authentication
 * - **Google authentication**: Sign-in using Google ID tokens through Credential Manager
 *
 * ## Authentication Flow
 * 1. Creates a sign-in session with the appropriate strategy
 * 2. Requests credentials from Android Credential Manager
 * 3. Presents available credentials to the user for selection
 * 4. Processes the selected credential based on its type
 * 5. Completes authentication with the Clerk API
 *
 * The service integrates with Android's Credential Manager API to provide a unified authentication
 * experience across different credential types.
 *
 * @see PasskeyCredentialManager
 * @see GoogleSignInService
 */
internal object GoogleCredentialAuthenticationService {

  /** The credential manager used for passkey operations. */
  private var credentialManager: PasskeyCredentialManager = PasskeyCredentialManagerImpl()

  /** The Google sign-in service used for Google ID token authentication. */
  private var googleSignInService: GoogleSignInService = GoogleSignInService()

  /** The Google credential manager used for Google ID token operations. */
  private val googleCredentialManager = GoogleCredentialManagerImpl()

  /**
   * Sets the credential manager for testing purposes.
   *
   * @param manager The credential manager implementation to use
   */
  @VisibleForTesting
  internal fun setCredentialManager(manager: PasskeyCredentialManager) {
    credentialManager = manager
  }

  /**
   * Initiates and completes a sign-in flow using Google credentials and other supported credential
   * types.
   *
   * This method orchestrates the entire authentication process by:
   * 1. Creates a new sign-in session with passkey strategy
   * 2. Requests available credentials from the Android Credential Manager
   * 3. Presents credentials to the user for selection
   * 4. Handles the selected credential based on its type (passkey, password, or Google)
   * 5. Completes the authentication with the Clerk API
   *
   * The method supports multiple credential types simultaneously, allowing users to choose their
   * preferred authentication method from the available options.
   *
   * @param allowedCredentialIds Optional list of specific credential IDs to allow. If provided,
   *   only these credentials will be available for selection. If empty or not provided, all
   *   available credentials will be presented to the user.
   * @param credentialTypes List of credential types to request from the system. Defaults to only
   *   passkey credentials. Can include [SignIn.CredentialType.PASSKEY],
   *   [SignIn.CredentialType.PASSWORD], and [SignIn.CredentialType.GOOGLE].
   * @return A [ClerkResult] containing either a successful [SignIn] object on authentication
   *   success, or a [ClerkErrorResponse] detailing the failure reason.
   * @throws Exception If credential retrieval fails or an unexpected error occurs during
   *   authentication.
   *
   * ### Example usage:
   * ```kotlin
   * // Request only passkey credentials
   * val result = GoogleCredentialAuthenticationService.signInWithGoogleCredential()
   *
   * // Request multiple credential types
   * val result = GoogleCredentialAuthenticationService.signInWithGoogleCredential(
   *   credentialTypes = listOf(
   *     CredentialType.PASSKEY,
   *     CredentialType.GOOGLE,
   *     CredentialType.PASSWORD
   *   )
   * )
   * ```
   */
  suspend fun signInWithGoogleCredential(
    credentialTypes: List<SignIn.CredentialType>,
    allowedCredentialIds: List<String> = emptyList(),
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    if (credentialTypes.isEmpty()) {
      return ClerkResult.unknownFailure(IllegalStateException("No credential types specified"))
    }
    ClerkLog.d("Starting passkey sign-in")

    val context = Clerk.applicationContext!!.get()!!
    return when (val createResult = createSignIn()) {
      is ClerkResult.Success -> {
        val signIn = createResult.value
        try {
          val credential =
            getCredentialFromManager(context, signIn, allowedCredentialIds, credentialTypes)
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
   * returned sign-in object contains the WebAuthn challenge and other authentication data needed
   * for credential-based authentication.
   *
   * The sign-in session includes:
   * - A unique session identifier
   * - WebAuthn challenge data in the first factor verification nonce
   * - Supported authentication factors
   * - Session status and metadata
   *
   * @return A [ClerkResult] containing either a [SignIn] session configured for passkey
   *   authentication, or a [ClerkErrorResponse] if session creation fails.
   */
  private suspend fun createSignIn(): ClerkResult<SignIn, ClerkErrorResponse> {
    return ClerkApi.signIn.createSignIn(mapOf(STRATEGY_KEY to PasskeyHelper.passkeyStrategy))
  }

  /**
   * Retrieves available credentials from the Android Credential Manager.
   *
   * This method builds a credential request containing the WebAuthn challenge and other
   * authentication parameters, then requests credentials from the system. The user will be
   * presented with available credentials matching the requested types and allowed IDs.
   *
   * The credential manager will:
   * - Filter credentials based on the allowed credential IDs (if specified)
   * - Present only the requested credential types to the user
   * - Handle user selection and return the chosen credential
   * - Throw appropriate exceptions if no credentials are available or accessible
   *
   * @param context Android context required for credential manager operations.
   * @param signIn The sign-in session containing the WebAuthn challenge and authentication data.
   * @param allowedCredentialIds Optional list of allowed credential IDs to filter results. If
   *   empty, all available credentials of the requested types will be presented.
   * @param credentialRequestTypes List of credential types to request from the system.
   * @return The selected [Credential] chosen by the user from the available options.
   * @throws NoCredentialException If no credentials are available for the requested types.
   * @throws Exception For other credential retrieval errors, such as user cancellation or system
   *   errors.
   */
  private suspend fun getCredentialFromManager(
    context: Context,
    signIn: SignIn,
    allowedCredentialIds: List<String> = emptyList(),
    credentialRequestTypes: List<SignIn.CredentialType>,
  ): Credential {
    val credentialRequest =
      buildCredentialRequest(signIn, allowedCredentialIds, credentialRequestTypes)

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
   * This method constructs a [GetCredentialRequest] that can handle multiple credential types
   * simultaneously. The request will include options for each requested credential type:
   * - **Passkey credentials**: Uses WebAuthn parameters from the sign-in session
   * - **Password credentials**: Requests saved password credentials from the system
   * - **Google ID credentials**: Requests Google ID token credentials for OAuth authentication
   *
   * The credential manager will present all available credentials matching the requested types and
   * allow the user to select their preferred authentication method.
   *
   * @param signIn The sign-in session containing the WebAuthn challenge and authentication
   *   metadata.
   * @param allowedCredentialIds Optional list of credential IDs to restrict selection to specific
   *   credentials. If empty, all available credentials of the requested types will be included.
   * @param credentialRequestTypes List of credential types to include in the request. Each type
   *   will be converted to the appropriate [CredentialOption] for the credential manager.
   * @return A [GetCredentialRequest] configured with the appropriate credential options for the
   *   specified types and restrictions.
   */
  private fun buildCredentialRequest(
    signIn: SignIn,
    allowedCredentialIds: List<String> = emptyList(),
    credentialRequestTypes: List<SignIn.CredentialType>,
  ): GetCredentialRequest {
    val requestOptions = mutableListOf<CredentialOption>()

    credentialRequestTypes.forEach {
      when (it) {
        SignIn.CredentialType.PASSKEY -> {
          val webAuthnRequest =
            createWebAuthnRequest(signIn.firstFactorVerification?.nonce, allowedCredentialIds)
          requestOptions.add(buildPublicKeyCredentialOption(webAuthnRequest))
        }
        SignIn.CredentialType.PASSWORD -> requestOptions.add(GetPasswordOption())
        SignIn.CredentialType.GOOGLE ->
          requestOptions.add(googleCredentialManager.getGoogleIdOption())
        SignIn.CredentialType.UNKNOWN -> {
          // Skip unknown credential types
        }
      }
    }

    return GetCredentialRequest(requestOptions)
  }

  /**
   * Creates a WebAuthn request object from the sign-in challenge.
   *
   * This method parses the nonce from the sign-in session to extract the WebAuthn challenge and
   * constructs a properly formatted request for passkey authentication. The request includes all
   * necessary parameters for WebAuthn authentication as specified by the Web Authentication API.
   *
   * The WebAuthn request includes:
   * - **Challenge**: The cryptographic challenge from the server
   * - **Allowed credentials**: List of specific credential IDs that can be used (if specified)
   * - **Timeout**: Maximum time allowed for the authentication ceremony
   * - **User verification**: Requirements for user presence and verification
   * - **Relying Party ID**: The domain identifier for the authentication request
   *
   * @param nonce The JSON-encoded challenge and metadata from the sign-in session's first factor
   *   verification. This contains the WebAuthn challenge and other authentication parameters.
   * @param allowedCredentialIds Optional list of credential IDs to restrict authentication to
   *   specific credentials. If empty, any registered credential for the user can be used.
   * @return A [GetPasskeyRequest] containing the WebAuthn parameters formatted for use with the
   *   Android Credential Manager.
   * @throws IllegalArgumentException If the nonce is null or contains invalid JSON.
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
   * [GetPublicKeyCredentialOption] for use with the Android Credential Manager. The serialized
   * request follows the WebAuthn specification format and includes all necessary parameters for
   * passkey authentication.
   *
   * @param webAuthnRequest The WebAuthn request parameters containing the challenge, allowed
   *   credentials, timeout, and other authentication metadata.
   * @return A [GetPublicKeyCredentialOption] containing the serialized WebAuthn request in JSON
   *   format, ready for use with the credential manager.
   */
  private fun buildPublicKeyCredentialOption(
    webAuthnRequest: GetPasskeyRequest
  ): GetPublicKeyCredentialOption {

    val jsonString = Json.encodeToString(webAuthnRequest)
    return GetPublicKeyCredentialOption(requestJson = jsonString)
  }

  /**
   * Handles the selected credential based on its type and completes authentication.
   *
   * This method processes different types of credentials returned by the Android Credential Manager
   * and routes them to the appropriate authentication handlers:
   * - **[PublicKeyCredential]**: Handles WebAuthn passkey authentication by extracting the
   *   authentication response and completing the first factor verification with Clerk
   * - **[PasswordCredential]**: Handles password-based authentication (currently returns success
   *   without processing for demonstration purposes)
   * - **[CustomCredential]**: Handles custom credentials such as Google ID tokens by delegating to
   *   specialized handlers like [GoogleSignInService]
   *
   * Each credential type follows its own authentication flow while maintaining a consistent result
   * interface through [ClerkResult].
   *
   * @param credential The credential selected by the user from the available options presented by
   *   the credential manager.
   * @param signIn The sign-in session used to complete authentication. Contains the necessary
   *   session state and authentication context.
   * @return A [ClerkResult] containing either a successful [SignIn] object with updated
   *   authentication state, or a [ClerkErrorResponse] detailing the authentication failure.
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
   * This method processes WebAuthn passkey authentication by extracting the authentication response
   * from the credential and using it to complete the first factor verification with the Clerk API.
   * The authentication response contains the cryptographic proof that the user possesses the
   * private key corresponding to the registered passkey.
   *
   * The process involves:
   * 1. Extracting the authentication response JSON from the credential
   * 2. Creating a passkey authentication attempt with the response
   * 3. Submitting the attempt to complete the first factor verification
   * 4. Processing the result and handling any authentication errors
   *
   * @param credential The public key credential containing the WebAuthn authentication response and
   *   other metadata from the passkey authentication ceremony.
   * @param signIn The sign-in session to authenticate against. This contains the challenge and
   *   other session state needed for verification.
   * @return A [ClerkResult] containing either the updated [SignIn] object with successful
   *   authentication state, or a [ClerkErrorResponse] if passkey authentication fails.
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
   * This method processes custom credential types that don't fit into the standard credential
   * categories. Currently, it specifically handles Google ID token credentials by delegating to the
   * [GoogleSignInService], but the architecture allows for easy extension to support additional
   * custom credential types.
   *
   * The method:
   * 1. Identifies the type of custom credential received
   * 2. Routes Google ID token credentials to the appropriate Google sign-in handler
   * 3. Processes the authentication result and extracts the sign-in object
   * 4. Returns a consistent [ClerkResult] interface regardless of the custom credential type
   *
   * @param credential The custom credential to process. Currently supports
   *   [GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL] for Google OAuth authentication.
   *   Additional custom credential types can be added as needed.
   * @return A [ClerkResult] containing either a successful [SignIn] object extracted from the
   *   custom credential authentication result, or a [ClerkErrorResponse] if the custom credential
   *   authentication fails.
   */
  private suspend fun handleCustomCredential(
    credential: CustomCredential
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      ClerkLog.d("Processing Google ID token credential")
    }

    return when (val result = googleSignInService.handleSignInResult(credential)) {
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
