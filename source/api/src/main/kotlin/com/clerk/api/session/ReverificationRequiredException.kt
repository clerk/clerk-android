package com.clerk.api.session

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import kotlin.math.floor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

private const val SESSION_REVERIFICATION_REQUIRED = "session_reverification_required"

/**
 * Signals that an action needs the user to verify their identity again before it can run.
 *
 * Report it from your own networking code when your backend rejects a request because the session's
 * verification is too old: return it in a failure with [ClerkResult.unknownFailure], or throw it.
 * Clerk's backend SDKs answer such a request with a 403 response whose body [fromResponseBody]
 * reads.
 *
 * ```kotlin
 * if (statusCode == 403) {
 *   ReverificationRequiredException.fromResponseBody(responseBody)?.let {
 *     return ClerkResult.unknownFailure(it)
 *   }
 * }
 * ```
 *
 * Call [reverificationRequiredOrNull] on a [ClerkResult.Failure] to tell a reverification-required
 * failure apart from other failures, whether it came from your backend or from Clerk's API.
 *
 * @property reverification How recently the rejected request needs the user to have verified, or
 *   `null` when the request didn't say, as when Clerk's API answers with
 *   `session_reverification_required`.
 */
public class ReverificationRequiredException(
  public val reverification: ReverificationConfig? = null
) : Exception("The session needs to be reverified before this action can run.") {

  public companion object {
    /**
     * Reads the reverification error body that Clerk's backend SDKs send with a 403 response.
     *
     * The body looks like `{"clerk_error": {"type": "forbidden", "reason": "reverification-error",
     * "metadata": {"reverification": "strict"}}}`, where `reverification` is `strict_mfa`,
     * `strict`, `moderate`, `lax`, or an object with `level` and `afterMinutes`. A `reverification`
     * value that isn't one of those, such as an unknown level or a non-positive `afterMinutes`,
     * still returns an exception, with a `null` [reverification].
     *
     * @return The exception the body describes, or `null` for any other body.
     */
    public fun fromResponseBody(body: String): ReverificationRequiredException? =
      (parseJsonObject(body)?.get("clerk_error") as? JsonObject)
        ?.takeIf {
          it.stringOrNull("type") == "forbidden" &&
            it.stringOrNull("reason") == "reverification-error"
        }
        ?.let { clerkError ->
          val requested = (clerkError["metadata"] as? JsonObject)?.get("reverification")
          ReverificationRequiredException(requested?.let(::reverificationConfig))
        }
  }
}

/**
 * Returns the reverification this failure asks for, or `null` when it doesn't ask for one.
 *
 * Recognizes a [ReverificationRequiredException] in [ClerkResult.Failure.throwable] and a
 * [ClerkErrorResponse] with the code `session_reverification_required`, which has no
 * [ReverificationRequiredException.reverification].
 */
public fun ClerkResult.Failure<*>.reverificationRequiredOrNull(): ReverificationRequiredException? {
  (throwable as? ReverificationRequiredException)?.let {
    return it
  }
  val errors = (error as? ClerkErrorResponse)?.errors.orEmpty()
  return if (errors.any { it.code == SESSION_REVERIFICATION_REQUIRED }) {
    ReverificationRequiredException()
  } else {
    null
  }
}

private fun parseJsonObject(body: String): JsonObject? =
  try {
    Json.parseToJsonElement(body) as? JsonObject
  } catch (_: SerializationException) {
    null
  }

private fun JsonObject.stringOrNull(key: String): String? =
  (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun reverificationConfig(element: JsonElement): ReverificationConfig? =
  when (element) {
    is JsonPrimitive -> element.takeIf { it.isString }?.content?.let(::namedReverificationConfig)
    is JsonObject -> customReverificationConfig(element)
    else -> null
  }

private fun namedReverificationConfig(name: String): ReverificationConfig? =
  when (name) {
    "strict_mfa" -> ReverificationConfig.StrictMfa
    "strict" -> ReverificationConfig.Strict
    "moderate" -> ReverificationConfig.Moderate
    "lax" -> ReverificationConfig.Lax
    else -> null
  }

private fun customReverificationConfig(custom: JsonObject): ReverificationConfig? {
  val level =
    custom.stringOrNull("level")?.let { name ->
      SessionVerification.Level.entries.firstOrNull { it.value == name }
        ?: SessionVerification.Level.UNKNOWN
    }
  val afterMinutes = custom["afterMinutes"]?.let(::wholeMinutes)
  return if (level != null && afterMinutes != null) {
    ReverificationConfig.Custom(level, afterMinutes).takeIf { resolveReverification(it) != null }
  } else {
    null
  }
}

private fun wholeMinutes(element: JsonElement): Int? =
  (element as? JsonPrimitive)
    ?.takeUnless { it.isString }
    ?.doubleOrNull
    ?.let(::floor)
    ?.takeIf { it in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble() }
    ?.toInt()
