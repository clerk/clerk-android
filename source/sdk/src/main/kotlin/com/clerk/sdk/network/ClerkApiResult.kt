package com.clerk.sdk.network

sealed class ClerkApiResult<out T, out E> {
  data class Success<T>(val data: T) : ClerkApiResult<T, Nothing>()

  data class Error<E>(val error: E) : ClerkApiResult<Nothing, E>()

  suspend fun <R> fold(onSuccess: suspend (T) -> R, onError: (E) -> R): R =
    when (this) {
      is Success -> onSuccess(data)
      is Error -> onError(error)
    }
}
