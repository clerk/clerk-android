package com.clerk.api.restorecredentials

import android.os.Build
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
import com.clerk.api.Constants.Strategy.PASSKEY
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.credentials.classifyCreateCredentialFailure
import com.clerk.api.credentials.classifyGetCredentialFailure
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor

/**
 * The main entry point for Google Play Restore Credentials operations.
 *
 * Access via [Clerk.restoreCredentials]. Restore credentials use Android Credential Manager to
 * create a system-managed restore key for the current user and silently redeem that key after the
 * app is transferred to a new Android device.
 */
@Suppress("ReturnCount")
object RestoreCredentials {

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
  suspend fun create(isCloudBackupEnabled: Boolean = true): ClerkResult<Unit, ClerkErrorResponse> {
    if (!isSupportedAndroidVersion) return unsupportedPlatformFailure()
    if (Clerk.activeSession == null || Clerk.user == null) {
      return ClerkResult.unknownFailure(
        IllegalStateException("A signed-in user is required to create a restore credential.")
      )
    }
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
            ClerkLog.e("Restore credential creation failed: ${e.message}")
            return classifyCreateCredentialFailure(e)
          } catch (e: Exception) {
            ClerkLog.e("Restore credential creation failed: ${e.message}")
            return ClerkResult.unknownFailure(e)
          }

        when (
          val verificationResult =
            ClerkApi.user.attemptPasskeyVerification(
              passkeyId = prepareResult.value.id,
              strategy = PASSKEY,
              publicKeyCredential = response.responseJson,
            )
        ) {
          is ClerkResult.Success -> ClerkResult.success(Unit)
          is ClerkResult.Failure -> verificationResult
        }
      }
    }
  }

  /**
   * Silently signs in with a restore credential transferred by Android device setup.
   *
   * This method does not display credential-selection UI. When no restore credential is available,
   * it returns a credential-flow failure so the host app can continue with its normal sign-in UI.
   */
  suspend fun signIn(): ClerkResult<SignIn, ClerkErrorResponse> {
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
            mapOf("strategy" to PASSKEY, "locale" to Clerk.locale.value.orEmpty())
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

    return try {
      val request = GetCredentialRequest(listOf(GetRestoreCredentialOption(nonce)))
      val credential = credentialManager.getCredential(context, request).credential
      if (credential !is RestoreCredential) {
        ClerkResult.unknownFailure(
            IllegalStateException("Credential Manager returned a non-restore credential.")
          )
          .also { clearSignInAttempt(signIn) }
      } else {
        signIn.attemptFirstFactor(
          SignIn.AttemptFirstFactorParams.Passkey(credential.authenticationResponseJson)
        )
      }
    } catch (e: GetCredentialException) {
      ClerkLog.d("Restore credential sign-in is unavailable: ${e.message}")
      classifyGetCredentialFailure(e, listOf(SignIn.CredentialType.PASSKEY)).also {
        clearSignInAttempt(signIn)
      }
    } catch (e: Exception) {
      ClerkLog.e("Restore credential sign-in failed: ${e.message}")
      ClerkResult.unknownFailure(e).also { clearSignInAttempt(signIn) }
    }
  }

  /** Deletes the app's restore credential from this device and its cloud backup. */
  suspend fun clear(): ClerkResult<Unit, ClerkErrorResponse> {
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
      ClerkLog.w("Failed to clear restore credential: ${e.message}")
      ClerkResult.unknownFailure(e)
    } catch (e: Exception) {
      ClerkLog.w("Failed to clear restore credential: ${e.message}")
      ClerkResult.unknownFailure(e)
    }
  }

  internal suspend fun clearSilently() {
    if (clear() is ClerkResult.Failure) {
      ClerkLog.w("Restore credential cleanup did not complete.")
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
      ClerkLog.d(
        "Encrypted cloud backup is unavailable; retrying restore credential creation without it."
      )
      credentialManager.createCredential(
        context,
        CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled = false),
      )
    }

  private fun clearSignInAttempt(signIn: SignIn) {
    if (!Clerk.clientInitialized || Clerk.client.signIn?.id != signIn.id) return
    Clerk.updateClient(Clerk.client.copy(signIn = null))
  }

  private val isSupportedAndroidVersion: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

  private fun <T : Any> unsupportedPlatformFailure(): ClerkResult<T, ClerkErrorResponse> {
    return ClerkResult.unknownFailure(CredentialFlowException.ProviderUnavailable())
  }
}
