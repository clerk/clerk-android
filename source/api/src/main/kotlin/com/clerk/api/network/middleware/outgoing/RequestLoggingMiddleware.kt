package com.clerk.api.network.middleware.outgoing

import com.clerk.api.network.middleware.SensitiveRequest
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

internal class RequestLoggingMiddleware(private val loggingInterceptor: HttpLoggingInterceptor) :
  Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    return if (chain.request().tag(SensitiveRequest::class.java) != null) {
      chain.proceed(chain.request())
    } else {
      loggingInterceptor.intercept(chain)
    }
  }

  companion object {
    fun create(
      logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
    ): RequestLoggingMiddleware =
      RequestLoggingMiddleware(
        HttpLoggingInterceptor { message -> logger.log(redactSecrets(message)) }
          .apply {
            level = HttpLoggingInterceptor.Level.BODY
            REDACTED_HEADERS.forEach(::redactHeader)
          }
      )
  }
}

private const val REDACTED = "██"

private val REDACTED_HEADERS = listOf("Authorization", "Cookie", "Set-Cookie")

private val SENSITIVE_FORM_FIELDS =
  listOf(
    "password",
    "current_password",
    "new_password",
    "code",
    "token",
    "ticket",
    "code_verifier",
    "id_token",
    "approval_token",
    "rotating_token_nonce",
    "signature",
  )

// "code" is deliberately absent: in JSON responses it holds error codes, which debugging needs.
private val SENSITIVE_JSON_KEYS =
  listOf("jwt", "secret", "token", "ticket", "code_verifier", "id_token", "approval_token")

private val formFieldPattern =
  Regex("(^|[?&])(${SENSITIVE_FORM_FIELDS.joinToString("|")})=[^&\\s]*")

private val jsonStringPattern =
  Regex("(\"(?:${SENSITIVE_JSON_KEYS.joinToString("|")})\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"")

private val backupCodesPattern = Regex("(\"(?:backup_codes|codes)\"\\s*:\\s*)\\[[^\\]]*]")

private val otpAuthUriPattern = Regex("otpauth://[^\"\\s]*")

internal fun redactSecrets(message: String): String =
  message
    .replace(formFieldPattern, "$1$2=$REDACTED")
    .replace(jsonStringPattern, "$1\"$REDACTED\"")
    .replace(backupCodesPattern, "$1[\"$REDACTED\"]")
    .replace(otpAuthUriPattern, REDACTED)
