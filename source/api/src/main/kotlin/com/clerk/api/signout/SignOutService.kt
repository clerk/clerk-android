package com.clerk.api.signout

import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLogger
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.restorecredentials.RestoreCredentials
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal object SignOutService {

  suspend fun signOut(): ClerkResult<Unit, ClerkErrorResponse> {
    var serverError: Exception? = null

    try {
      when (val result = ClerkApi.session.deleteSessions()) {
        is ClerkResult.Success -> Clerk.updateClient(result.value)
        is ClerkResult.Failure ->
          serverError = result.throwable as? Exception ?: Exception(result.errorMessage)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      ClerkLogger.w("Server sign-out failed: ${e.message}")
      serverError = e
    } finally {
      withContext(NonCancellable) {
        RestoreCredentials.clearSilently()
        StorageHelper.deleteValue(StorageKey.DEVICE_TOKEN)
        Clerk.updateClient(Client())
      }
    }

    runCatching {
      val clientResult = Clerk.fetchAndApplyClient { Client.getSkippingClientId() }
      if (clientResult is ClerkResult.Failure) {
        ClerkLogger.w("Client refresh after sign-out failed: ${clientResult.errorMessage}")
      }
    }
      .onFailure {
        if (it is CancellationException) throw it
        ClerkLogger.w("Client refresh after sign-out failed: ${it.message}")
      }

    return if (serverError != null) {
      ClerkResult.unknownFailure(Exception(serverError.message ?: "Unknown error"))
    } else {
      ClerkResult.success(Unit)
    }
  }
}
