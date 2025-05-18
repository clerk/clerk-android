package com.clerk.clerkserializer

import com.clerk.clerkserializer.ClerkApiResult.Failure.ClerkApiFailure
import com.clerk.clerkserializer.ClerkApiResult.Failure.HttpFailure
import com.clerk.clerkserializer.ClerkApiResult.Failure.UnknownFailure
import kotlin.reflect.KClass
import toUnmodifiableMap

/**
 * Represents a result from a traditional HTTP API. [ClerkApiResult] has two sealed subtypes:
 * [Success] and [Failure]. [Success] is typed to [T] with no error type and [Failure] is typed to
 * [E] with no success type.
 *
 * [Failure] in turn is represented by four sealed subtypes of its own: [Failure.NetworkFailure],
 * [Failure.ClerkApiFailure], [Failure.HttpFailure], and [Failure.UnknownFailure]. This allows for
 * simple handling of results through a consistent, non-exceptional flow via sealed `when` branches.
 *
 * ```
 * when (val result = myApi.someEndpoint()) {
 *   is Success -> doSomethingWith(result.response)
 *   is Failure -> when (result) {
 *     is NetworkFailure -> showError(result.error)
 *     is HttpFailure -> showError(result.code)
 *     is ApiFailure -> showError(result.error)
 *     is UnknownError -> showError(result.error)
 *   }
 * }
 * ```
 *
 * Usually, user code for this could just simply show a generic error message for a [Failure] case,
 * but a sealed class is exposed for more specific error messaging.
 */
public sealed interface ClerkApiResult<out T : Any, out E : Any> {

  /** A successful result with the data available in [value]. */
  public class Success<out T : Any>
  public constructor(public val value: T, tags: Map<KClass<*>, Any>) : ClerkApiResult<T, Nothing> {

    /** Extra metadata associated with the result such as original requests, responses, etc. */
    public val tags: Map<KClass<*>, Any> = tags.toUnmodifiableMap()

    /** Returns a new copy of this with the given [tags]. */
    public fun withTags(tags: Map<KClass<*>, Any>): Success<T> {
      return Success(value, tags)
    }
  }

  /** Represents a failure of some sort. */
  public sealed interface Failure<out E : Any> : ClerkApiResult<Nothing, E> {

    /**
     * An unknown failure caused by a given [error]. This error is opaque, as the actual type could
     * be from a number of sources (serialization issues, etc). This event is generally considered
     * to be a non-recoverable and should be used as signal or logging before attempting to
     * gracefully degrade or retry.
     */
    public class UnknownFailure
    public constructor(public val error: Throwable, tags: Map<KClass<*>, Any>) : Failure<Nothing> {

      /** Extra metadata associated with the result such as original requests, responses, etc. */
      internal val tags: Map<KClass<*>, Any> = tags.toUnmodifiableMap()

      /** Returns a new copy of this with the given [tags]. */
      public fun withTags(tags: Map<KClass<*>, Any>): UnknownFailure {
        return UnknownFailure(error, tags.toUnmodifiableMap())
      }
    }

    /**
     * An HTTP failure. This indicates a 4xx or 5xx response. The [code] is available for reference.
     *
     * @property code The HTTP status code.
     * @property error An optional [error][E]. This would be from the error body of the response.
     */
    public class HttpFailure<E : Any>
    public constructor(public val code: Int, public val error: E?, tags: Map<KClass<*>, Any>) :
      Failure<E> {

      /** Extra metadata associated with the result such as original requests, responses, etc. */
      internal val tags: Map<KClass<*>, Any> = tags.toUnmodifiableMap()

      /** Returns a new copy of this with the given [tags]. */
      public fun withTags(tags: Map<KClass<*>, Any>): HttpFailure<E> {
        return HttpFailure(code, error, tags.toUnmodifiableMap())
      }
    }

    /**
     * An API failure. This indicates a 2xx response where [ApiException] was thrown during response
     * body conversion.
     *
     * An [ApiException], the [error] property will be best-effort populated with the value of the
     * [ApiException.error] property.
     *
     * @property error An optional [error][E].
     */
    public class ClerkApiFailure<E : Any>
    public constructor(public val error: E?, tags: Map<KClass<*>, Any>) : Failure<E> {

      /** Extra metadata associated with the result such as original requests, responses, etc. */
      internal val tags: Map<KClass<*>, Any> = tags.toUnmodifiableMap()

      /** Returns a new copy of this with the given [tags]. */
      public fun withTags(tags: Map<KClass<*>, Any>): ClerkApiFailure<E> {
        return ClerkApiFailure(error, tags.toUnmodifiableMap())
      }
    }
  }

  public companion object {
    private const val OK = 200
    private val HTTP_SUCCESS_RANGE = OK..299
    private val HTTP_FAILURE_RANGE = 400..599

    /** Returns a new [Success] with given [value]. */
    public fun <T : Any> success(value: T): Success<T> = Success(value, emptyMap())

    /** Returns a new [HttpFailure] with given [code] and optional [error]. */
    public fun <E : Any> httpFailure(code: Int): Failure.HttpFailure<E> {
      return httpFailure(code, null)
    }

    /** Returns a new [HttpFailure] with given [code] and optional [error]. */
    public fun <E : Any> httpFailure(code: Int, error: E? = null): Failure.HttpFailure<E> {
      checkHttpFailureCode(code)
      return Failure.HttpFailure(code, error, emptyMap())
    }

    /** Returns a new [ClerkApiFailure] with given [error]. */
    public fun <E : Any> apiFailure(): Failure.ClerkApiFailure<E> = apiFailure(null)

    /** Returns a new [ClerkApiFailure] with given [error]. */
    public fun <E : Any> apiFailure(error: E? = null): Failure.ClerkApiFailure<E> =
      Failure.ClerkApiFailure(error, emptyMap())

    /** Returns a new [UnknownFailure] with given [error]. */
    public fun unknownFailure(error: Throwable): Failure.UnknownFailure =
      Failure.UnknownFailure(error, emptyMap())

    internal fun checkHttpFailureCode(code: Int) {
      require(code !in HTTP_SUCCESS_RANGE) {
        "Status code '$code' is a successful HTTP response. If you mean to use a $OK code + error " +
          "string to indicate an API error, use the ApiResult.apiFailure() factory."
      }
      require(code in HTTP_FAILURE_RANGE) {
        "Status code '$code' is not a HTTP failure response. Must be a 4xx or 5xx code."
      }
    }
  }
}
