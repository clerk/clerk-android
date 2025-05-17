package com.clerk.sdk.network

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
@Suppress(
  // Inline to allow contextual actions
  "NOTHING_TO_INLINE",
  // https://youtrack.jetbrains.com/issue/KT-71690
  "WRONG_INVOCATION_KIND",
)
public suspend inline fun <T : Any, E : Any, C> ApiResult<T, E>.fold(
  noinline onSuccess: (value: T) -> C,
  noinline onFailure: (failure: ApiResult.Failure<E>) -> C,
): C {
  contract {
    callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
  }
  @Suppress("UNCHECKED_CAST")
  return fold(
    onSuccess,
    onFailure as (ApiResult.Failure.NetworkFailure) -> C,
    onFailure as (ApiResult.Failure.UnknownFailure) -> C,
    onFailure,
    onFailure,
  )
}
