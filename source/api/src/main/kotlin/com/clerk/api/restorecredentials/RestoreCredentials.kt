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
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import kotlin.coroutines.cancellation.CancellationException

/**
 * The main entry point for Google Play Restore Credentials operations.
 *
 * Access via [Clerk.restoreCredentials]. Restore credentials use Android Credential Manager to
 * create a system-managed restore key for the current user and silently redeem that key after the
 * app is transferred to a new Android device.
 */
@Suppress("ReturnCount")
public object RestoreCredentials {

  @VisibleForTesting
  internal var credentialManager: RestoreCredentialManager = RestoreCredentialManagerImpl()

  /**
   * Creates a restore credential for the current signed-in user.
   *
   * Cloud backup is attempted by default. If the device does not have end-to-end encrypted backup
   * available, creation is retried automatically with device-to-device restore support only.
   *
   * @param isCloudBackupEnabled Whether Google Password Manager should back up the restore key to
   *   the cloud when end-to-end encrypted backup is available.
   */
  public suspend fun create(
    isCloudBackupEnabled: Boolean = true
  ): ClerkResult<Unit, ClerkErrorResponse> {
    if (!isSupportedAndroidVersion) return unsupportedPlatformFailure()
    val userId =
      Clerk.user?.id?.takeIf { Clerk.activeSession != null }
        ?: return ClerkResult.unknownFailure(
          IllegalStateException("A signed-in user is required to create a restore credential.")
        )
    val context =
      Clerk.applicationContext?.get()
        ?: return ClerkResult.unknownFailure(
          IllegalStateException("Clerk must be initialized before creating a restore credential.")
        )

    return when (val prepareResult = ClerkApi.user.createPasskey()) {
      is ClerkResult.Failure -> prepareResult
      is ClerkResult.Success -> {
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
              passkeyId = prepareResult.value.id,
              strategy = Strategy.Passkey.value,
              publicKeyCredential = response.responseJson,
            )
        ) {
          is ClerkResult.Success -> {
            RestoreCredentialOwner.userId = userId
            ClerkResult.success(Unit)
          }
          is ClerkResult.Failure -> verificationResult
        }
      }
    }
  }

  /**
   * Creates a restore credential for the current signed-in user unless this device already holds
   * one for them.
   *
   * Every [create] registers a new passkey on the user's account, so this skips users who already
   * have a restore credential on this device, from [create] or [signIn], until [clear] removes it.
   * It also skips instances that do not have passkeys enabled.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public suspend fun createIfMissing(): ClerkResult<Unit, ClerkErrorResponse> {
    if (!Clerk.passkeyIsEnabled) {
      ClerkLogger.d("Skipping restore credential creation: passkeys are disabled.")
      return ClerkResult.success(Unit)
    }
    val userId = Clerk.user?.id
    if (userId != null && userId == RestoreCredentialOwner.userId) {
      ClerkLogger.d("Skipping restore credential creation: this device already has one.")
      return ClerkResult.success(Unit)
    }
    return create()
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
   * Runs [signIn] without emitting [com.clerk.api.auth.AuthEvent.Error] when it fails, for callers
   * that try it on their own and fall back to their sign-in UI without telling the user.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public suspend fun signInWithoutReportingFailures(): ClerkResult<SignIn, ClerkErrorResponse> =
    withoutAuthErrorReporting {
      signInWithRestoreCredential()
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

  /** Deletes the app's restore credential from this device and its cloud backup. */
  public suspend fun clear(): ClerkResult<Unit, ClerkErrorResponse> {
    RestoreCredentialOwner.userId = null
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

  internal suspend fun clearSilently() {
    if (clear() is ClerkResult.Failure) {
      ClerkLogger.w("Restore credential cleanup did not complete.")
    }
  }

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
          .also { if (it is ClerkResult.Success) RestoreCredentialOwner.rememberUserOf(it.value) }
      }
    } catch (e: GetCredentialException) {
      ClerkLogger.d("Restore credential sign-in is unavailable: ${e.message}")
      classifyGetCredentialFailure(e, listOf(SignIn.CredentialType.PASSKEY)).also {
        clearSignInAttempt(signIn)
      }
    } catch (e: CancellationException) {
      runCatching { clearSignInAttempt(signIn) }.exceptionOrNull()?.let(e::addSuppressed)
      throw e
    } catch (e: Exception) {
      ClerkLogger.e("Restore credential sign-in failed: ${e.message}")
      ClerkResult.unknownFailure(e).also { clearSignInAttempt(signIn) }
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

private object RestoreCredentialOwner {
  var userId: String?
    get() = StorageHelper.loadValue(StorageKey.RESTORE_CREDENTIAL_USER_ID)
    set(value) {
      if (value == null) {
        StorageHelper.deleteValue(StorageKey.RESTORE_CREDENTIAL_USER_ID)
      } else {
        StorageHelper.saveValue(StorageKey.RESTORE_CREDENTIAL_USER_ID, value)
      }
    }

  fun rememberUserOf(signIn: SignIn) {
    if (signIn.status != SignIn.Status.COMPLETE) return
    val session = Clerk.sessionsFlow.value.firstOrNull { it.id == signIn.createdSessionId }
    userId = session?.user?.id ?: return
  }
}
