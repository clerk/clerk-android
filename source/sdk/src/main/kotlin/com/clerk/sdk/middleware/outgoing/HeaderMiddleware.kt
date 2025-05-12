package com.clerk.sdk.middleware.outgoing

import okhttp3.Interceptor
import okhttp3.Response

/**
 * HeaderMiddleware is an OkHttp interceptor that adds custom headers to outgoing requests. It adds
 * the following headers:
 * - X-Clerk-Client: "android"
 * - clerk-api-version: "{Current API Version}"
 * - x-android-sdk-version: "{Current SDK Version}"
 * - x-mobile: "1"
 */
class HeaderMiddleware : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val newRequest =
      request
        .newBuilder()
        .addHeader(OutgoingHeaders.X_CLERK_CLIENT.header, "android")
        .addHeader(OutgoingHeaders.CLERK_API_VERSION.header, "2024-10-01")
        .addHeader(OutgoingHeaders.X_ANDROID_SDK_VERSION.header, "0.1.0")
        .addHeader(OutgoingHeaders.X_MOBILE.header, "1")
        .build()

    return chain.proceed(newRequest)
  }
}

enum class OutgoingHeaders(val header: String) {
  X_CLERK_CLIENT("X-Clerk-Client"),
  CLERK_API_VERSION("clerk-api-version"),
  X_ANDROID_SDK_VERSION("x-android-sdk-version"),
  X_MOBILE("x-mobile"),
}
