package com.clerk.api.network.middleware.incoming

import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.log.ClerkLogger
import com.clerk.api.storage.StorageHelper
import okhttp3.Interceptor
import okhttp3.Response

internal class DeviceTokenSavingMiddleware : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())
    val deviceToken = response.header(AUTHORIZATION_HEADER)
    val requestDeviceToken = response.request.header(AUTHORIZATION_HEADER)

    if (
      deviceToken != null &&
        !StorageHelper.compareAndSetDeviceToken(expected = requestDeviceToken, value = deviceToken)
    ) {
      ClerkLogger.d("Device token update skipped for a stale shared-session response")
    }

    return response
  }
}
