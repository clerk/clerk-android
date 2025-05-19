@file:Suppress("TooManyFunctions")

package com.clerk.sdk.network.serialization

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** If [ClerkApiResult.Success], returns the underlying [T] value. Otherwise, returns null. */
public fun <T : Any, E : Any> ClerkApiResult<T, E>.successOrNull(): T? =
  when (this) {
    is ClerkApiResult.Success -> value
    else -> null
  }

/**
 * If [ClerkApiResult.Success], returns the underlying [T] value. Otherwise, returns the result of
 * the [defaultValue] function.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkApiResult<T, E>.successOrElse(
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
public inline fun <T : Any, E : Any> ClerkApiResult<T, E>.successOrNothing(
  body: (failure: ClerkApiResult.Failure<E>) -> Nothing
): T {
  contract { callsInPlace(body, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is ClerkApiResult.Success -> value
    is ClerkApiResult.Failure -> body(this)
  }
}

/**
 * Returns the encapsulated [Throwable] exception if this failure type if one is available or null
 * if none are available.
 *
 * Note that if this is [ClerkApiResult.Failure.HttpFailure] or [ClerkApiResult.Failure.ApiFailure],
 * the `error` property will be returned IFF it's a [Throwable].
 */
public fun <E : Any> ClerkApiResult.Failure<E>.exceptionOrNull(): Throwable? {
  return when (this) {
    is ClerkApiResult.Failure.UnknownFailure -> error
    is ClerkApiResult.Failure.HttpFailure -> error as? Throwable?
    is ClerkApiResult.Failure.ClerkApiFailure -> error as? Throwable?
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
public inline fun <T : Any, E : Any, C> ClerkApiResult<T, E>.fold(
  noinline onSuccess: (value: T) -> C,
  noinline onFailure: (failure: ClerkApiResult.Failure<E>) -> C,
): C {
  contract {
    callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
  }
  @Suppress("UNCHECKED_CAST")
  return fold(
    onSuccess,
    onFailure as (ClerkApiResult.Failure.UnknownFailure) -> C,
    onFailure,
    onFailure,
  )
}

/** Transforms an [ClerkApiResult] into a [C] value. */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any, C> ClerkApiResult<T, E>.fold(
  onSuccess: (value: T) -> C,
  onUnknownFailure: (failure: ClerkApiResult.Failure.UnknownFailure) -> C,
  onHttpFailure: (failure: ClerkApiResult.Failure.HttpFailure<E>) -> C,
  onApiFailure: (failure: ClerkApiResult.Failure.ClerkApiFailure<E>) -> C,
): C {
  contract {
    callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onUnknownFailure, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onHttpFailure, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onApiFailure, InvocationKind.AT_MOST_ONCE)
  }
  return when (this) {
    is ClerkApiResult.Success -> onSuccess(value)
    is ClerkApiResult.Failure.ClerkApiFailure -> onApiFailure(this)
    is ClerkApiResult.Failure.HttpFailure -> onHttpFailure(this)
    is ClerkApiResult.Failure.UnknownFailure -> onUnknownFailure(this)
  }
}

/**
 * Performs the given [action] on the encapsulated [ClerkApiResult.Failure] if this instance
 * represents [failure][ClerkApiResult.Failure]. Returns the original
 * com.clerk.clerkserializer.ClerkApiResult` unchanged.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkApiResult<T, E>.onFailure(
  action: (failure: ClerkApiResult.Failure<E>) -> Unit
): ClerkApiResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkApiResult.Failure) action(this)
  return this
}

/**
 * Performs the given [action] on the encapsulated [ClerkApiResult.Failure.HttpFailure] if this
 * instance represents [failure][ClerkApiResult.Failure.HttpFailure]. Returns the original
 * com.clerk.clerkserializer.ClerkApiResult` unchanged.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkApiResult<T, E>.onHttpFailure(
  action: (failure: ClerkApiResult.Failure.HttpFailure<E>) -> Unit
): ClerkApiResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkApiResult.Failure.HttpFailure) action(this)
  return this
}

/**
 * Performs the given [action] on the encapsulated [ClerkApiResult.Failure.ApiFailure] if this
 * instance represents [failure][ClerkApiResult.Failure.ApiFailure]. Returns the original
 * com.clerk.clerkserializer.ClerkApiResult` unchanged.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkApiResult<T, E>.onApiFailure(
  action: (failure: ClerkApiResult.Failure.ClerkApiFailure<E>) -> Unit
): ClerkApiResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkApiResult.Failure.ClerkApiFailure) action(this)
  return this
}

/**
 * Performs the given [action] on the encapsulated [ClerkApiResult.Failure.UnknownFailure] if this
 * instance represents [failure][ClerkApiResult.Failure.UnknownFailure]. Returns the original
 * com.clerk.clerkserializer.ClerkApiResult` unchanged.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkApiResult<T, E>.onUnknownFailure(
  action: (failure: ClerkApiResult.Failure.UnknownFailure) -> Unit
): ClerkApiResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkApiResult.Failure.UnknownFailure) action(this)
  return this
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents
 * [success][ClerkApiResult.Success]. Returns the original com.clerk.clerkserializer.ClerkApiResult`
 * unchanged.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any, E : Any> ClerkApiResult<T, E>.onSuccess(
  action: (value: T) -> Unit
): ClerkApiResult<T, E> {
  contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
  if (this is ClerkApiResult.Success) action(value)
  return this
}
