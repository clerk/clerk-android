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
import com.clerk.api.auth.createSignIn
import com.clerk.api.auth.types.Strategy
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.credentials.classifyGetCredentialFailure
import com.clerk.api.credentials.shouldSuppressAutomaticCredentialFlowError
import com.clerk.api.log.ClerkLogger
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.network.serialization.catchingClerkResult
import com.clerk.api.network.serialization.localFailure
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.session.attemptFirstFactorVerification
import com.clerk.api.session.attemptSecondFactorVerification
import com.clerk.api.session.prepareFirstFactorVerification
import com.clerk.api.session.prepareSecondFactorVerification
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.signin.attemptSecondFactorImpl
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

  @Suppress("DEPRECATION") private val googleCredentialManager = GoogleCredentialManagerImpl()

  @VisibleForTesting
  internal fun setCredentialManager(manager: PasskeyCredentialManager) {
    credentialManager = manager
  }

  @VisibleForTesting
  internal fun setGoogleSignInService(service: GoogleSignInService) {
    googleSignInService = service
  }

  suspend fun signInWithGoogleCredential(
    credentialTypes: List<SignIn.CredentialType>,
    allowedCredentialIds: List<String> = emptyList(),
    preferImmediatelyAvailableCredentials: Boolean = false,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLogger.d(
      "Starting passkey sign-in; preferImmediatelyAvailableCredentials=" +
        preferImmediatelyAvailableCredentials
    )
    val activity = Clerk.credentialActivity()
    return when {
      credentialTypes.isEmpty() -> {
        ClerkResult.unknownFailure(IllegalStateException("No credential types specified"))
      }

      activity == null -> {
        ClerkLogger.e("Passkey sign-in requires an active Activity")
        ClerkResult.unknownFailure(CredentialFlowException.MissingActivity())
      }

      else -> {
        when (val createResult = createSignIn()) {
          is ClerkResult.Success -> {
            val signIn = createResult.value
            catchingClerkResult(
              onException = { ClerkLogger.e("Passkey sign-in failed: ${it.message}") }
            ) {
              signInWithSelectedCredential(
                activity = activity,
                signIn = signIn,
                credentialTypes = credentialTypes,
                allowedCredentialIds = allowedCredentialIds,
                preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
              )
            }
          }
          is ClerkResult.Failure -> {
            ClerkLogger.e("Failed to create SignIn: ${createResult.error}")
            createResult
          }
        }
      }
    }
  }

  private suspend fun signInWithSelectedCredential(
    activity: android.app.Activity,
    signIn: SignIn,
    credentialTypes: List<SignIn.CredentialType>,
    allowedCredentialIds: List<String>,
    preferImmediatelyAvailableCredentials: Boolean,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    val nonce = signIn.firstFactorVerification?.nonce
    if (nonce == null && SignIn.CredentialType.PASSKEY in credentialTypes) {
      return missingPreparedVerificationNonceFailure("firstFactorVerification")
    }
    return try {
      val credential =
        getCredentialFromManager(
          activity = activity,
          nonce = nonce,
          allowedCredentialIds = allowedCredentialIds,
          credentialRequestTypes = credentialTypes,
          preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
        )
      handleCredential(credential, signIn)
    } catch (e: GetCredentialException) {
      ClerkLogger.e("Passkey sign-in failed: ${e.message}")
      classifyGetCredentialFailure(e, credentialTypes).also { failure ->
        clearSuppressedAutomaticSignInAttempt(
          preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
          signIn = signIn,
          failure = failure,
        )
      }
    } catch (e: CredentialFlowException) {
      ClerkLogger.e("Passkey sign-in cannot start: ${e.message}")
      ClerkResult.unknownFailure(e).also { failure ->
        clearSuppressedAutomaticSignInAttempt(
          preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
          signIn = signIn,
          failure = failure,
        )
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
      signIn.supportedSecondFactors?.any { it.strategyType == Strategy.Passkey } == true

    return when {
      !isSecondFactorStatus || !hasPasskeySecondFactor ->
        signInWithGoogleCredential(
          credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
          allowedCredentialIds = allowedCredentialIds,
        )
      else -> {
        val activity =
          Clerk.credentialActivity()
            ?: return ClerkResult.unknownFailure(CredentialFlowException.MissingActivity()).also {
              ClerkLogger.e("Passkey second-factor sign-in requires an active Activity")
            }
        val prepareResult =
          signIn.prepareSecondFactor(strategy = SignIn.PrepareSecondFactorStrategy.Passkey)
        when (prepareResult) {
          is ClerkResult.Failure -> {
            ClerkLogger.e("Failed to prepare passkey second-factor sign-in: ${prepareResult.error}")
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
  ): ClerkResult<SignIn, ClerkErrorResponse> =
    catchingClerkResult(
      onException = { ClerkLogger.e("Passkey second-factor sign-in failed: ${it.message}") }
    ) {
      attemptSignInPasskeySecondFactorOrThrow(activity, signIn, allowedCredentialIds, nonce)
    }

  private suspend fun attemptSignInPasskeySecondFactorOrThrow(
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
        signIn.attemptSecondFactorImpl(
          SignIn.AttemptSecondFactorParams.Passkey(credential.authenticationResponseJson)
        )
      } else {
        ClerkResult.unknownFailure(IllegalStateException("Unsupported credential type"))
      }
    } catch (e: GetCredentialException) {
      ClerkLogger.e("Passkey second-factor sign-in failed: ${e.message}")
      classifyGetCredentialFailure(e, listOf(SignIn.CredentialType.PASSKEY))
    } catch (e: CredentialFlowException) {
      ClerkLogger.e("Passkey second-factor sign-in cannot start: ${e.message}")
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
    val cleared = Clerk.mutateClient { client ->
      client.copy(signIn = null).takeIf { client.signIn?.id == signIn.id }
    }
    if (cleared) {
      ClerkLogger.d("Cleared suppressed automatic passkey sign-in attempt ${signIn.id}")
    }
  }

  suspend fun verifySessionWithPasskey(
    session: Session,
    allowedCredentialIds: List<String> = emptyList(),
    level: SessionVerification.Level = SessionVerification.Level.FIRST_FACTOR,
  ): ClerkResult<SessionVerification, ClerkErrorResponse> {
    ClerkLogger.d("Starting passkey session reverification")
    val activity =
      Clerk.credentialActivity()
        ?: return ClerkResult.unknownFailure(CredentialFlowException.MissingActivity()).also {
          ClerkLogger.e("Passkey session reverification requires an active Activity")
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
        session.prepareSecondFactorVerification(Strategy.Passkey.value)
      } else {
        session.prepareFirstFactorVerification(Strategy.Passkey.value)
      }
    return when (prepareResult) {
      is ClerkResult.Failure -> {
        ClerkLogger.e("Failed to prepare passkey session reverification: ${prepareResult.error}")
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
  ): ClerkResult.Failure<ClerkErrorResponse> {
    ClerkLogger.e("Missing nonce in prepared verification $verificationField")
    return localFailure(
      code = LocalFailureCodes.MISSING_RESOURCE_DATA,
      longMessage = "Prepared passkey verification is missing the nonce",
    )
  }

  private suspend fun attemptSessionPasskeyVerification(
    activity: android.app.Activity,
    session: Session,
    allowedCredentialIds: List<String>,
    nonce: String,
    level: SessionVerification.Level,
  ): ClerkResult<SessionVerification, ClerkErrorResponse> =
    catchingClerkResult(
      onException = { ClerkLogger.e("Passkey session reverification failed: ${it.message}") }
    ) {
      attemptSessionPasskeyVerificationOrThrow(
        activity,
        session,
        allowedCredentialIds,
        nonce,
        level,
      )
    }

  private suspend fun attemptSessionPasskeyVerificationOrThrow(
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
        ClerkLogger.d("Attempting passkey session reverification")
        val result =
          if (level == SessionVerification.Level.SECOND_FACTOR) {
            session.attemptSecondFactorVerification(
              strategy = Strategy.Passkey.value,
              publicKeyCredential = credential.authenticationResponseJson,
            )
          } else {
            session.attemptFirstFactorVerification(
              Session.AttemptFirstFactorParams.Passkey(credential.authenticationResponseJson)
            )
          }

        if (result is ClerkResult.Failure) {
          ClerkLogger.e("Passkey session reverification failed: ${result.error}")
        }

        result
      } else {
        ClerkResult.unknownFailure(IllegalStateException("Unsupported credential type"))
      }
    } catch (e: GetCredentialException) {
      ClerkLogger.e("Passkey session reverification failed: ${e.message}")
      classifyGetCredentialFailure(e, listOf(SignIn.CredentialType.PASSKEY))
    } catch (e: CredentialFlowException) {
      ClerkLogger.e("Passkey session reverification cannot start: ${e.message}")
      ClerkResult.unknownFailure(e)
    }
  }

  private suspend fun createSignIn(): ClerkResult<SignIn, ClerkErrorResponse> {
    return ClerkApi.signIn.createSignIn(
      mapOf(STRATEGY to Strategy.Passkey.value, "locale" to Clerk.locale.value.orEmpty())
    )
  }

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
        ClerkLogger.e("No credential available: ${e.message}")
        throw e
      } catch (e: GetCredentialException) {
        ClerkLogger.e("Error getting credential: ${e.message}")
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
        ClerkLogger.d(
          "Built credential request; preferImmediatelyAvailableCredentials=" +
            it.preferImmediatelyAvailableCredentials
        )
      }
  }

  @VisibleForTesting
  internal fun createWebAuthnRequest(
    nonce: String?,
    allowedCredentialIds: List<String> = emptyList(),
  ): GetPasskeyRequest {
    val requestJson = Json.parseToJsonElement(requireNotNull(nonce)).jsonObject
    val challenge = requestJson.getValue("challenge").jsonPrimitive.content
    val rpId =
      checkNotNull(requestJson.passkeyRpId() ?: PasskeyHelper.getDomain()) {
        "Missing passkey relying party id"
      }
    val allowCredentials =
      allowedCredentialIds.toAllowCredentials().ifEmpty { requestJson.passkeyAllowCredentials() }
    ClerkLogger.d(
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

  private suspend fun handleCredential(
    credential: Credential,
    signIn: SignIn,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLogger.d("Handling credential type: ${credential::class.simpleName}")

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
    ClerkLogger.d("Attempting passkey authentication")

    val result = signIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.Passkey(responseJson))

    if (result is ClerkResult.Failure) {
      ClerkLogger.e("Passkey authentication failed: ${result.error}")
    }

    return result
  }

  private suspend fun handleCustomCredential(
    credential: CustomCredential
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      ClerkLogger.d("Processing Google ID token credential")
    }

    return when (val result = googleSignInService.handleSignInResult(credential)) {
      is ClerkResult.Success -> result.value.toSignInResult()
      is ClerkResult.Failure -> {
        ClerkLogger.e("Google sign-in failed: ${result.error}")
        result
      }
    }
  }

  private suspend fun handlePasswordCredential(
    credential: PasswordCredential
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLogger.d("Attempting password authentication with saved credential")
    val result =
      Clerk.auth.createSignIn(
        SignIn.CreateParams.Strategy.Password(
          identifier = credential.id,
          password = credential.password,
        )
      )

    if (result is ClerkResult.Failure) {
      ClerkLogger.e("Password authentication failed: ${result.error}")
    }

    return result
  }

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
        ClerkLogger.d("Google sign-in transferred to a sign-up that needs more fields")
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
