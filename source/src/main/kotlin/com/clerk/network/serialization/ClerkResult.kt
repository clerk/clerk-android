package com.clerk.network.serialization

import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.error.firstMessage
import kotlin.reflect.KClass
import toUnmodifiableMap

/**
 * ClerkResult is a sealed interface used throughout the Clerk SDK to represent the result of an API
 * call. It provides a type-safe and non-exceptional way to handle either a successful response with
 * data or a failure with error details.
 *
 * All consumer facing functions return [ClerkResult] values. This interface allows for pattern
 * matching with [Success] and [Failure] cases using Kotlin's `when` expression.
 *
 * [Success] contains the successfully retrieved data, while [Failure] wraps error information such
 * as HTTP status codes, API errors, or unexpected failures. This design ensures predictable error
 * handling and avoids checked exceptions.
 *
 * # Usage Example
 *
 * ```kotlin
 * // Example: Creating a sign-in
 * scope.launch {
 *     val email = "user@example.com"
 *     when (val result = ClerkApi.instance.signIn(email)) {
 *         is ClerkResult.Success -> {
 *             val signIn = result.value.response
 *             // Proceed with the sign-in flow based on available factors
 *             val firstFactor = signIn.supportedFirstFactors.firstOrNull()
 *             // Handle first factor preparation
 *         }
 *         is ClerkResult.Failure -> {
 *             // Handle sign-in failure using error, code, or throwable
 *             val errorMessage = result.error?.firstMessage() ?: "Unknown error"
 *         }
 *     }
 * }
 * ```
 */
public sealed interface ClerkResult<out T : Any, out E : Any> {

  /** A successful result with the data available in [value]. */
  public class Success<out T : Any>
  public constructor(public val value: T, tags: Map<KClass<*>, Any>) : ClerkResult<T, Nothing> {
    public val tags: Map<KClass<*>, Any> = tags.toUnmodifiableMap()

    public fun withTags(tags: Map<KClass<*>, Any>): Success<T> {
      return Success(value, tags)
    }
  }

  /** A unified failure type that contains all necessary error information. */
  public class Failure<out E : Any>
  public constructor(
    public val error: E?,
    public val throwable: Throwable? = null,
    public val code: Int? = null,
    public val errorType: ErrorType = ErrorType.UNKNOWN,
    tags: Map<KClass<*>, Any> = emptyMap(),
  ) : ClerkResult<Nothing, E> {
    public val tags: Map<KClass<*>, Any> = tags.toUnmodifiableMap()

    public fun withTags(tags: Map<KClass<*>, Any>): Failure<E> {
      return Failure(error, throwable, code, errorType, tags)
    }

    public enum class ErrorType {
      API, // For ClerkAPI failures
      HTTP, // For HTTP failures
      UNKNOWN, // For unknown/network failures
    }
  }

  public companion object {
    private const val OK = 200
    private val HTTP_SUCCESS_RANGE = OK..299
    private val HTTP_FAILURE_RANGE = 400..599

    /** Returns a new [Success] with given [value]. */
    public fun <T : Any> success(value: T): Success<T> = Success(value, emptyMap())

    /** Returns a new [Failure] with HTTP error details. */
    public fun <E : Any> httpFailure(code: Int, error: E? = null): Failure<E> {
      checkHttpFailureCode(code)
      return Failure(error, null, code, Failure.ErrorType.HTTP)
    }

    /** Returns a new [Failure] with API error details. */
    public fun <E : Any> apiFailure(error: E? = null): Failure<E> =
      Failure(error, null, null, Failure.ErrorType.API)

    /** Returns a new [Failure] with unknown error details. */
    public fun unknownFailure(throwable: Throwable): Failure<Nothing> =
      Failure(null, throwable, null, Failure.ErrorType.UNKNOWN)

    internal fun checkHttpFailureCode(code: Int) {
      require(code !in HTTP_SUCCESS_RANGE) { "Status code '$code' is a successful HTTP response." }
      require(code in HTTP_FAILURE_RANGE) {
        "Status code '$code' is not a HTTP failure response. Must be a 4xx or 5xx code."
      }
    }
  }
}

/**
 * Convenience function to extract the first error message from a [ClerkResult.Failure] containing a
 * [ClerkErrorResponse]. Returns `null` if the error is not a [ClerkErrorResponse] or if there are
 * no error messages.
 */
fun ClerkResult.Failure<ClerkErrorResponse>.firstErrorMessageOrNull() = this.error?.firstMessage()
