package com.clerk.api.network.middleware.incoming

import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

internal class ForceUpdateStatusMiddleware : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())
    if (response.code != 426) {
      return response
    }

    val body = response.body ?: return response
    val contentType = body.contentType()
    val bodyString = body.string()
    val copiedResponse = response.newBuilder().body(bodyString.toResponseBody(contentType)).build()

    if (bodyString.isBlank()) {
      return copiedResponse
    }

    val clerkError =
      try {
        ClerkApi.json.decodeFromString<ClerkErrorResponse>(bodyString)
      } catch (e: Exception) {
        ClerkLog.e("Failed to parse force update error response: ${e.message}")
        return copiedResponse
      }

    val unsupportedVersionError =
      clerkError.errors.firstOrNull { it.code == "unsupported_app_version" } ?: return copiedResponse

    Clerk.updateForceUpdateStatusFromUnsupportedErrorMeta(unsupportedVersionError.meta)
    return copiedResponse
  }
}
