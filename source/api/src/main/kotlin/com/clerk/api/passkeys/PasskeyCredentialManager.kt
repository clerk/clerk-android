package com.clerk.api.passkeys

import android.content.Context
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse

internal interface PasskeyCredentialManager {
  suspend fun createCredential(
    context: Context,
    request: CreatePublicKeyCredentialRequest,
  ): CreateCredentialResponse

  suspend fun getCredential(context: Context, request: GetCredentialRequest): GetCredentialResponse
}

internal class PasskeyCredentialManagerImpl : PasskeyCredentialManager {

  override suspend fun createCredential(
    context: Context,
    request: CreatePublicKeyCredentialRequest,
  ): CreateCredentialResponse {
    val credentialManager = CredentialManager.create(context)
    return credentialManager.createCredential(context, request)
  }

  override suspend fun getCredential(
    context: Context,
    request: GetCredentialRequest,
  ): GetCredentialResponse {
    val credentialManager = CredentialManager.create(context)
    return credentialManager.getCredential(context, request)
  }
}
