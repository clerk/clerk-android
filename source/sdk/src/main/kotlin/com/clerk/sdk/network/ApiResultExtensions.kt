package com.clerk.sdk.network

import com.slack.eithernet.ApiResult
import com.slack.eithernet.fold
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

@OptIn(ExperimentalContracts::class)
private suspend inline fun <T : Any, E : Any, C> ApiResult<T, E>.fold(
  onSuccess: suspend (value: T) -> C,
  onNetworkFailure: (failure: ApiResult.Failure.NetworkFailure) -> C,
  onUnknownFailure: (failure: ApiResult.Failure.UnknownFailure) -> C,
  onHttpFailure: (failure: ApiResult.Failure.HttpFailure<E>) -> C,
  onApiFailure: (failure: ApiResult.Failure.ApiFailure<E>) -> C,
): C {
  contract {
    callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onNetworkFailure, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onUnknownFailure, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onHttpFailure, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onApiFailure, InvocationKind.AT_MOST_ONCE)
  }
  return when (this) {
    is ApiResult.Success -> onSuccess(value)
    is ApiResult.Failure.ApiFailure -> onApiFailure(this)
    is ApiResult.Failure.HttpFailure -> onHttpFailure(this)
    is ApiResult.Failure.NetworkFailure -> onNetworkFailure(this)
    is ApiResult.Failure.UnknownFailure -> onUnknownFailure(this)
  }
}
