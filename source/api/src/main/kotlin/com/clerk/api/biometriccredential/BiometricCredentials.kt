package com.clerk.api.biometriccredential

import androidx.annotation.RestrictTo
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
 * The main entry point for biometric-credential (biometric sign-in) credential operations.
 *
 * Access via [Clerk.biometricCredentials].
 *
 * Enrolling generates a biometric-gated private key in the Android Keystore and registers its
 * public key with Clerk. Signing in signs a server challenge with that key after the user passes
 * the system biometric prompt. The private key never leaves the device.
 *
 * ### Example usage:
 * ```kotlin
 * // Enroll the current device while signed in
 * Clerk.biometricCredentials.enroll()
 *
 * // Later, sign in with biometrics
 * Clerk.biometricCredentials.signIn().onSuccess { signIn ->
 *   // Session created
 * }
 * ```
 */
@Suppress("TooManyFunctions", "ReturnCount")
object BiometricCredentials {

  @VisibleForTesting
  internal var keyManager: BiometricCredentialKeyManager = DefaultBiometricCredentialKeyManager

  @VisibleForTesting
  internal var credentialStore: BiometricCredentialLocalCredentialStore =
    DefaultBiometricCredentialLocalCredentialStore

  /**
   * Lists active biometric credentials for the signed-in user.
   *
   * @return A [ClerkResult] containing the list of [BiometricCredential] credentials on success, or
   *   a [ClerkErrorResponse] on failure.
   */
  suspend fun list(): ClerkResult<List<BiometricCredential>, ClerkErrorResponse> {
    return ClerkApi.biometricCredential.list()
  }

  /**
   * Returns local biometric-credential sign-in availability.
   *
   * When a Clerk session is active, this also reconciles the local credential with the server.
   * Without an active session, this reports whether the local biometric-gated credential can be
   * used to start biometric-credential sign-in.
   *
   * @param id The biometric credential ID to check. When omitted, the available local credential is
   *   used.
   * @param identifierHint A local-only user identifier hint used to choose a matching credential.
   */
  suspend fun availability(
    id: String? = null,
    identifierHint: String? = null,
  ): BiometricCredentialAvailability {
    return when (val result = selectedLocalCredential(id, identifierHint, userId = null)) {
      is LocalCredentialResult.Available -> BiometricCredentialAvailability.Available
      is LocalCredentialResult.Unavailable ->
        BiometricCredentialAvailability.Unavailable(result.reason)
    }
  }

  /**
   * Returns biometric-credential sign-in availability for the current signed-in user, reconciling
   * the local credential with the server.
   */
  suspend fun currentUserAvailability(): BiometricCredentialAvailability {
    val userId =
      Clerk.user?.id
        ?: return BiometricCredentialAvailability.Unavailable(
          BiometricCredentialAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
        )

    return when (val result = selectedLocalCredential(id = null, identifierHint = null, userId)) {
      is LocalCredentialResult.Available -> BiometricCredentialAvailability.Available
      is LocalCredentialResult.Unavailable ->
        BiometricCredentialAvailability.Unavailable(result.reason)
    }
  }

  /**
   * Returns local biometric-credential sign-in availability without reconciling with the server.
   */
  fun localAvailability(
    id: String? = null,
    identifierHint: String? = null,
  ): BiometricCredentialAvailability {
    return when (val result = localCredentialCandidates(id, identifierHint, userId = null)) {
      is LocalCredentialsResult.Available -> BiometricCredentialAvailability.Available
      is LocalCredentialsResult.Unavailable ->
        BiometricCredentialAvailability.Unavailable(result.reason)
    }
  }

  /**
   * Returns local biometric-credential sign-in availability for the current signed-in user without
   * reconciling with the server.
   */
  fun currentUserLocalAvailability(): BiometricCredentialAvailability {
    val userId =
      Clerk.user?.id
        ?: return BiometricCredentialAvailability.Unavailable(
          BiometricCredentialAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
        )

    return when (val result = localCredentialCandidates(id = null, identifierHint = null, userId)) {
      is LocalCredentialsResult.Available -> BiometricCredentialAvailability.Available
      is LocalCredentialsResult.Unavailable ->
        BiometricCredentialAvailability.Unavailable(result.reason)
    }
  }

  /**
   * Enrolls the current app installation as a biometric credential.
   *
   * This requires an active or pending Clerk session. The generated private key stays on the
   * device.
   *
   * @param name A human-readable name stored with the biometric credential.
   * @param identifierHint A local-only user identifier hint for selecting this credential later.
   * @param policy The local authentication policy used to protect the generated private key.
   *   Defaults to requiring biometric availability while allowing device credential fallback during
   *   authentication.
   * @param promptTitle The title shown in the system biometric prompt.
   * @param promptSubtitle The subtitle shown in the system biometric prompt.
   * @return A [ClerkResult] containing the enrolled [BiometricCredential] on success, or a
   *   [ClerkErrorResponse] on failure.
   */
  suspend fun enroll(
    name: String? = null,
    identifierHint: String? = null,
    policy: BiometricCredentialPolicy = BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE,
    promptTitle: String? = null,
    promptSubtitle: String? = null,
  ): ClerkResult<BiometricCredential, ClerkErrorResponse> {
    if (Clerk.session?.status?.allowsBiometricCredentialEnrollment != true) {
      return clientFailure(
        "Unable to enroll a biometric credential without an active or pending Clerk session."
      )
    }
    featureUnavailableFailure()?.let {
      return it
    }
    val appIdentifier =
      Clerk.applicationId
        ?: return clientFailure(
          "Unable to enroll a biometric credential without an application ID."
        )
    val userId =
      Clerk.user?.id
        ?: return clientFailure(
          "Unable to enroll a biometric credential without a user for the current session."
        )

    val localKey =
      try {
        keyManager.createKey(policy)
      } catch (e: BiometricCredentialKeyManagerException) {
        return ClerkResult.unknownFailure(e)
      }

    val enrollmentResult =
      performEnrollment(localKey, appIdentifier, name, promptTitle, promptSubtitle)
    return when (enrollmentResult) {
      is ClerkResult.Success -> {
        val biometricCredential = enrollmentResult.value
        saveLocalCredential(biometricCredential, localKey, userId, identifierHint)?.let { failure ->
          runCatching { keyManager.deleteKey(localKey.localKeyId) }
          return failure
        }
        removeOtherLocalCredentialsForCurrentApp(userId = userId, keeping = biometricCredential)
        enrollmentResult
      }
      is ClerkResult.Failure -> {
        runCatching { keyManager.deleteKey(localKey.localKeyId) }
        enrollmentResult
      }
    }
  }

  /**
   * Revokes a biometric credential for the signed-in user.
   *
   * @param id The biometric credential ID to revoke.
   * @return A [ClerkResult] containing the revoked [BiometricCredential] on success, or a
   *   [ClerkErrorResponse] on failure.
   */
  suspend fun revoke(id: String): ClerkResult<BiometricCredential, ClerkErrorResponse> {
    val result = ClerkApi.biometricCredential.revoke(id)
    if (result is ClerkResult.Success) {
      credentialStore.credential(id)?.let { deleteLocalCredential(it) }
    }
    return result
  }

  /**
   * Revokes the available local biometric credential for the current signed-in user, if one exists.
   *
   * Succeeds without a server call when there is no local credential to revoke.
   *
   * @return A [ClerkResult] containing [Unit] on success, or a [ClerkErrorResponse] on failure.
   */
  suspend fun revokeCurrentBiometricCredential(): ClerkResult<Unit, ClerkErrorResponse> {
    if (Clerk.session?.status?.allowsBiometricCredentialEnrollment != true) {
      return clientFailure(
        "Unable to revoke a biometric credential without an active or pending Clerk session."
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
   * Deletes local biometric credentials and keys belonging to [deletedUserId].
   *
   * Call this after the user's account has been deleted so stale local credentials don't linger.
   *
   * @return The number of local credentials that were removed.
   */
  fun forgetLocalCredentials(deletedUserId: String): Int {
    val credentials = storedLocalCredentialsForCurrentApp().filter { it.userId == deletedUserId }
    credentials.forEach { deleteLocalCredential(it, propagateKeyDeletionFailure = true) }
    return credentials.size
  }

  /**
   * Deletes account-scoped local credentials, retaining failed cleanup work for the next SDK
   * initialization.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  fun forgetLocalCredentialsAfterAccountDeletion(deletedUserId: String): Int {
    BiometricCredentialPendingCleanupStore.add(deletedUserId)
    return forgetLocalCredentials(deletedUserId).also {
      BiometricCredentialPendingCleanupStore.remove(deletedUserId)
    }
  }

  internal fun retryPendingLocalCredentialCleanup() {
    BiometricCredentialPendingCleanupStore.all().forEach { deletedUserId ->
      runCatching { forgetLocalCredentials(deletedUserId) }
        .onSuccess { BiometricCredentialPendingCleanupStore.remove(deletedUserId) }
        .onFailure { ClerkLog.w("Failed to retry biometric-credential local credential cleanup.") }
    }
  }

  /**
   * Signs in with a locally enrolled biometric credential.
   *
   * @param id The biometric credential ID to use. When omitted, the available local credential is
   *   used.
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
          return clientFailure("Biometric-credential sign-in is unavailable.")
      }

    val createResult =
      ClerkApi.signIn.createSignIn(
        mapOf(
          "strategy" to TRUSTED_DEVICE,
          "trusted_device_id" to localCredential.id,
          "locale" to Clerk.locale.value.orEmpty(),
        )
      )
    val signIn =
      when (createResult) {
        is ClerkResult.Success -> createResult.value
        is ClerkResult.Failure -> return handleBiometricSignInError(createResult, localCredential)
      }

    val challenge =
      signIn.firstFactorVerification?.biometricCredentialChallenge
        ?: return clientFailure("Biometric-credential sign-in did not return a challenge.")

    val signature =
      try {
        keyManager.sign(
          clientData = challenge.clientData,
          localKeyId = localCredential.localKeyId,
          policy = localCredential.policy,
          promptTitle = promptTitle ?: DEFAULT_SIGN_IN_PROMPT_TITLE,
          promptSubtitle = promptSubtitle,
        )
      } catch (e: BiometricCredentialKeyManagerException) {
        if (
          e.code == BiometricCredentialKeyManagerException.Code.KEY_INVALIDATED ||
            e.code == BiometricCredentialKeyManagerException.Code.KEY_NOT_FOUND
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
      is ClerkResult.Failure -> handleBiometricSignInError(attemptResult, localCredential)
    }
  }

  /**
   * Validates the local biometric credential against the server when possible.
   *
   * Stale local credentials are cleaned up when the server reports them missing.
   *
   * @param id The biometric credential ID to validate. When omitted, the available local credential
   *   is used.
   * @param identifierHint A local-only user identifier hint used to choose a matching credential.
   */
  suspend fun validateLocalCredentialIfPossible(
    id: String? = null,
    identifierHint: String? = null,
  ): BiometricCredentialValidationResult {
    if (
      biometricCredentialFeatureUnavailableReason() ==
        BiometricCredentialAvailability.UnavailableReason.ENVIRONMENT_UNAVAILABLE
    ) {
      return BiometricCredentialValidationResult.Inconclusive
    }

    val localCredential =
      when (val result = localCredentialCandidates(id, identifierHint, userId = null)) {
        is LocalCredentialsResult.Available -> result.credentials.first()
        is LocalCredentialsResult.Unavailable ->
          return BiometricCredentialValidationResult.Invalid(result.reason)
      }

    return when (
      val result = ClerkApi.biometricCredential.validateSignInCredential(localCredential.id)
    ) {
      is ClerkResult.Success ->
        if (result.value.valid) {
          BiometricCredentialValidationResult.Valid
        } else {
          deleteLocalCredential(localCredential)
          BiometricCredentialValidationResult.Invalid(
            BiometricCredentialAvailability.UnavailableReason.SERVER_CREDENTIAL_MISSING
          )
        }
      is ClerkResult.Failure -> {
        if (result.isMissingBiometricCredential) {
          deleteLocalCredential(localCredential)
          return BiometricCredentialValidationResult.Invalid(
            BiometricCredentialAvailability.UnavailableReason.SERVER_CREDENTIAL_MISSING
          )
        }
        result.biometricCredentialValidationUnavailableReason()?.let {
          return BiometricCredentialValidationResult.Invalid(it)
        }
        BiometricCredentialValidationResult.Inconclusive
      }
    }
  }

  /** Whether biometric-gated biometric-credential keys can be created and used on this device. */
  val deviceSupportsBiometricAuthentication: Boolean
    get() = keyManager.isSupported(BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE)

  // region Private helpers

  private suspend fun performEnrollment(
    localKey: BiometricCredentialLocalKey,
    appIdentifier: String,
    name: String?,
    promptTitle: String?,
    promptSubtitle: String?,
  ): ClerkResult<BiometricCredential, ClerkErrorResponse> {
    val challenge =
      when (
        val prepareResult =
          ClerkApi.biometricCredential.prepareEnrollment(
            appIdentifier = appIdentifier,
            name = name,
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
      } catch (e: BiometricCredentialKeyManagerException) {
        return ClerkResult.unknownFailure(e)
      }

    return ClerkApi.biometricCredential.attemptEnrollment(
      appIdentifier = appIdentifier,
      name = name,
      publicKeyJwk = localKey.publicKeyJwk,
      clientData = signature.clientData,
      signature = signature.signature,
    )
  }

  private suspend fun saveLocalCredential(
    biometricCredential: BiometricCredential,
    localKey: BiometricCredentialLocalKey,
    userId: String,
    identifierHint: String?,
  ): ClerkResult.Failure<ClerkErrorResponse>? {
    return try {
      credentialStore.save(
        BiometricCredentialLocalCredential(
          id = biometricCredential.id,
          localKeyId = localKey.localKeyId,
          userId = userId,
          appIdentifier = biometricCredential.appIdentifier,
          identifierHint =
            BiometricCredentialLocalCredential.normalizedIdentifierHint(identifierHint),
          policy = localKey.policy,
          createdAt = biometricCredential.createdAt,
          updatedAt = biometricCredential.updatedAt,
        )
      )
      null
    } catch (e: Exception) {
      ClerkApi.biometricCredential.revoke(biometricCredential.id)
      ClerkResult.unknownFailure(e)
    }
  }

  @VisibleForTesting
  internal fun removeOtherLocalCredentialsForCurrentApp(
    userId: String,
    keeping: BiometricCredential,
  ) {
    storedLocalCredentialsForCurrentApp()
      .filter { it.userId == userId && it.id != keeping.id }
      .forEach { credential ->
        runCatching { deleteLocalCredential(credential) }
          .onFailure { ClerkLog.w("Failed to remove replaced biometric credential locally.") }
      }
  }

  private sealed interface LocalCredentialResult {
    data class Available(val credential: BiometricCredentialLocalCredential) : LocalCredentialResult

    data class Unavailable(val reason: BiometricCredentialAvailability.UnavailableReason) :
      LocalCredentialResult
  }

  private sealed interface LocalCredentialsResult {
    data class Available(val credentials: List<BiometricCredentialLocalCredential>) :
      LocalCredentialsResult

    data class Unavailable(val reason: BiometricCredentialAvailability.UnavailableReason) :
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

    var biometricCredentials: List<BiometricCredential>? = null
    var firstUnavailableReason: BiometricCredentialAvailability.UnavailableReason? = null

    for (credential in supportedCredentials) {
      if (credential.userId != activeUserId) {
        return LocalCredentialResult.Available(credential)
      }

      val activeUserBiometricCredentials =
        biometricCredentials
          ?: when (val listResult = ClerkApi.biometricCredential.list()) {
            is ClerkResult.Success -> listResult.value.also { biometricCredentials = it }
            is ClerkResult.Failure ->
              // Reconciliation is best-effort; keep the local credential when listing fails.
              return LocalCredentialResult.Available(credential)
          }

      val biometricCredential = activeUserBiometricCredentials.firstOrNull {
        it.id == credential.id
      }
      if (biometricCredential == null) {
        deleteLocalCredential(credential)
        firstUnavailableReason =
          firstUnavailableReason
            ?: BiometricCredentialAvailability.UnavailableReason.SERVER_CREDENTIAL_MISSING
        continue
      }

      if (biometricCredential.status != BiometricCredential.Status.ACTIVE) {
        deleteLocalCredential(credential)
        firstUnavailableReason =
          firstUnavailableReason
            ?: BiometricCredentialAvailability.UnavailableReason.SERVER_CREDENTIAL_REVOKED
        continue
      }

      return LocalCredentialResult.Available(credential)
    }

    return LocalCredentialResult.Unavailable(
      firstUnavailableReason
        ?: BiometricCredentialAvailability.UnavailableReason.SERVER_CREDENTIAL_MISSING
    )
  }

  private fun localCredentialCandidates(
    id: String?,
    identifierHint: String?,
    userId: String?,
  ): LocalCredentialsResult {
    biometricCredentialFeatureUnavailableReason()?.let {
      return LocalCredentialsResult.Unavailable(it)
    }

    val localCredentials = candidateLocalCredentials(id, identifierHint, userId)
    if (localCredentials.isEmpty()) {
      return LocalCredentialsResult.Unavailable(
        BiometricCredentialAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
      )
    }

    val credentialsWithKeys = localCredentialsWithExistingKeys(localCredentials)
    if (credentialsWithKeys.isEmpty()) {
      return LocalCredentialsResult.Unavailable(
        BiometricCredentialAvailability.UnavailableReason.LOCAL_KEY_MISSING
      )
    }

    val supportedCredentials = credentialsWithKeys.filter { keyManager.isSupported(it.policy) }
    if (supportedCredentials.isEmpty()) {
      return LocalCredentialsResult.Unavailable(
        BiometricCredentialAvailability.UnavailableReason.BIOMETRIC_AUTHENTICATION_UNAVAILABLE
      )
    }

    return LocalCredentialsResult.Available(supportedCredentials)
  }

  private fun candidateLocalCredentials(
    id: String?,
    identifierHint: String?,
    userId: String?,
  ): List<BiometricCredentialLocalCredential> {
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
      compareByDescending<BiometricCredentialLocalCredential> { it.createdAt }
        .thenByDescending { it.updatedAt }
        .thenByDescending { it.id }
    )
  }

  private fun storedLocalCredentialsForCurrentApp(): List<BiometricCredentialLocalCredential> {
    val appIdentifier = Clerk.applicationId ?: return emptyList()
    return credentialStore.all(appIdentifier)
  }

  private fun localCredentialsWithExistingKeys(
    credentials: List<BiometricCredentialLocalCredential>
  ): List<BiometricCredentialLocalCredential> {
    return credentials.filter { credential ->
      val hasKey = runCatching { keyManager.hasKey(credential.localKeyId) }.getOrDefault(false)
      if (!hasKey) {
        deleteLocalCredential(credential)
      }
      hasKey
    }
  }

  private fun deleteLocalCredential(
    credential: BiometricCredentialLocalCredential,
    propagateKeyDeletionFailure: Boolean = false,
  ) {
    val keyDeletionResult = runCatching { keyManager.deleteKey(credential.localKeyId) }
    keyDeletionResult.onFailure { ClerkLog.w("Failed to delete biometric-credential private key.") }
    if (propagateKeyDeletionFailure) {
      keyDeletionResult.getOrThrow()
    }
    credentialStore.delete(credential.id)
  }

  private fun biometricCredentialFeatureUnavailableReason():
    BiometricCredentialAvailability.UnavailableReason? {
    val environment =
      Clerk.environment
        ?: return BiometricCredentialAvailability.UnavailableReason.ENVIRONMENT_UNAVAILABLE
    if (!environment.authConfig.nativeSettings.apiEnabled) {
      return BiometricCredentialAvailability.UnavailableReason.NATIVE_API_DISABLED
    }
    if (!environment.authConfig.nativeSettings.biometricSignInEnabled) {
      return BiometricCredentialAvailability.UnavailableReason.FEATURE_DISABLED
    }
    return null
  }

  private fun featureUnavailableFailure(): ClerkResult.Failure<ClerkErrorResponse>? {
    val reason = biometricCredentialFeatureUnavailableReason() ?: return null
    val message =
      when (reason) {
        BiometricCredentialAvailability.UnavailableReason.ENVIRONMENT_UNAVAILABLE ->
          "Unable to use biometric-credential sign-in before the Clerk environment is loaded."
        BiometricCredentialAvailability.UnavailableReason.NATIVE_API_DISABLED ->
          "Unable to use biometric-credential sign-in because Native API is disabled."
        BiometricCredentialAvailability.UnavailableReason.FEATURE_DISABLED ->
          "Unable to use biometric-credential sign-in because it is disabled."
        else -> "Biometric-credential sign-in is unavailable."
      }
    return clientFailure(message)
  }

  private fun <T : Any> handleBiometricSignInError(
    failure: ClerkResult.Failure<ClerkErrorResponse>,
    localCredential: BiometricCredentialLocalCredential,
  ): ClerkResult<T, ClerkErrorResponse> {
    if (!failure.isMissingBiometricCredential) {
      return failure
    }

    deleteLocalCredential(localCredential)
    return clientFailure(
      "Biometric sign-in is no longer set up on this device. Sign in another way to enable it again."
    )
  }

  private fun clientFailure(message: String): ClerkResult.Failure<ClerkErrorResponse> {
    return ClerkResult.apiFailure(
      ClerkErrorResponse(
        errors =
          listOf(
            Error(
              message = message,
              longMessage = message,
              code = "biometric_credential_client_error",
            )
          )
      )
    )
  }

  private val ClerkResult.Failure<ClerkErrorResponse>.isMissingBiometricCredential: Boolean
    get() =
      error?.errors.orEmpty().any { error ->
        error.code in MISSING_CREDENTIAL_ERROR_CODES &&
          error.meta?.get("param_name")?.jsonPrimitive?.contentOrNull == "trusted_device_id"
      }

  private fun ClerkResult.Failure<ClerkErrorResponse>
    .biometricCredentialValidationUnavailableReason():
    BiometricCredentialAvailability.UnavailableReason? {
    return when (error?.errors.orEmpty().firstOrNull()?.code) {
      "native_api_disabled" -> BiometricCredentialAvailability.UnavailableReason.NATIVE_API_DISABLED
      "feature_not_enabled" -> BiometricCredentialAvailability.UnavailableReason.FEATURE_DISABLED
      else -> null
    }
  }

  private val MISSING_CREDENTIAL_ERROR_CODES =
    setOf("form_resource_not_found", "trusted_device_not_registered")

  private const val DEFAULT_SIGN_IN_PROMPT_TITLE = "Sign in"
  private const val DEFAULT_ENROLLMENT_PROMPT_TITLE = "Enroll this device"

  // endregion
}

/** Whether this session status allows enrolling or revoking a biometric credential. */
internal val Session.SessionStatus.allowsBiometricCredentialEnrollment: Boolean
  get() = this == Session.SessionStatus.ACTIVE || this == Session.SessionStatus.PENDING
