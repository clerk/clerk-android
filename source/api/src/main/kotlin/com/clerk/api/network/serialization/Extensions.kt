@file:Suppress("TooManyFunctions")

package com.clerk.api.network.serialization

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** If [ClerkResult.Success], returns the underlying [T] value. Otherwise, returns null. */
public fun <T : Any, E : Any> ClerkResult<T, E>.successOrNull(): T? =
  when (this) {
    is ClerkResult.Success -> value
    else -> null
  }

/**
 * If [ClerkResult.Success], returns the underlying [T] value. Otherwise, returns the result of the
 * [defaultValue] function.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkResult<T, E>.successOrElse(
  defaultValue: (failure: ClerkResult.Failure<E>) -> T
): T {
  contract { callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkResult.Success -> value
    is ClerkResult.Failure -> defaultValue(this)
  }
}

/**
 * If [ClerkResult.Success], returns the underlying [T] value. Otherwise, calls [body] with the
 * failure, which can either throw an exception or return early (since this function is inline).
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkResult<T, E>.successOrNothing(
  body: (failure: ClerkResult.Failure<E>) -> Nothing
): T {
  contract { callsInPlace(body, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkResult.Success -> value
    is ClerkResult.Failure -> body(this)
  }
}

/** Returns the encapsulated [Throwable] exception if this is a failure. */
public fun <E : Any> ClerkResult.Failure<E>.exceptionOrNull(): Throwable? {
  return throwable
}

/** Transforms an [ClerkResult] into a [C] value. */
@OptIn(ExperimentalContracts::class)
@Suppress(
  // Inline to allow contextual actions
  "NOTHING_TO_INLINE",
  // https://youtrack.jetbrains.com/issue/KT-71690
  "WRONG_INVOCATION_KIND",
)
public suspend inline fun <T : Any, E : Any, C> ClerkResult<T, E>.suspendingFold(
  noinline onSuccess: suspend (value: T) -> C,
  noinline onFailure: (failure: ClerkResult.Failure<E>) -> C,
): C {
  contract {
    callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
  }
  return when (this) {
    is ClerkResult.Success -> onSuccess(value)
    is ClerkResult.Failure -> onFailure(this)
  }
}

/** Transforms an [ClerkResult] into a [C] value. */
@OptIn(ExperimentalContracts::class)
@Suppress(
  // Inline to allow contextual actions
  "NOTHING_TO_INLINE",
  // https://youtrack.jetbrains.com/issue/KT-71690
  "WRONG_INVOCATION_KIND",
)
public inline fun <T : Any, E : Any, C> ClerkResult<T, E>.fold(
  onSuccess: (value: T) -> C,
  onFailure: (failure: ClerkResult.Failure<E>) -> C,
): C {
  contract {
    callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
  }
  return when (this) {
    is ClerkResult.Success -> onSuccess(value)
    is ClerkResult.Failure -> onFailure(this)
  }
}

/**
 * Returns a [ClerkResult.Success] holding [transform] applied to the value of a
 * [ClerkResult.Success], keeping its tags, or returns the original [ClerkResult.Failure] unchanged.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, R : Any, E : Any> ClerkResult<T, E>.map(
  transform: (value: T) -> R
): ClerkResult<R, E> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkResult.Success -> ClerkResult.Success(transform(value), tags)
    is ClerkResult.Failure -> this
  }
}

/**
 * Returns a [ClerkResult.Failure] whose error is [transform] applied to the original error, keeping
 * the throwable, status code, error type and tags, or returns the original [ClerkResult.Success]
 * unchanged. [transform] is not called when the failure carries no error.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any, F : Any> ClerkResult<T, E>.mapError(
  transform: (error: E) -> F
): ClerkResult<T, F> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkResult.Success -> this
    is ClerkResult.Failure ->
      ClerkResult.Failure(
        error = error?.let(transform),
        throwable = throwable,
        code = code,
        errorType = errorType,
        tags = tags,
      )
  }
}

/**
 * Returns a new [ClerkResult] by applying [transform] to the value of a [ClerkResult.Success], or
 * returns the original [ClerkResult.Failure] if this is a failure.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, R : Any, E : Any> ClerkResult<T, E>.flatMap(
  transform: (value: T) -> ClerkResult<R, E>
): ClerkResult<R, E> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkResult.Success -> transform(value)
    is ClerkResult.Failure -> this as ClerkResult<R, E>
  }
}

/**
 * Returns a new [ClerkResult] by applying [transform] to the value of a [ClerkResult.Success], or
 * returns the original [ClerkResult.Failure] if this is a failure.
 */
@OptIn(ExperimentalContracts::class)
public suspend inline fun <T : Any, R : Any, E : Any> ClerkResult<T, E>.suspendingFlatMap(
  transform: suspend (value: T) -> ClerkResult<R, E>
): ClerkResult<R, E> {
  contract { callsInPlace(transform, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkResult.Success -> transform(value)
    is ClerkResult.Failure -> this as ClerkResult<R, E>
  }
}

/**
 * Performs the given [action] on the encapsulated [ClerkResult.Failure] if this instance represents
 * [failure][ClerkResult.Failure]. Returns the original `ClerkApiResult` unchanged.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkResult<T, E>.onFailure(
  action: (failure: ClerkResult.Failure<E>) -> Unit
): ClerkResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkResult.Failure) action(this)
  return this
}

/**
 * Performs the given [action] on the encapsulated failure if this instance represents a failure
 * with the specified error type. Returns the original `ClerkApiResult` unchanged.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkResult<T, E>.onFailureType(
  errorType: ClerkResult.Failure.ErrorType,
  action: (failure: ClerkResult.Failure<E>) -> Unit,
): ClerkResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkResult.Failure && this.errorType == errorType) action(this)
  return this
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents
 * [success][ClerkResult.Success]. Returns the original `ClerkApiResult` unchanged.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkResult<T, E>.onSuccess(
  action: (value: T) -> Unit
): ClerkResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkResult.Success) action(value)
  return this
}
