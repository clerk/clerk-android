package com.clerk.api.network.middleware.outgoing

import com.clerk.api.Clerk
import com.clerk.api.Constants
import com.clerk.api.configuration.DeviceIdGenerator
import com.clerk.api.network.paths.Paths
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import okhttp3.Interceptor
import okhttp3.Response

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
        .addHeader(OutgoingHeaders.CLERK_API_VERSION.header, Constants.Http.CURRENT_API_VERSION)
        .addHeader(OutgoingHeaders.X_ANDROID_SDK_VERSION.header, Constants.Http.CURRENT_SDK_VERSION)
        .addHeader(OutgoingHeaders.X_MOBILE.header, Constants.Http.IS_MOBILE_HEADER_VALUE)
        .addHeader(
          OutgoingHeaders.X_CLERK_DEVICE_ID.header,
          DeviceIdGenerator.getOrGenerateDeviceId(),
        )

    if (Clerk.isInitialized.value) {
      Clerk.client.id?.let {
        newRequestBuilder.addHeader(OutgoingHeaders.X_CLERK_CLIENT_ID.header, it)
      }
    }

    StorageHelper.loadValue(StorageKey.DEVICE_TOKEN)?.let {
      newRequestBuilder.addHeader(OutgoingHeaders.AUTHORIZATION.header, it)
    }

    // See: https://community.cloudflare.com/t/cannot-seem-to-send-multipart-form-data/163491
    if (request.url.encodedPath.contains(Paths.UserPath.PROFILE_IMAGE)) {
      newRequestBuilder.removeHeader("Content-Type")
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
  X_CLERK_DEVICE_ID("x-native-device-id"),
}
