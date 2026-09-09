package com.clerk.ui.core.common

import kotlinx.coroutines.CancellationException

/** UI error handling must not turn a cancelled screen operation into a visible failure. */
internal suspend inline fun <T> runUiOperation(block: () -> T): Result<T> =
  try {
    Result.success(block())
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (error: Exception) {
    Result.failure(error)
  }

internal val Throwable.displayMessage: String
  get() = localizedMessage ?: "The operation could not be completed."
