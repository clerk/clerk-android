package com.clerk.api.passkeys

import androidx.annotation.VisibleForTesting
import androidx.credentials.Credential
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.clerk.api.Clerk
import com.clerk.api.Constants.Fields.STRATEGY
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.credentials.classifyGetCredentialFailure
import com.clerk.api.credentials.shouldSuppressAutomaticCredentialFlowError
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.session.attemptFirstFactorVerification
import com.clerk.api.session.attemptSecondFactorVerification
import com.clerk.api.session.prepareFirstFactorVerification
import com.clerk.api.session.prepareSecondFactorVerification
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.signin.attemptSecondFactor
import com.clerk.api.signin.prepareSecondFactor
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.GoogleCredentialManagerImpl
import com.clerk.api.sso.GoogleSignInService
import com.clerk.api.sso.OAuthResult
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Suppress("TooManyFunctions")
internal object GoogleCredentialAuthenticationService {
  private var credentialManager: PasskeyCredentialManager = PasskeyCredentialManagerImpl()

  private var googleSignInService: GoogleSignInService = GoogleSignInService()

  private val googleCredentialManager = GoogleCredentialManagerImpl()

  @VisibleForTesting
  internal fun setCredentialManager(manager: PasskeyCredentialManager) {
    credentialManager = manager
  }

  @VisibleForTesting
  internal fun setGoogleSignInService(service: GoogleSignInService) {
    googleSignInService = service
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
   *   [SignIn.CredentialType.PASSWORD], and [SignIn.CredentialType.GOOGLE]. A selected saved
   *   password signs in with a new password-strategy sign-in. A Google account that has no Clerk
   *   user is transferred to a sign-up; a completed sign-up is returned as a completed [SignIn]
   *   carrying the new session ID, and an incomplete one returns a failure.
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
    preferImmediatelyAvailableCredentials: Boolean = false,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLog.d(
      "Starting passkey sign-in; preferImmediatelyAvailableCredentials=" +
        preferImmediatelyAvailableCredentials
    )
    return when {
      credentialTypes.isEmpty() -> {
        ClerkResult.unknownFailure(IllegalStateException("No credential types specified"))
      }

      Clerk.credentialActivity() == null -> {
        ClerkLog.e("Passkey sign-in requires an active Activity")
        ClerkResult.unknownFailure(CredentialFlowException.MissingActivity())
      }

      else -> {
        val activity = Clerk.credentialActivity()!!
        when (val createResult = createSignIn()) {
          is ClerkResult.Success -> {
            val signIn = createResult.value
            try {
              val credential =
                getCredentialFromManager(
                  activity = activity,
                  nonce = signIn.firstFactorVerification?.nonce,
                  allowedCredentialIds = allowedCredentialIds,
                  credentialRequestTypes = credentialTypes,
                  preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
                )
              handleCredential(credential, signIn)
            } catch (e: GetCredentialException) {
              ClerkLog.e("Passkey sign-in failed: ${e.message}")
              classifyGetCredentialFailure(e, credentialTypes).also { failure ->
                clearSuppressedAutomaticSignInAttempt(
                  preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
                  signIn = signIn,
                  failure = failure,
                )
              }
            } catch (e: CredentialFlowException) {
              ClerkLog.e("Passkey sign-in cannot start: ${e.message}")
              ClerkResult.unknownFailure(e).also { failure ->
                clearSuppressedAutomaticSignInAttempt(
                  preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
                  signIn = signIn,
                  failure = failure,
                )
              }
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
    }
  }

  suspend fun authenticateWithPasskey(
    signIn: SignIn,
    allowedCredentialIds: List<String> = emptyList(),
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    val isSecondFactorStatus =
      signIn.status == SignIn.Status.NEEDS_SECOND_FACTOR ||
        signIn.status == SignIn.Status.NEEDS_CLIENT_TRUST
    val hasPasskeySecondFactor =
      signIn.supportedSecondFactors?.any { it.strategy == PasskeyHelper.passkeyStrategy } == true

    return when {
      !isSecondFactorStatus || !hasPasskeySecondFactor ->
        signInWithGoogleCredential(
          credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
          allowedCredentialIds = allowedCredentialIds,
        )
      Clerk.credentialActivity() == null ->
        ClerkResult.unknownFailure(CredentialFlowException.MissingActivity()).also {
          ClerkLog.e("Passkey second-factor sign-in requires an active Activity")
        }
      else -> {
        val activity = requireNotNull(Clerk.credentialActivity())
        val prepareResult =
          signIn.prepareSecondFactor(strategy = SignIn.PrepareSecondFactorStrategy.Passkey)
        when (prepareResult) {
          is ClerkResult.Failure -> {
            ClerkLog.e("Failed to prepare passkey second-factor sign-in: ${prepareResult.error}")
            prepareResult
          }
          is ClerkResult.Success -> {
            val nonce = prepareResult.value.secondFactorVerification?.nonce
            if (nonce == null) {
              missingPreparedVerificationNonceFailure("secondFactorVerification")
            } else {
              attemptSignInPasskeySecondFactor(
                activity = activity,
                signIn = prepareResult.value,
                allowedCredentialIds = allowedCredentialIds,
                nonce = nonce,
              )
            }
          }
        }
      }
    }
  }

  private suspend fun attemptSignInPasskeySecondFactor(
    activity: android.app.Activity,
    signIn: SignIn,
    allowedCredentialIds: List<String>,
    nonce: String,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    return try {
      val credential =
        getCredentialFromManager(
          activity = activity,
          nonce = nonce,
          allowedCredentialIds = allowedCredentialIds,
          credentialRequestTypes = listOf(SignIn.CredentialType.PASSKEY),
        )

      if (credential is PublicKeyCredential) {
        signIn.attemptSecondFactor(
          SignIn.AttemptSecondFactorParams.Passkey(credential.authenticationResponseJson)
        )
      } else {
        ClerkResult.unknownFailure(IllegalStateException("Unsupported credential type"))
      }
    } catch (e: GetCredentialException) {
      ClerkLog.e("Passkey second-factor sign-in failed: ${e.message}")
      classifyGetCredentialFailure(e, listOf(SignIn.CredentialType.PASSKEY))
    } catch (e: CredentialFlowException) {
      ClerkLog.e("Passkey second-factor sign-in cannot start: ${e.message}")
      ClerkResult.unknownFailure(e)
    } catch (e: Exception) {
      ClerkLog.e("Passkey second-factor sign-in failed: ${e.message}")
      ClerkResult.unknownFailure(e)
    }
  }

  private fun clearSuppressedAutomaticSignInAttempt(
    preferImmediatelyAvailableCredentials: Boolean,
    signIn: SignIn,
    failure: ClerkResult.Failure<ClerkErrorResponse>,
  ) {
    if (
      !preferImmediatelyAvailableCredentials || !failure.shouldSuppressAutomaticCredentialFlowError
    ) {
      return
    }
    if (!Clerk.clientInitialized || Clerk.client.signIn?.id != signIn.id) return

    ClerkLog.d("Clearing suppressed automatic passkey sign-in attempt ${signIn.id}")
    Clerk.updateClient(Clerk.client.copy(signIn = null))
  }

  /**
   * Initiates and completes an in-session passkey reverification flow.
   *
   * This uses the session verification nonce as the WebAuthn request challenge, then submits the
   * selected public key credential to the session verification endpoint.
   */
  suspend fun verifySessionWithPasskey(
    session: Session,
    allowedCredentialIds: List<String> = emptyList(),
    level: SessionVerification.Level = SessionVerification.Level.FIRST_FACTOR,
  ): ClerkResult<SessionVerification, ClerkErrorResponse> {
    ClerkLog.d("Starting passkey session reverification")
    val activity =
      Clerk.credentialActivity()
        ?: return ClerkResult.unknownFailure(CredentialFlowException.MissingActivity()).also {
          ClerkLog.e("Passkey session reverification requires an active Activity")
        }

    return verifySessionWithPasskey(activity, session, allowedCredentialIds, level)
  }

  private suspend fun verifySessionWithPasskey(
    activity: android.app.Activity,
    session: Session,
    allowedCredentialIds: List<String>,
    level: SessionVerification.Level,
  ): ClerkResult<SessionVerification, ClerkErrorResponse> {
    if (
      level != SessionVerification.Level.FIRST_FACTOR &&
        level != SessionVerification.Level.SECOND_FACTOR
    ) {
      return ClerkResult.unknownFailure(
        IllegalArgumentException("Passkey verification level must be first_factor or second_factor")
      )
    }

    val prepareResult =
      if (level == SessionVerification.Level.SECOND_FACTOR) {
        session.prepareSecondFactorVerification(PasskeyHelper.passkeyStrategy)
      } else {
        session.prepareFirstFactorVerification(PasskeyHelper.passkeyStrategy)
      }
    return when (prepareResult) {
      is ClerkResult.Failure -> {
        ClerkLog.e("Failed to prepare passkey session reverification: ${prepareResult.error}")
        prepareResult
      }
      is ClerkResult.Success -> {
        val nonce =
          if (level == SessionVerification.Level.SECOND_FACTOR) {
            prepareResult.value.secondFactorVerification?.nonce
          } else {
            prepareResult.value.firstFactorVerification?.nonce
          }
        if (nonce == null) {
          missingPreparedVerificationNonceFailure(
            if (level == SessionVerification.Level.SECOND_FACTOR) {
              "secondFactorVerification"
            } else {
              "firstFactorVerification"
            }
          )
        } else {
          attemptSessionPasskeyVerification(activity, session, allowedCredentialIds, nonce, level)
        }
      }
    }
  }

  private fun missingPreparedVerificationNonceFailure(
    verificationField: String
  ): ClerkResult.Failure<Nothing> {
    ClerkLog.e("Missing nonce in prepared verification $verificationField")
    return ClerkResult.unknownFailure(
      IllegalStateException("Missing nonce in prepared verification")
    )
  }

  private suspend fun attemptSessionPasskeyVerification(
    activity: android.app.Activity,
    session: Session,
    allowedCredentialIds: List<String>,
    nonce: String,
    level: SessionVerification.Level,
  ): ClerkResult<SessionVerification, ClerkErrorResponse> {
    return try {
      val credential =
        getCredentialFromManager(
          activity = activity,
          nonce = nonce,
          allowedCredentialIds = allowedCredentialIds,
          credentialRequestTypes = listOf(SignIn.CredentialType.PASSKEY),
        )

      if (credential is PublicKeyCredential) {
        ClerkLog.d("Attempting passkey session reverification")
        val result =
          if (level == SessionVerification.Level.SECOND_FACTOR) {
            session.attemptSecondFactorVerification(
              strategy = PasskeyHelper.passkeyStrategy,
              publicKeyCredential = credential.authenticationResponseJson,
            )
          } else {
            session.attemptFirstFactorVerification(
              Session.AttemptFirstFactorParams.Passkey(credential.authenticationResponseJson)
            )
          }

        if (result is ClerkResult.Failure) {
          ClerkLog.e("Passkey session reverification failed: ${result.error}")
        }

        result
      } else {
        ClerkResult.unknownFailure(IllegalStateException("Unsupported credential type"))
      }
    } catch (e: GetCredentialException) {
      ClerkLog.e("Passkey session reverification failed: ${e.message}")
      classifyGetCredentialFailure(e, listOf(SignIn.CredentialType.PASSKEY))
    } catch (e: CredentialFlowException) {
      ClerkLog.e("Passkey session reverification cannot start: ${e.message}")
      ClerkResult.unknownFailure(e)
    } catch (e: Exception) {
      ClerkLog.e("Passkey session reverification failed: ${e.message}")
      ClerkResult.unknownFailure(e)
    }
  }

  private suspend fun createSignIn(): ClerkResult<SignIn, ClerkErrorResponse> {
    return ClerkApi.signIn.createSignIn(
      mapOf(STRATEGY to PasskeyHelper.passkeyStrategy, "locale" to Clerk.locale.value.orEmpty())
    )
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
   * @param activity Android activity required for credential manager operations.
   * @param nonce The JSON-encoded WebAuthn challenge metadata.
   * @param allowedCredentialIds Optional list of allowed credential IDs to filter results. If
   *   empty, all available credentials of the requested types will be presented.
   * @param credentialRequestTypes List of credential types to request from the system.
   * @return The selected [Credential] chosen by the user from the available options.
   * @throws NoCredentialException If no credentials are available for the requested types.
   * @throws Exception For other credential retrieval errors, such as user cancellation or system
   *   errors.
   */
  private suspend fun getCredentialFromManager(
    activity: android.app.Activity,
    nonce: String?,
    allowedCredentialIds: List<String> = emptyList(),
    credentialRequestTypes: List<SignIn.CredentialType>,
    preferImmediatelyAvailableCredentials: Boolean = false,
  ): Credential {
    val credentialRequest =
      buildCredentialRequest(
        nonce = nonce,
        allowedCredentialIds = allowedCredentialIds,
        credentialRequestTypes = credentialRequestTypes,
        preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
      )

    val result =
      try {
        credentialManager.getCredential(activity, credentialRequest)
      } catch (e: NoCredentialException) {
        ClerkLog.e("No credential available: ${e.message}")
        throw e
      } catch (e: Exception) {
        ClerkLog.e("Error getting credential: ${e.message}")
        throw e
      }

    return result.credential
  }

  private fun buildCredentialRequest(
    nonce: String?,
    allowedCredentialIds: List<String> = emptyList(),
    credentialRequestTypes: List<SignIn.CredentialType>,
    preferImmediatelyAvailableCredentials: Boolean = false,
  ): GetCredentialRequest {
    val requestOptions = mutableListOf<CredentialOption>()

    credentialRequestTypes.forEach {
      when (it) {
        SignIn.CredentialType.PASSKEY -> {
          val webAuthnRequest = createWebAuthnRequest(nonce, allowedCredentialIds)
          requestOptions.add(buildPublicKeyCredentialOption(webAuthnRequest))
        }
        SignIn.CredentialType.PASSWORD -> requestOptions.add(GetPasswordOption())
        SignIn.CredentialType.GOOGLE ->
          requestOptions.add(googleCredentialManager.getGoogleIdOption())
        SignIn.CredentialType.UNKNOWN -> {}
      }
    }

    return GetCredentialRequest(
        credentialOptions = requestOptions,
        preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
      )
      .also {
        ClerkLog.d(
          "Built credential request; preferImmediatelyAvailableCredentials=" +
            it.preferImmediatelyAvailableCredentials
        )
      }
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
  @VisibleForTesting
  internal fun createWebAuthnRequest(
    nonce: String?,
    allowedCredentialIds: List<String> = emptyList(),
  ): GetPasskeyRequest {
    val requestJson = Json.parseToJsonElement(requireNotNull(nonce)).jsonObject
    val challenge = requestJson.getValue("challenge").jsonPrimitive.content
    val rpId = requestJson.passkeyRpId() ?: PasskeyHelper.getDomain()
    val allowCredentials =
      allowedCredentialIds.toAllowCredentials().ifEmpty { requestJson.passkeyAllowCredentials() }
    ClerkLog.d(
      "Created passkey request; rpId=$rpId, allowCredentialsCount=${allowCredentials.size}"
    )

    return GetPasskeyRequest(
      challenge = challenge,
      allowCredentials = allowCredentials,
      timeout = 1800000,
      userVerification = "required",
      rpId = rpId,
    )
  }

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
   * - **[PasswordCredential]**: Signs in with the saved identifier and password using a new
   *   password-strategy sign-in
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
        handlePasswordCredential(credential)
      }

      is CustomCredential -> {
        handleCustomCredential(credential)
      }

      else -> {
        ClerkResult.unknownFailure(IllegalStateException("Unknown credential type"))
      }
    }
  }

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

  private suspend fun handleCustomCredential(
    credential: CustomCredential
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      ClerkLog.d("Processing Google ID token credential")
    }

    return when (val result = googleSignInService.handleSignInResult(credential)) {
      is ClerkResult.Success -> result.value.toSignInResult()
      is ClerkResult.Failure -> {
        ClerkLog.e("Google sign-in failed: ${result.error}")
        result
      }
    }
  }

  /**
   * Signs in with a saved password selected from the Credential Manager.
   *
   * The sign-in created up front uses the passkey strategy and cannot be reused for a password
   * attempt, so a new sign-in is created with the password strategy. Creating it replaces the
   * client's in-progress passkey sign-in.
   */
  private suspend fun handlePasswordCredential(
    credential: PasswordCredential
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLog.d("Attempting password authentication with saved credential")
    val result =
      SignIn.create(
        SignIn.CreateParams.Strategy.Password(
          identifier = credential.id,
          password = credential.password,
        )
      )

    if (result is ClerkResult.Failure) {
      ClerkLog.e("Password authentication failed: ${result.error}")
    }

    return result
  }

  /**
   * Maps a Google ID token result onto this service's [SignIn]-typed result.
   *
   * New Google users are transferred to a sign-up, so the result can hold a [SignUp] instead of a
   * [SignIn]. A completed sign-up has already created a session and is reported as a completed
   * sign-in carrying that session (its ID is the sign-up's ID, since no sign-in exists). A sign-up
   * that still needs more fields is reported as a [CredentialFlowException.SignUpIncomplete]
   * failure; it remains on the client's sign-up so the caller can continue it.
   */
  private fun OAuthResult.toSignInResult(): ClerkResult<SignIn, ClerkErrorResponse> {
    val completedSignIn = signIn
    val transferredSignUp = signUp
    return when {
      completedSignIn != null -> ClerkResult.success(completedSignIn)
      transferredSignUp != null && transferredSignUp.status == SignUp.Status.COMPLETE ->
        ClerkResult.success(
          SignIn(
            id = transferredSignUp.id,
            status = SignIn.Status.COMPLETE,
            identifier = transferredSignUp.emailAddress,
            createdSessionId = transferredSignUp.createdSessionId,
          )
        )
      transferredSignUp != null -> {
        ClerkLog.d("Google sign-in transferred to a sign-up that needs more fields")
        ClerkResult.unknownFailure(CredentialFlowException.SignUpIncomplete())
      }
      else -> ClerkResult.unknownFailure(IllegalStateException("Google sign-in returned no result"))
    }
  }
}

private fun JsonObject.passkeyRpId(): String? {
  val topLevelRpId =
    listOf("rpId", "rp_id").firstNotNullOfOrNull { key ->
      this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }

  return topLevelRpId
    ?: runCatching {
      this["rp"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }
      .getOrNull()
}

private fun JsonObject.passkeyAllowCredentials(): List<Map<String, String>> {
  return runCatching {
    this["allowCredentials"]?.jsonArray?.mapNotNull { credential ->
      val credentialJson = credential.jsonObject
      val credentialId =
        credentialJson["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
          ?: return@mapNotNull null
      val credentialType =
        credentialJson["type"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
          ?: "public-key"

      mapOf("type" to credentialType, "id" to credentialId)
    }
  }
    .getOrNull()
    .orEmpty()
}

private fun List<String>.toAllowCredentials(): List<Map<String, String>> {
  return map { credentialId -> mapOf("type" to "public-key", "id" to credentialId) }
}
