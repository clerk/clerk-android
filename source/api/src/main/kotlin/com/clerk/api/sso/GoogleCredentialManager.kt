package com.clerk.api.sso

import android.os.Bundle
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.clerk.api.Clerk
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.UUID

/**
 * GoogleCredentialManager is responsible for managing Google credentials. This interface defines
 * the contract for retrieving Google credentials. This is mostly a wrapper around the Android
 * Credential Manager API so it can be mocked in tests.
 */
internal interface GoogleCredentialManager {

  /**
   * Retrieves a Google credential for sign-in purposes.
   *
   * @param context The context used to access the credential manager.
   * @return A [GetCredentialResponse] containing the retrieved Google credential.
   */
  suspend fun getSignInWithGoogleCredential(): GetCredentialResponse

  /** Take a [GoogleIdTokenCredential] and extract the ID token from it. */
  fun getIdTokenFromCredential(credentialData: Bundle): String

  fun getGoogleIdOption(): GetGoogleIdOption
}

class GoogleCredentialManagerImpl : GoogleCredentialManager {
  override suspend fun getSignInWithGoogleCredential(): GetCredentialResponse {

    val oneTapClientId =
      requireNotNull(Clerk.environment.displayConfig.googleOneTapClientId) {
        "Google One Tap Client ID is not set. Please set it in your Clerk dashboard."
      }

    val googleIdOption: GetGoogleIdOption =
      GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(true)
        .setNonce(UUID.randomUUID().toString())
        .setServerClientId(oneTapClientId)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    val credentialManager = CredentialManager.create(Clerk.applicationContext!!.get()!!)

    return credentialManager.getCredential(
      request = request,
      context = Clerk.applicationContext!!.get()!!,
    )
  }

  override fun getIdTokenFromCredential(credentialData: Bundle): String {
    return GoogleIdTokenCredential.createFrom(credentialData).idToken
  }

  override fun getGoogleIdOption(): GetGoogleIdOption {
    val oneTapClientId = requireNotNull(Clerk.environment.displayConfig.googleOneTapClientId)

    return GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(false)
      .setAutoSelectEnabled(true)
      .setNonce(UUID.randomUUID().toString())
      .setServerClientId(oneTapClientId)
      .build()
  }
}
