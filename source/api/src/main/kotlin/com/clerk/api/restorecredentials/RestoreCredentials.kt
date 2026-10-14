package com.clerk.api.restorecredentials

import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import com.clerk.api.Clerk
import com.clerk.api.auth.reportingFailures
import com.clerk.api.auth.types.Strategy
import com.clerk.api.auth.withoutAuthErrorReporting
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.credentials.classifyCreateCredentialFailure
import com.clerk.api.credentials.classifyGetCredentialFailure
import com.clerk.api.log.ClerkLogger
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The main entry point for Google Play Restore Credentials operations.
 *
 * Access via [Clerk.restoreCredentials]. Restore credentials use Android Credential Manager to
 * create a system-managed restore key for the current user and silently redeem that key after the
 * app is transferred to a new Android device.
 */
@Suppress("ReturnCount", "TooManyFunctions")
public object RestoreCredentials {

  internal const val PASSKEY_NAME = "Android restore key"

  @VisibleForTesting
  internal var credentialManager: RestoreCredentialManager = RestoreCredentialManagerImpl()

  private val creationMutex = Mutex()

  @Volatile private var creation: CreationAttempt? = null

  /**
   * Creates a restore credential for the current signed-in user.
   *
   * Cloud backup is attempted by default. If the device does not have end-to-end encrypted backup
   * available, creation is retried automatically with device-to-device restore support only.
   *
   * Android keeps one restore credential per app on a device, so this replaces any restore
   * credential the device already holds, and the device's restore credential belongs to the user
   * who most recently created one. The new passkey is named "Android restore key" so users can
   * recognize it in their profile. When the restore credential this device created before belongs
   * to the same user, its passkey is deleted from their account. A previous restore credential that
   * belongs to a different user cannot be deleted from this session and stays on that user's
   * account.
   *
   * @param isCloudBackupEnabled Whether Google Password Manager should back up the restore key to
   *   the cloud when end-to-end encrypted backup is available.
   */
  public suspend fun create(
    isCloudBackupEnabled: Boolean = true
  ): ClerkResult<Unit, ClerkErrorResponse> = singleFlight {
    createForSignedInUser(isCloudBackupEnabled)
  }

  /**
   * Creates a restore credential for the current signed-in user unless this device already holds
   * one for them.
   *
   * Every [create] registers a new passkey on the user's account, so this skips users who already
   * hold this device's restore credential, from [create] or from signing in with it, until [clear]
   * removes it. It also skips instances where passkeys are not a first factor, because the restore
   * credential could not sign anyone in there, and devices where this install cannot record the
   * restore credential it created.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public suspend fun createIfMissing(): ClerkResult<Unit, ClerkErrorResponse> = singleFlight {
    createForSignedInUserIfMissing()
  }

  /**
   * Silently signs in with a restore credential transferred by Android device setup.
   *
   * This method does not display credential-selection UI. When no restore credential is available,
   * it returns a credential-flow failure so the host app can continue with its normal sign-in UI.
   */
  public suspend fun signIn(): ClerkResult<SignIn, ClerkErrorResponse> =
    Clerk.auth.reportingFailures { signInWithRestoreCredential() }

  /**
   * Signs in with the restore credential the way [signIn] does, but only on the first launch after
   * the app is installed or restored onto a device, and without emitting
   * [com.clerk.api.auth.AuthEvent.Error] when it fails.
   *
   * After this install has signed in with its restore credential, or learned that it has none, it
   * returns a failure without contacting Clerk or Credential Manager. A session that ended without
   * going through this SDK therefore cannot be silently restored on the same install.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public suspend fun signInOnFirstLaunch(): ClerkResult<SignIn, ClerkErrorResponse> {
    if (RestoreCredentialStore.read().signInAttempted) {
      return ClerkResult.unknownFailure(
        IllegalStateException("This install already tried its restore credential.")
      )
    }
    return withoutAuthErrorReporting { signInWithRestoreCredential() }
  }

  private suspend fun signInWithRestoreCredential(): ClerkResult<SignIn, ClerkErrorResponse> {
    if (!isSupportedAndroidVersion) return unsupportedPlatformFailure()
    if (Clerk.activeSession != null) {
      return ClerkResult.unknownFailure(
        IllegalStateException("Restore credential sign-in requires a signed-out client.")
      )
    }
    val context =
      Clerk.applicationContext?.get()
        ?: return ClerkResult.unknownFailure(
          IllegalStateException("Clerk must be initialized before restore credential sign-in.")
        )

    val signIn =
      when (
        val createResult =
          ClerkApi.signIn.createSignIn(
            mapOf("strategy" to Strategy.Passkey.value, "locale" to Clerk.locale.value.orEmpty())
          )
      ) {
        is ClerkResult.Success -> createResult.value
        is ClerkResult.Failure -> return createResult
      }

    val nonce =
      signIn.firstFactorVerification?.nonce?.takeIf { it.isNotBlank() }
        ?: return ClerkResult.unknownFailure(
            IllegalStateException("Restore credential sign-in did not return a challenge.")
          )
          .also { clearSignInAttempt(signIn) }

    return attemptWithRestoreCredential(context, signIn, nonce)
  }

  /**
   * Deletes the app's restore credential from this device and its cloud backup.
   *
   * A restore credential creation that is still running is cancelled first, so it cannot store a
   * restore credential after this returns.
   */
  public suspend fun clear(): ClerkResult<Unit, ClerkErrorResponse> {
    creation?.cancelForSignOut()
    return creationMutex.withLock { clearDeviceKey() }
  }

  internal suspend fun clearSilently() {
    if (clear() is ClerkResult.Failure) {
      ClerkLogger.w("Restore credential cleanup did not complete.")
    }
  }

  internal fun onSignInCompleted(signIn: SignIn) {
    val createdSessionId = signIn.createdSessionId ?: return
    if (RestoreCredentialStore.read().pendingSignInId != signIn.id) return
    RestoreCredentialStore.update { state ->
      if (state.pendingSignInId != signIn.id) {
        state
      } else {
        state.copy(pendingSignInId = null, restoredSessionId = createdSessionId)
      }
    }
  }

  private suspend fun singleFlight(
    create: suspend () -> ClerkResult<Unit, ClerkErrorResponse>
  ): ClerkResult<Unit, ClerkErrorResponse> = creationMutex.withLock {
    coroutineScope {
      val attempt = CreationAttempt(async(start = CoroutineStart.LAZY) { create() })
      creation = attempt
      try {
        attempt.deferred.await()
      } catch (e: CancellationException) {
        if (!attempt.cancelledForSignOut) throw e
        ensureActive()
        ClerkLogger.d("Restore credential creation stopped for sign-out.")
        ClerkResult.unknownFailure(e)
      } finally {
        creation = null
      }
    }
  }

  private suspend fun createForSignedInUserIfMissing(): ClerkResult<Unit, ClerkErrorResponse> {
    if (!Clerk.passkeyFirstFactorIsEnabled) {
      ClerkLogger.d("Skipping restore credential creation: passkeys are not a first factor.")
      return ClerkResult.success(Unit)
    }
    val userId = signedInUserId() ?: return signedOutCreationFailure()
    val state = RestoreCredentialStore.read()
    val sessionId = Clerk.session?.id
    if (sessionId != null && sessionId == state.restoredSessionId) {
      RestoreCredentialStore.update { it.copy(deviceKeyUserId = userId, restoredSessionId = null) }
      ClerkLogger.d(
        "Skipping restore credential creation: this session used the restore credential."
      )
      return ClerkResult.success(Unit)
    }
    if (userId == state.deviceKeyUserId) {
      ClerkLogger.d("Skipping restore credential creation: this device already has one.")
      return ClerkResult.success(Unit)
    }
    if (!RestoreCredentialStore.update { it.copy(deviceKeyUserId = userId) }) {
      ClerkLogger.w(
        "Skipping restore credential creation: this device cannot record it, so every sign-in " +
          "would create another passkey."
      )
      return ClerkResult.unknownFailure(
        IllegalStateException("Restore credential state could not be saved.")
      )
    }
    val forgetUser = {
      RestoreCredentialStore.update { it.copy(deviceKeyUserId = state.deviceKeyUserId) }
    }
    val result =
      try {
        createForSignedInUser(isCloudBackupEnabled = true)
      } catch (e: CancellationException) {
        forgetUser()
        throw e
      }
    if (result is ClerkResult.Failure) forgetUser()
    return result
  }

  private suspend fun createForSignedInUser(
    isCloudBackupEnabled: Boolean
  ): ClerkResult<Unit, ClerkErrorResponse> {
    if (!isSupportedAndroidVersion) return unsupportedPlatformFailure()
    val userId = signedInUserId() ?: return signedOutCreationFailure()
    val context =
      Clerk.applicationContext?.get()
        ?: return ClerkResult.unknownFailure(
          IllegalStateException("Clerk must be initialized before creating a restore credential.")
        )
    val previousPasskey = RestoreCredentialStore.read().createdPasskey

    return when (val prepareResult = ClerkApi.user.createPasskey()) {
      is ClerkResult.Failure -> prepareResult
      is ClerkResult.Success -> {
        val passkeyId = prepareResult.value.id
        val nonce =
          prepareResult.value.verification?.nonce?.takeIf { it.isNotBlank() }
            ?: return ClerkResult.unknownFailure(
              IllegalStateException("Restore credential registration did not return a challenge.")
            )

        val response =
          try {
            createCredentialWithCloudFallback(context, nonce, isCloudBackupEnabled)
          } catch (e: CreateCredentialException) {
            ClerkLogger.e("Restore credential creation failed: ${e.message}")
            return classifyCreateCredentialFailure(e)
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            ClerkLogger.e("Restore credential creation failed: ${e.message}")
            return ClerkResult.unknownFailure(e)
          }

        when (
          val verificationResult =
            ClerkApi.user.attemptPasskeyVerification(
              passkeyId = passkeyId,
              strategy = Strategy.Passkey.value,
              publicKeyCredential = response.responseJson,
            )
        ) {
          is ClerkResult.Success -> {
            RestoreCredentialStore.update {
              it.copy(
                deviceKeyUserId = userId,
                createdPasskey = RestorePasskey(userId = userId, passkeyId = passkeyId),
              )
            }
            nameRestorePasskey(passkeyId)
            previousPasskey?.let { deleteReplacedPasskey(it, userId) }
            ClerkResult.success(Unit)
          }
          is ClerkResult.Failure -> verificationResult
        }
      }
    }
  }

  private suspend fun nameRestorePasskey(passkeyId: String) {
    val result = ClerkApi.user.updatePasskey(passkeyId = passkeyId, name = PASSKEY_NAME)
    if (result is ClerkResult.Failure) {
      ClerkLogger.w("Failed to name the restore credential passkey: ${result.errorMessage}")
    }
  }

  private suspend fun deleteReplacedPasskey(replaced: RestorePasskey, userId: String) {
    if (replaced.userId != userId) {
      ClerkLogger.d(
        "Keeping the replaced restore credential passkey: it belongs to a user who is not signed in."
      )
      return
    }
    val result = ClerkApi.user.deletePasskey(passkeyId = replaced.passkeyId)
    if (result is ClerkResult.Failure) {
      ClerkLogger.w(
        "Failed to delete the replaced restore credential passkey: ${result.errorMessage}"
      )
    }
  }

  private suspend fun clearDeviceKey(): ClerkResult<Unit, ClerkErrorResponse> {
    RestoreCredentialStore.update {
      it.copy(deviceKeyUserId = null, pendingSignInId = null, restoredSessionId = null)
    }
    if (!isSupportedAndroidVersion) return ClerkResult.success(Unit)
    val context =
      Clerk.applicationContext?.get()
        ?: return ClerkResult.unknownFailure(
          IllegalStateException("Clerk must be initialized before clearing a restore credential.")
        )

    return try {
      credentialManager.clearCredentialState(
        context,
        ClearCredentialStateRequest(ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL),
      )
      ClerkResult.success(Unit)
    } catch (e: ClearCredentialException) {
      ClerkLogger.w("Failed to clear restore credential: ${e.message}")
      ClerkResult.unknownFailure(e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      ClerkLogger.w("Failed to clear restore credential: ${e.message}")
      ClerkResult.unknownFailure(e)
    }
  }

  private fun signedInUserId(): String? = Clerk.user?.id?.takeIf { Clerk.activeSession != null }

  private fun <T : Any> signedOutCreationFailure(): ClerkResult<T, ClerkErrorResponse> =
    ClerkResult.unknownFailure(
      IllegalStateException("A signed-in user is required to create a restore credential.")
    )

  private suspend fun createCredentialWithCloudFallback(
    context: android.content.Context,
    requestJson: String,
    isCloudBackupEnabled: Boolean,
  ) =
    try {
      credentialManager.createCredential(
        context,
        CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled),
      )
    } catch (e: E2eeUnavailableException) {
      if (!isCloudBackupEnabled) throw e
      ClerkLogger.d(
        "Encrypted cloud backup is unavailable; retrying restore credential creation without it."
      )
      credentialManager.createCredential(
        context,
        CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled = false),
      )
    }

  private suspend fun attemptWithRestoreCredential(
    context: android.content.Context,
    signIn: SignIn,
    nonce: String,
  ): ClerkResult<SignIn, ClerkErrorResponse> =
    try {
      val request = GetCredentialRequest(listOf(GetRestoreCredentialOption(nonce)))
      val credential = credentialManager.getCredential(context, request).credential
      if (credential !is RestoreCredential) {
        ClerkResult.unknownFailure(
            IllegalStateException("Credential Manager returned a non-restore credential.")
          )
          .also { clearSignInAttempt(signIn) }
      } else {
        signIn
          .attemptFirstFactor(
            SignIn.AttemptFirstFactorParams.Passkey(credential.authenticationResponseJson)
          )
          .also(::rememberSignInAttempt)
      }
    } catch (e: NoCredentialException) {
      RestoreCredentialStore.update { it.copy(signInAttempted = true) }
      restoreCredentialUnavailable(e, signIn)
    } catch (e: GetCredentialException) {
      restoreCredentialUnavailable(e, signIn)
    } catch (e: CancellationException) {
      runCatching { clearSignInAttempt(signIn) }.exceptionOrNull()?.let(e::addSuppressed)
      throw e
    } catch (e: Exception) {
      ClerkLogger.e("Restore credential sign-in failed: ${e.message}")
      ClerkResult.unknownFailure(e).also { clearSignInAttempt(signIn) }
    }

  private fun restoreCredentialUnavailable(
    exception: GetCredentialException,
    signIn: SignIn,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLogger.d("Restore credential sign-in is unavailable: ${exception.message}")
    return classifyGetCredentialFailure(exception, listOf(SignIn.CredentialType.PASSKEY)).also {
      clearSignInAttempt(signIn)
    }
  }

  private fun rememberSignInAttempt(result: ClerkResult<SignIn, ClerkErrorResponse>) {
    when {
      result is ClerkResult.Success ->
        RestoreCredentialStore.update { state ->
          val signIn = result.value
          if (signIn.status == SignIn.Status.COMPLETE) {
            state.copy(signInAttempted = true, restoredSessionId = signIn.createdSessionId)
          } else {
            state.copy(signInAttempted = true, pendingSignInId = signIn.id)
          }
        }
      result is ClerkResult.Failure && result.errorType == ClerkResult.Failure.ErrorType.API ->
        RestoreCredentialStore.update { it.copy(signInAttempted = true) }
    }
  }

  private fun clearSignInAttempt(signIn: SignIn) {
    Clerk.mutateClient { client ->
      client.copy(signIn = null).takeIf { client.signIn?.id == signIn.id }
    }
  }

  private val isSupportedAndroidVersion: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

  private fun <T : Any> unsupportedPlatformFailure(): ClerkResult<T, ClerkErrorResponse> {
    return ClerkResult.unknownFailure(CredentialFlowException.ProviderUnavailable())
  }
}

private class CreationAttempt(val deferred: Deferred<ClerkResult<Unit, ClerkErrorResponse>>) {
  @Volatile
  var cancelledForSignOut: Boolean = false
    private set

  fun cancelForSignOut() {
    cancelledForSignOut = true
    deferred.cancel()
  }
}
