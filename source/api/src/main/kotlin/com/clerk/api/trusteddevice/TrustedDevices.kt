package com.clerk.api.trusteddevice

import androidx.annotation.VisibleForTesting
import com.clerk.api.Clerk
import com.clerk.api.Constants.Strategy.TRUSTED_DEVICE
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The main entry point for trusted-device (biometric sign-in) credential operations.
 *
 * Access via [Clerk.trustedDevices].
 *
 * Enrolling generates a biometric-gated private key in the Android Keystore and registers its
 * public key with Clerk. Signing in signs a server challenge with that key after the user passes
 * the system biometric prompt. The private key never leaves the device.
 *
 * ### Example usage:
 * ```kotlin
 * // Enroll the current device while signed in
 * Clerk.trustedDevices.enroll()
 *
 * // Later, sign in with biometrics
 * Clerk.trustedDevices.signIn().onSuccess { signIn ->
 *   // Session created
 * }
 * ```
 */
@Suppress("TooManyFunctions", "ReturnCount")
object TrustedDevices {

  @VisibleForTesting
  internal var keyManager: TrustedDeviceKeyManager = DefaultTrustedDeviceKeyManager

  @VisibleForTesting
  internal var credentialStore: TrustedDeviceLocalCredentialStore =
    DefaultTrustedDeviceLocalCredentialStore

  /**
   * Lists active trusted-device credentials for the signed-in user.
   *
   * @return A [ClerkResult] containing the list of [TrustedDevice] credentials on success, or a
   *   [ClerkErrorResponse] on failure.
   */
  suspend fun list(): ClerkResult<List<TrustedDevice>, ClerkErrorResponse> {
    return ClerkApi.trustedDevice.list()
  }

  /**
   * Returns local trusted-device sign-in availability.
   *
   * When a Clerk session is active, this also reconciles the local credential with the server.
   * Without an active session, this reports whether the local biometric-gated credential can be
   * used to start trusted-device sign-in.
   *
   * @param id The trusted-device credential ID to check. When omitted, the available local
   *   credential is used.
   * @param identifierHint A local-only user identifier hint used to choose a matching credential.
   */
  suspend fun availability(
    id: String? = null,
    identifierHint: String? = null,
  ): TrustedDeviceAvailability {
    return when (val result = selectedLocalCredential(id, identifierHint, userId = null)) {
      is LocalCredentialResult.Available -> TrustedDeviceAvailability.Available
      is LocalCredentialResult.Unavailable -> TrustedDeviceAvailability.Unavailable(result.reason)
    }
  }

  /**
   * Returns trusted-device sign-in availability for the current signed-in user, reconciling the
   * local credential with the server.
   */
  suspend fun currentUserAvailability(): TrustedDeviceAvailability {
    val userId =
      Clerk.user?.id
        ?: return TrustedDeviceAvailability.Unavailable(
          TrustedDeviceAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
        )

    return when (val result = selectedLocalCredential(id = null, identifierHint = null, userId)) {
      is LocalCredentialResult.Available -> TrustedDeviceAvailability.Available
      is LocalCredentialResult.Unavailable -> TrustedDeviceAvailability.Unavailable(result.reason)
    }
  }

  /** Returns local trusted-device sign-in availability without reconciling with the server. */
  fun localAvailability(
    id: String? = null,
    identifierHint: String? = null,
  ): TrustedDeviceAvailability {
    return when (val result = localCredentialCandidates(id, identifierHint, userId = null)) {
      is LocalCredentialsResult.Available -> TrustedDeviceAvailability.Available
      is LocalCredentialsResult.Unavailable -> TrustedDeviceAvailability.Unavailable(result.reason)
    }
  }

  /**
   * Returns local trusted-device sign-in availability for the current signed-in user without
   * reconciling with the server.
   */
  fun currentUserLocalAvailability(): TrustedDeviceAvailability {
    val userId =
      Clerk.user?.id
        ?: return TrustedDeviceAvailability.Unavailable(
          TrustedDeviceAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
        )

    return when (val result = localCredentialCandidates(id = null, identifierHint = null, userId)) {
      is LocalCredentialsResult.Available -> TrustedDeviceAvailability.Available
      is LocalCredentialsResult.Unavailable -> TrustedDeviceAvailability.Unavailable(result.reason)
    }
  }

  /**
   * Enrolls the current app installation as a biometric trusted device.
   *
   * This requires an active or pending Clerk session. The generated private key stays on the
   * device.
   *
   * @param deviceName A human-readable device name stored with the trusted-device credential.
   * @param identifierHint A local-only user identifier hint for selecting this credential later.
   * @param policy The local authentication policy used to protect the generated private key.
   *   Defaults to requiring biometric availability while allowing device credential fallback during
   *   authentication.
   * @param promptTitle The title shown in the system biometric prompt.
   * @param promptSubtitle The subtitle shown in the system biometric prompt.
   * @return A [ClerkResult] containing the enrolled [TrustedDevice] on success, or a
   *   [ClerkErrorResponse] on failure.
   */
  suspend fun enroll(
    deviceName: String? = null,
    identifierHint: String? = null,
    policy: TrustedDevicePolicy = TrustedDevicePolicy.BIOMETRY_OR_DEVICE_PASSCODE,
    promptTitle: String? = null,
    promptSubtitle: String? = null,
  ): ClerkResult<TrustedDevice, ClerkErrorResponse> {
    if (Clerk.session?.status?.allowsTrustedDeviceEnrollment != true) {
      return clientFailure(
        "Unable to enroll a trusted device without an active or pending Clerk session."
      )
    }
    featureUnavailableFailure()?.let {
      return it
    }
    val appIdentifier =
      Clerk.applicationId
        ?: return clientFailure("Unable to enroll a trusted device without an application ID.")
    val userId =
      Clerk.user?.id
        ?: return clientFailure(
          "Unable to enroll a trusted device without a user for the current session."
        )

    val localKey =
      try {
        keyManager.createKey(policy)
      } catch (e: TrustedDeviceKeyManagerException) {
        return ClerkResult.unknownFailure(e)
      }

    val enrollmentResult =
      performEnrollment(localKey, appIdentifier, deviceName, promptTitle, promptSubtitle)
    return when (enrollmentResult) {
      is ClerkResult.Success -> {
        val trustedDevice = enrollmentResult.value
        saveLocalCredential(trustedDevice, localKey, userId, identifierHint)?.let { failure ->
          runCatching { keyManager.deleteKey(localKey.localKeyId) }
          return failure
        }
        removeOtherLocalCredentialsForCurrentApp(keeping = trustedDevice)
        enrollmentResult
      }
      is ClerkResult.Failure -> {
        runCatching { keyManager.deleteKey(localKey.localKeyId) }
        enrollmentResult
      }
    }
  }

  /**
   * Revokes a trusted-device credential for the signed-in user.
   *
   * @param id The trusted-device credential ID to revoke.
   * @return A [ClerkResult] containing the revoked [TrustedDevice] on success, or a
   *   [ClerkErrorResponse] on failure.
   */
  suspend fun revoke(id: String): ClerkResult<TrustedDevice, ClerkErrorResponse> {
    val result = ClerkApi.trustedDevice.revoke(id)
    if (result is ClerkResult.Success) {
      credentialStore.credential(id)?.let { deleteLocalCredential(it) }
    }
    return result
  }

  /**
   * Revokes the available local trusted-device credential for the current signed-in user, if one
   * exists.
   *
   * Succeeds without a server call when there is no local credential to revoke.
   *
   * @return A [ClerkResult] containing [Unit] on success, or a [ClerkErrorResponse] on failure.
   */
  suspend fun revokeCurrentDeviceCredential(): ClerkResult<Unit, ClerkErrorResponse> {
    if (Clerk.session?.status?.allowsTrustedDeviceEnrollment != true) {
      return clientFailure(
        "Unable to revoke a trusted device without an active or pending Clerk session."
      )
    }
    val userId = Clerk.user?.id ?: return ClerkResult.success(Unit)

    return when (val result = selectedLocalCredential(id = null, identifierHint = null, userId)) {
      is LocalCredentialResult.Available ->
        when (val revokeResult = revoke(result.credential.id)) {
          is ClerkResult.Success -> ClerkResult.success(Unit)
          is ClerkResult.Failure -> revokeResult
        }
      is LocalCredentialResult.Unavailable -> ClerkResult.success(Unit)
    }
  }

  /**
   * Deletes local trusted-device credentials and keys belonging to [deletedUserId].
   *
   * Call this after the user's account has been deleted so stale local credentials don't linger.
   *
   * @return The number of local credentials that were removed.
   */
  fun forgetLocalCredentials(deletedUserId: String): Int {
    val credentials = storedLocalCredentialsForCurrentApp().filter { it.userId == deletedUserId }
    credentials.forEach { deleteLocalCredential(it) }
    return credentials.size
  }

  /**
   * Signs in with a locally enrolled biometric trusted-device credential.
   *
   * @param id The trusted-device credential ID to use. When omitted, the available local credential
   *   is used.
   * @param identifierHint A local-only user identifier hint used to choose a matching credential.
   * @param promptTitle The title shown in the system biometric prompt.
   * @param promptSubtitle The subtitle shown in the system biometric prompt.
   * @return A [ClerkResult] containing the completed [SignIn] on success, or a [ClerkErrorResponse]
   *   on failure.
   */
  suspend fun signIn(
    id: String? = null,
    identifierHint: String? = null,
    promptTitle: String? = null,
    promptSubtitle: String? = null,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    val localCredential =
      when (val result = selectedLocalCredential(id, identifierHint, userId = null)) {
        is LocalCredentialResult.Available -> result.credential
        is LocalCredentialResult.Unavailable ->
          return clientFailure("Trusted-device sign-in is unavailable.")
      }

    val createResult =
      ClerkApi.signIn.createSignIn(
        mapOf("strategy" to TRUSTED_DEVICE, "trusted_device_id" to localCredential.id)
      )
    val signIn =
      when (createResult) {
        is ClerkResult.Success -> createResult.value
        is ClerkResult.Failure ->
          return handleTrustedDeviceSignInError(createResult, localCredential)
      }

    val challenge =
      signIn.firstFactorVerification?.trustedDeviceChallenge
        ?: return clientFailure("Trusted-device sign-in did not return a challenge.")

    val signature =
      try {
        keyManager.sign(
          clientData = challenge.clientData,
          localKeyId = localCredential.localKeyId,
          policy = localCredential.policy,
          promptTitle = promptTitle ?: DEFAULT_SIGN_IN_PROMPT_TITLE,
          promptSubtitle = promptSubtitle,
        )
      } catch (e: TrustedDeviceKeyManagerException) {
        if (
          e.code == TrustedDeviceKeyManagerException.Code.KEY_INVALIDATED ||
            e.code == TrustedDeviceKeyManagerException.Code.KEY_NOT_FOUND
        ) {
          deleteLocalCredential(localCredential)
        }
        return ClerkResult.unknownFailure(e)
      }

    val attemptResult =
      ClerkApi.signIn.attemptFirstFactor(
        id = signIn.id,
        params =
          mapOf(
            "strategy" to TRUSTED_DEVICE,
            "trusted_device_id" to localCredential.id,
            "client_data" to signature.clientData,
            "signature" to signature.signature,
            "algorithm" to signature.algorithm,
          ),
      )
    return when (attemptResult) {
      is ClerkResult.Success -> attemptResult
      is ClerkResult.Failure -> handleTrustedDeviceSignInError(attemptResult, localCredential)
    }
  }

  /**
   * Validates the local trusted-device credential against the server when possible.
   *
   * Stale local credentials are cleaned up when the server reports them missing.
   *
   * @param id The trusted-device credential ID to validate. When omitted, the available local
   *   credential is used.
   * @param identifierHint A local-only user identifier hint used to choose a matching credential.
   */
  suspend fun validateLocalCredentialIfPossible(
    id: String? = null,
    identifierHint: String? = null,
  ): TrustedDeviceValidationResult {
    if (
      trustedDeviceFeatureUnavailableReason() ==
        TrustedDeviceAvailability.UnavailableReason.ENVIRONMENT_UNAVAILABLE
    ) {
      return TrustedDeviceValidationResult.Inconclusive
    }

    val localCredential =
      when (val result = localCredentialCandidates(id, identifierHint, userId = null)) {
        is LocalCredentialsResult.Available -> result.credentials.first()
        is LocalCredentialsResult.Unavailable ->
          return TrustedDeviceValidationResult.Invalid(result.reason)
      }

    return when (val result = ClerkApi.trustedDevice.validateSignInCredential(localCredential.id)) {
      is ClerkResult.Success ->
        if (result.value.valid) {
          TrustedDeviceValidationResult.Valid
        } else {
          deleteLocalCredential(localCredential)
          TrustedDeviceValidationResult.Invalid(
            TrustedDeviceAvailability.UnavailableReason.SERVER_CREDENTIAL_MISSING
          )
        }
      is ClerkResult.Failure -> {
        if (result.isMissingTrustedDeviceCredential) {
          deleteLocalCredential(localCredential)
          return TrustedDeviceValidationResult.Invalid(
            TrustedDeviceAvailability.UnavailableReason.SERVER_CREDENTIAL_MISSING
          )
        }
        result.trustedDeviceValidationUnavailableReason()?.let {
          return TrustedDeviceValidationResult.Invalid(it)
        }
        TrustedDeviceValidationResult.Inconclusive
      }
    }
  }

  /** Whether biometric-gated trusted-device keys can be created and used on this device. */
  val deviceSupportsBiometricAuthentication: Boolean
    get() = keyManager.isSupported(TrustedDevicePolicy.BIOMETRY_OR_DEVICE_PASSCODE)

  // region Private helpers

  private suspend fun performEnrollment(
    localKey: TrustedDeviceLocalKey,
    appIdentifier: String,
    deviceName: String?,
    promptTitle: String?,
    promptSubtitle: String?,
  ): ClerkResult<TrustedDevice, ClerkErrorResponse> {
    val challenge =
      when (
        val prepareResult =
          ClerkApi.trustedDevice.prepareEnrollment(
            appIdentifier = appIdentifier,
            name = deviceName,
            publicKeyJwk = localKey.publicKeyJwk,
          )
      ) {
        is ClerkResult.Success -> prepareResult.value
        is ClerkResult.Failure -> return prepareResult
      }

    val signature =
      try {
        keyManager.sign(
          clientData = challenge.clientData,
          localKeyId = localKey.localKeyId,
          policy = localKey.policy,
          promptTitle = promptTitle ?: DEFAULT_ENROLLMENT_PROMPT_TITLE,
          promptSubtitle = promptSubtitle,
        )
      } catch (e: TrustedDeviceKeyManagerException) {
        return ClerkResult.unknownFailure(e)
      }

    return ClerkApi.trustedDevice.attemptEnrollment(
      appIdentifier = appIdentifier,
      name = deviceName,
      publicKeyJwk = localKey.publicKeyJwk,
      clientData = signature.clientData,
      signature = signature.signature,
    )
  }

  private suspend fun saveLocalCredential(
    trustedDevice: TrustedDevice,
    localKey: TrustedDeviceLocalKey,
    userId: String,
    identifierHint: String?,
  ): ClerkResult.Failure<ClerkErrorResponse>? {
    return try {
      credentialStore.save(
        TrustedDeviceLocalCredential(
          id = trustedDevice.id,
          localKeyId = localKey.localKeyId,
          userId = userId,
          appIdentifier = trustedDevice.appIdentifier,
          identifierHint = TrustedDeviceLocalCredential.normalizedIdentifierHint(identifierHint),
          policy = localKey.policy,
          createdAt = trustedDevice.createdAt,
          updatedAt = trustedDevice.updatedAt,
        )
      )
      null
    } catch (e: Exception) {
      ClerkApi.trustedDevice.revoke(trustedDevice.id)
      ClerkResult.unknownFailure(e)
    }
  }

  private fun removeOtherLocalCredentialsForCurrentApp(keeping: TrustedDevice) {
    storedLocalCredentialsForCurrentApp()
      .filter { it.id != keeping.id }
      .forEach { credential ->
        runCatching { deleteLocalCredential(credential) }
          .onFailure { ClerkLog.w("Failed to remove replaced trusted-device credential locally.") }
      }
  }

  private sealed interface LocalCredentialResult {
    data class Available(val credential: TrustedDeviceLocalCredential) : LocalCredentialResult

    data class Unavailable(val reason: TrustedDeviceAvailability.UnavailableReason) :
      LocalCredentialResult
  }

  private sealed interface LocalCredentialsResult {
    data class Available(val credentials: List<TrustedDeviceLocalCredential>) :
      LocalCredentialsResult

    data class Unavailable(val reason: TrustedDeviceAvailability.UnavailableReason) :
      LocalCredentialsResult
  }

  @Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
  private suspend fun selectedLocalCredential(
    id: String?,
    identifierHint: String?,
    userId: String?,
  ): LocalCredentialResult {
    val supportedCredentials =
      when (val candidates = localCredentialCandidates(id, identifierHint, userId)) {
        is LocalCredentialsResult.Available -> candidates.credentials
        is LocalCredentialsResult.Unavailable ->
          return LocalCredentialResult.Unavailable(candidates.reason)
      }

    if (Clerk.session?.status != Session.SessionStatus.ACTIVE) {
      return LocalCredentialResult.Available(supportedCredentials.first())
    }
    val activeUserId =
      Clerk.user?.id ?: return LocalCredentialResult.Available(supportedCredentials.first())

    var trustedDevices: List<TrustedDevice>? = null
    var firstUnavailableReason: TrustedDeviceAvailability.UnavailableReason? = null

    for (credential in supportedCredentials) {
      if (credential.userId != activeUserId) {
        return LocalCredentialResult.Available(credential)
      }

      val activeUserTrustedDevices =
        trustedDevices
          ?: when (val listResult = ClerkApi.trustedDevice.list()) {
            is ClerkResult.Success -> listResult.value.also { trustedDevices = it }
            is ClerkResult.Failure ->
              // Reconciliation is best-effort; keep the local credential when listing fails.
              return LocalCredentialResult.Available(credential)
          }

      val trustedDevice = activeUserTrustedDevices.firstOrNull { it.id == credential.id }
      if (trustedDevice == null) {
        deleteLocalCredential(credential)
        firstUnavailableReason =
          firstUnavailableReason
            ?: TrustedDeviceAvailability.UnavailableReason.SERVER_CREDENTIAL_MISSING
        continue
      }

      if (trustedDevice.status != TrustedDevice.Status.ACTIVE) {
        deleteLocalCredential(credential)
        firstUnavailableReason =
          firstUnavailableReason
            ?: TrustedDeviceAvailability.UnavailableReason.SERVER_CREDENTIAL_REVOKED
        continue
      }

      return LocalCredentialResult.Available(credential)
    }

    return LocalCredentialResult.Unavailable(
      firstUnavailableReason
        ?: TrustedDeviceAvailability.UnavailableReason.SERVER_CREDENTIAL_MISSING
    )
  }

  private fun localCredentialCandidates(
    id: String?,
    identifierHint: String?,
    userId: String?,
  ): LocalCredentialsResult {
    trustedDeviceFeatureUnavailableReason()?.let {
      return LocalCredentialsResult.Unavailable(it)
    }

    val localCredentials = candidateLocalCredentials(id, identifierHint, userId)
    if (localCredentials.isEmpty()) {
      return LocalCredentialsResult.Unavailable(
        TrustedDeviceAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
      )
    }

    val credentialsWithKeys = localCredentialsWithExistingKeys(localCredentials)
    if (credentialsWithKeys.isEmpty()) {
      return LocalCredentialsResult.Unavailable(
        TrustedDeviceAvailability.UnavailableReason.LOCAL_KEY_MISSING
      )
    }

    val supportedCredentials = credentialsWithKeys.filter { keyManager.isSupported(it.policy) }
    if (supportedCredentials.isEmpty()) {
      return LocalCredentialsResult.Unavailable(
        TrustedDeviceAvailability.UnavailableReason.BIOMETRIC_AUTHENTICATION_UNAVAILABLE
      )
    }

    return LocalCredentialsResult.Available(supportedCredentials)
  }

  private fun candidateLocalCredentials(
    id: String?,
    identifierHint: String?,
    userId: String?,
  ): List<TrustedDeviceLocalCredential> {
    var credentials = storedLocalCredentialsForCurrentApp()
    if (id != null) {
      credentials = credentials.filter { it.id == id }
    }
    credentials =
      if (userId != null) {
        credentials.filter { it.userId == userId }
      } else {
        credentials.filter { it.matches(identifierHint) }
      }
    return credentials.sortedWith(
      compareByDescending<TrustedDeviceLocalCredential> { it.createdAt }
        .thenByDescending { it.updatedAt }
        .thenByDescending { it.id }
    )
  }

  private fun storedLocalCredentialsForCurrentApp(): List<TrustedDeviceLocalCredential> {
    val appIdentifier = Clerk.applicationId ?: return emptyList()
    return credentialStore.all(appIdentifier)
  }

  private fun localCredentialsWithExistingKeys(
    credentials: List<TrustedDeviceLocalCredential>
  ): List<TrustedDeviceLocalCredential> {
    return credentials.filter { credential ->
      val hasKey = runCatching { keyManager.hasKey(credential.localKeyId) }.getOrDefault(false)
      if (!hasKey) {
        deleteLocalCredential(credential)
      }
      hasKey
    }
  }

  private fun deleteLocalCredential(credential: TrustedDeviceLocalCredential) {
    runCatching { keyManager.deleteKey(credential.localKeyId) }
      .onFailure { ClerkLog.w("Failed to delete trusted-device private key.") }
    credentialStore.delete(credential.id)
  }

  private fun trustedDeviceFeatureUnavailableReason():
    TrustedDeviceAvailability.UnavailableReason? {
    val environment =
      Clerk.environment
        ?: return TrustedDeviceAvailability.UnavailableReason.ENVIRONMENT_UNAVAILABLE
    if (!environment.authConfig.nativeSettings.apiEnabled) {
      return TrustedDeviceAvailability.UnavailableReason.NATIVE_API_DISABLED
    }
    if (!environment.authConfig.nativeSettings.trustedDeviceSignInEnabled) {
      return TrustedDeviceAvailability.UnavailableReason.FEATURE_DISABLED
    }
    return null
  }

  private fun featureUnavailableFailure(): ClerkResult.Failure<ClerkErrorResponse>? {
    val reason = trustedDeviceFeatureUnavailableReason() ?: return null
    val message =
      when (reason) {
        TrustedDeviceAvailability.UnavailableReason.ENVIRONMENT_UNAVAILABLE ->
          "Unable to use trusted-device sign-in before the Clerk environment is loaded."
        TrustedDeviceAvailability.UnavailableReason.NATIVE_API_DISABLED ->
          "Unable to use trusted-device sign-in because Native API is disabled."
        TrustedDeviceAvailability.UnavailableReason.FEATURE_DISABLED ->
          "Unable to use trusted-device sign-in because it is disabled."
        else -> "Trusted-device sign-in is unavailable."
      }
    return clientFailure(message)
  }

  private fun <T : Any> handleTrustedDeviceSignInError(
    failure: ClerkResult.Failure<ClerkErrorResponse>,
    localCredential: TrustedDeviceLocalCredential,
  ): ClerkResult<T, ClerkErrorResponse> {
    if (!failure.isMissingTrustedDeviceCredential) {
      return failure
    }

    deleteLocalCredential(localCredential)
    return clientFailure(
      "This device is no longer trusted. Sign in another way to enroll it again."
    )
  }

  private fun clientFailure(message: String): ClerkResult.Failure<ClerkErrorResponse> {
    return ClerkResult.apiFailure(
      ClerkErrorResponse(
        errors =
          listOf(
            Error(message = message, longMessage = message, code = "trusted_device_client_error")
          )
      )
    )
  }

  private val ClerkResult.Failure<ClerkErrorResponse>.isMissingTrustedDeviceCredential: Boolean
    get() =
      error?.errors.orEmpty().any { error ->
        error.code in MISSING_CREDENTIAL_ERROR_CODES &&
          error.meta?.get("param_name")?.jsonPrimitive?.contentOrNull == "trusted_device_id"
      }

  private fun ClerkResult.Failure<ClerkErrorResponse>.trustedDeviceValidationUnavailableReason():
    TrustedDeviceAvailability.UnavailableReason? {
    return when (error?.errors.orEmpty().firstOrNull()?.code) {
      "native_api_disabled" -> TrustedDeviceAvailability.UnavailableReason.NATIVE_API_DISABLED
      "feature_not_enabled" -> TrustedDeviceAvailability.UnavailableReason.FEATURE_DISABLED
      else -> null
    }
  }

  private val MISSING_CREDENTIAL_ERROR_CODES =
    setOf("form_resource_not_found", "trusted_device_not_registered")

  private const val DEFAULT_SIGN_IN_PROMPT_TITLE = "Sign in"
  private const val DEFAULT_ENROLLMENT_PROMPT_TITLE = "Enroll this device"

  // endregion
}

/** Whether this session status allows enrolling or revoking a trusted device. */
internal val Session.SessionStatus.allowsTrustedDeviceEnrollment: Boolean
  get() = this == Session.SessionStatus.ACTIVE || this == Session.SessionStatus.PENDING
