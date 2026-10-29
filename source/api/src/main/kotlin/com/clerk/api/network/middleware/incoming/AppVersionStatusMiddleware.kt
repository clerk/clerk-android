package com.clerk.api.network.middleware.incoming

import com.clerk.api.appversion.AppVersionSupport
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import java.io.IOException
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response

internal const val UNSUPPORTED_APP_VERSION_CODE = "unsupported_app_version"
private const val APP_VERSION_STATUS_HEADER = "X-Clerk-App-Version-Status"
private const val APP_VERSION_STATUS_SUPPORTED = "supported"
private const val MAX_ERROR_BODY_BYTES = 64L * 1024

internal class AppVersionStatusMiddleware(private val json: Json) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = AppVersionSupport.nextRequest()
    val response = chain.proceed(chain.request())

    val unsupportedAppVersionError =
      if (response.code == HTTP_FORBIDDEN) unsupportedAppVersionError(response) else null
    when {
      unsupportedAppVersionError != null ->
        AppVersionSupport.applyUnsupportedAppVersion(unsupportedAppVersionError.meta, request)
      response
        .header(APP_VERSION_STATUS_HEADER)
        .equals(APP_VERSION_STATUS_SUPPORTED, ignoreCase = true) ->
        AppVersionSupport.applySupportedAppVersion(request)
    }
    return response
  }

  private fun unsupportedAppVersionError(response: Response): Error? =
    try {
      json
        .decodeFromString<ClerkErrorResponse>(response.peekBody(MAX_ERROR_BODY_BYTES).string())
        .errors
        .firstOrNull { it.code == UNSUPPORTED_APP_VERSION_CODE }
    } catch (_: SerializationException) {
      null
    } catch (_: IOException) {
      null
    }
}
