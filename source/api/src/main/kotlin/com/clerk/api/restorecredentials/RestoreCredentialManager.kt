package com.clerk.api.restorecredentials

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse

/** Testable wrapper around the Android Credential Manager restore-credential operations. */
internal interface RestoreCredentialManager {

  suspend fun createCredential(
    context: Context,
    request: CreateRestoreCredentialRequest,
  ): CreateRestoreCredentialResponse

  suspend fun getCredential(context: Context, request: GetCredentialRequest): GetCredentialResponse

  suspend fun clearCredentialState(context: Context, request: ClearCredentialStateRequest)
}

internal class RestoreCredentialManagerImpl : RestoreCredentialManager {

  override suspend fun createCredential(
    context: Context,
    request: CreateRestoreCredentialRequest,
  ): CreateRestoreCredentialResponse {
    return CredentialManager.create(context).createCredential(context, request)
      as CreateRestoreCredentialResponse
  }

  override suspend fun getCredential(
    context: Context,
    request: GetCredentialRequest,
  ): GetCredentialResponse {
    return CredentialManager.create(context).getCredential(context, request)
  }

  override suspend fun clearCredentialState(
    context: Context,
    request: ClearCredentialStateRequest,
  ) {
    CredentialManager.create(context).clearCredentialState(request)
  }
}
