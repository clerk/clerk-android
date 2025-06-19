package com.clerk.network.middleware.outgoing

import com.clerk.Clerk
import com.clerk.storage.StorageHelper
import com.clerk.storage.StorageKey
import okhttp3.Interceptor
import okhttp3.Response

private const val CURRENT_API_VERSION = "2024-10-01"
private const val CURRENT_SDK_VERSION = "0.1.0"
private const val IS_MOBILE_HEADER_VALUE = "1"

/**
 * HeaderMiddleware is an OkHttp interceptor that adds custom clerk specific headers to outgoing
 * requests.
 *
 * This is never intended to be used directly by the user.
 */
internal class HeaderMiddleware : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val newRequestBuilder =
      request
        .newBuilder()
        .addHeader(OutgoingHeaders.CLERK_API_VERSION.header, CURRENT_API_VERSION)
        .addHeader(OutgoingHeaders.X_ANDROID_SDK_VERSION.header, CURRENT_SDK_VERSION)
        .addHeader(OutgoingHeaders.X_MOBILE.header, IS_MOBILE_HEADER_VALUE)
    //        .addHeader(OutgoingHeaders.X_CLERK_DEVICE_ID.header,
    // DeviceIdGenerator.getOrGenerateDeviceId())

    if (Clerk.isInitialized.value) {
      Clerk.client.id?.let {
        newRequestBuilder.addHeader(OutgoingHeaders.X_CLERK_CLIENT_ID.header, it)
      }
    }

    StorageHelper.loadValue(StorageKey.DEVICE_TOKEN)?.let {
      newRequestBuilder.addHeader(OutgoingHeaders.AUTHORIZATION.header, it)
    }

    return chain.proceed(newRequestBuilder.build())
  }
}

private enum class OutgoingHeaders(val header: String) {
  CLERK_API_VERSION("clerk-api-version"),
  X_ANDROID_SDK_VERSION("x-android-sdk-version"),
  X_MOBILE("x-mobile"),
  AUTHORIZATION("Authorization"),
  X_CLERK_CLIENT_ID("x-clerk-client-id"),
  X_CLERK_DEVICE_ID("x-native-device-id:"),
}
