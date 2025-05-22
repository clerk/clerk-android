@file:Suppress("TooGenericExceptionCaught")

package com.clerk.sdk.service

import com.clerk.sdk.Clerk
import com.clerk.sdk.model.session.delete
import com.clerk.sdk.network.ClerkApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * SignOutService is responsible for signing out the user by removing the session from the Clerk
 * API. It uses a coroutine to perform the network operation on a background thread.
 */
/**
 * SignOutService is responsible for signing out the user by removing the session from the Clerk
 * API. It uses a coroutine to perform the network operation on a background thread and returns a
 * Flow of SignOutState to provide updates on the operation status.
 */
object SignOutService {

  /** Represents the various states during the sign-out process. */
  sealed class SignOutState {
    object Loading : SignOutState()

    object Success : SignOutState()

    data class Error(val exception: Throwable) : SignOutState()
  }

  /**
   * Signs out the user by removing the session from the Clerk API.
   *
   * @return Flow emitting the current state of the sign-out operation.
   */
  fun signOut(): Flow<SignOutState> =
    flow {
        emit(SignOutState.Loading)

        try {
          if (Clerk.session?.id != null) {
            Clerk.session?.id?.let { sessionId -> ClerkApi.instance.removeSession(sessionId) }
          } else {
            Clerk.session?.delete()
          }
          emit(SignOutState.Success)
        } catch (e: Exception) {
          emit(SignOutState.Error(e))
        }
      }
      .flowOn(Dispatchers.IO)
}
