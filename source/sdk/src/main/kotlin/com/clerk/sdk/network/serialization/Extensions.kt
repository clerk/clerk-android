@file:Suppress("TooManyFunctions")

package com.clerk.sdk.network.serialization

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** If [ClerkApiResult.Success], returns the underlying [T] value. Otherwise, returns null. */
fun <T : Any, E : Any> ClerkApiResult<T, E>.successOrNull(): T? =
  when (this) {
    is ClerkApiResult.Success -> value
    else -> null
  }

/**
 * If [ClerkApiResult.Success], returns the underlying [T] value. Otherwise, returns the result of
 * the [defaultValue] function.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Any, E : Any> ClerkApiResult<T, E>.successOrElse(
  defaultValue: (failure: ClerkApiResult.Failure<E>) -> T
): T {
  contract { callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkApiResult.Success -> value
    is ClerkApiResult.Failure -> defaultValue(this)
  }
}

/**
 * If [ClerkApiResult.Success], returns the underlying [T] value. Otherwise, calls [body] with the
 * failure, which can either throw an exception or return early (since this function is inline).
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Any, E : Any> ClerkApiResult<T, E>.successOrNothing(
  body: (failure: ClerkApiResult.Failure<E>) -> Nothing
): T {
  contract { callsInPlace(body, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkApiResult.Success -> value
    is ClerkApiResult.Failure -> body(this)
  }
}

/** Returns the encapsulated [Throwable] exception if this is a failure. */
fun <E : Any> ClerkApiResult.Failure<E>.exceptionOrNull(): Throwable? {
  return throwable
}

/** Transforms an [ClerkApiResult] into a [C] value. */
@OptIn(ExperimentalContracts::class)
@Suppress(
  // Inline to allow contextual actions
  "NOTHING_TO_INLINE",
  // https://youtrack.jetbrains.com/issue/KT-71690
  "WRONG_INVOCATION_KIND",
)
suspend inline fun <T : Any, E : Any, C> ClerkApiResult<T, E>.suspendingFold(
  noinline onSuccess: suspend (value: T) -> C,
  noinline onFailure: (failure: ClerkApiResult.Failure<E>) -> C,
): C {
  contract {
    callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
  }
  return when (this) {
    is ClerkApiResult.Success -> onSuccess(value)
    is ClerkApiResult.Failure -> onFailure(this)
  }
}

/** Transforms an [ClerkApiResult] into a [C] value. */
@OptIn(ExperimentalContracts::class)
@Suppress(
  // Inline to allow contextual actions
  "NOTHING_TO_INLINE",
  // https://youtrack.jetbrains.com/issue/KT-71690
  "WRONG_INVOCATION_KIND",
)
inline fun <T : Any, E : Any, C> ClerkApiResult<T, E>.fold(
  onSuccess: (value: T) -> C,
  onFailure: (failure: ClerkApiResult.Failure<E>) -> C,
): C {
  contract {
    callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
  }
  return when (this) {
    is ClerkApiResult.Success -> onSuccess(value)
    is ClerkApiResult.Failure -> onFailure(this)
  }
}

/**
 * Returns a new [ClerkApiResult] by applying [transform] to the value of a
 * [ClerkApiResult.Success], or returns the original [ClerkApiResult.Failure] if this is a failure.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R : Any, E : Any> ClerkApiResult<T, E>.flatMap(
  transform: (value: T) -> ClerkApiResult<R, E>
): ClerkApiResult<R, E> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkApiResult.Success -> transform(value)
    is ClerkApiResult.Failure -> this as ClerkApiResult<R, E>
  }
}

/**
 * Returns a new [ClerkApiResult] by applying [transform] to the value of a
 * [ClerkApiResult.Success], or returns the original [ClerkApiResult.Failure] if this is a failure.
 */
@OptIn(ExperimentalContracts::class)
suspend inline fun <T : Any, R : Any, E : Any> ClerkApiResult<T, E>.suspendingFlatMap(
  transform: suspend (value: T) -> ClerkApiResult<R, E>
): ClerkApiResult<R, E> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkApiResult.Success -> transform(value)
    is ClerkApiResult.Failure -> this as ClerkApiResult<R, E>
  }
}

/**
 * Performs the given [action] on the encapsulated [ClerkApiResult.Failure] if this instance
 * represents [failure][ClerkApiResult.Failure]. Returns the original `ClerkApiResult` unchanged.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Any, E : Any> ClerkApiResult<T, E>.onFailure(
  action: (failure: ClerkApiResult.Failure<E>) -> Unit
): ClerkApiResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkApiResult.Failure) action(this)
  return this
}

/**
 * Performs the given [action] on the encapsulated failure if this instance represents a failure
 * with the specified error type. Returns the original `ClerkApiResult` unchanged.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Any, E : Any> ClerkApiResult<T, E>.onFailureType(
  errorType: ClerkApiResult.Failure.ErrorType,
  action: (failure: ClerkApiResult.Failure<E>) -> Unit,
): ClerkApiResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkApiResult.Failure && this.errorType == errorType) action(this)
  return this
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents
 * [success][ClerkApiResult.Success]. Returns the original `ClerkApiResult` unchanged.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Any, E : Any> ClerkApiResult<T, E>.onSuccess(
  action: (value: T) -> Unit
): ClerkApiResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkApiResult.Success) action(value)
  return this
}
