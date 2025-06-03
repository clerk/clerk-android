package com.clerk.network.serialization

import kotlin.reflect.KClass
import toUnmodifiableMap

/**
 * ClerkApiService is an internal interface that defines the API endpoints for the Clerk
 * authentication service.
 *
 * This interface is not meant to be used directly by SDK consumers. Instead, all API operations
 * should be accessed through the [ClerkApi.instance] singleton, which provides a more user-friendly
 * API surface.
 *
 * The interface handles user authentication, session management, sign-up and sign-in flows, and
 * environment information using Retrofit annotations to define the HTTP methods and endpoints.
 *
 * All endpoints return a [ClerkApiResult], which is a sealed type that represents either a
 * successful response with the expected data ([ClerkApiResult.Success]) or a failure
 * ([ClerkApiResult.Failure]). This approach allows for non-exceptional, type-safe handling of API
 * responses.
 *
 * # Usage Example
 *
 * ```kotlin
 * // Example: Creating a sign-in
 * scope.launch {
 *     val email = "user@example.com"
 *     when (val result = ClerkApi.instance.signIn(email)) {
 *         is ClerkApiResult.Success -> {
 *             val signIn = result.value.response
 *             // Proceed with the sign-in flow based on available factors
 *             val firstFactor = signIn.supportedFirstFactors.firstOrNull()
 *             // Handle first factor preparation
 *         }
 *         is ClerkApiResult.Failure -> {
 *             // Handle sign-in failure
 *         }
 *     }
 * }
 * ```
 */
public sealed interface ClerkApiResult<out T : Any, out E : Any> {

  /** A successful result with the data available in [value]. */
  public class Success<out T : Any>
  public constructor(public val value: T, tags: Map<KClass<*>, Any>) : ClerkApiResult<T, Nothing> {
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
  ) : ClerkApiResult<Nothing, E> {
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
