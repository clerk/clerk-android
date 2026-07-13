package com.clerk.api.network.middleware.incoming

import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.log.ClerkLog
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that saves device tokens from response headers to local storage.
 *
 * This middleware intercepts network responses and checks for the presence of an Authorization
 * header. If found, the token is automatically saved to the device's local storage for future use.
 */
internal class DeviceTokenSavingMiddleware : Interceptor {
  /**
   * Intercepts the network response to save any device token present in the Authorization header.
   *
   * @param chain The interceptor chain containing the request and response information.
   * @return The unmodified response after saving any device token found.
   */
  override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())
    val deviceToken = response.header(AUTHORIZATION_HEADER)
    val requestDeviceToken = response.request.header(AUTHORIZATION_HEADER)
    val currentDeviceToken = StorageHelper.loadValue(StorageKey.DEVICE_TOKEN)

    // Do not let a response that started with an older shared token overwrite the newer token.
    if (deviceToken != null && currentDeviceToken == requestDeviceToken) {
      StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, deviceToken)
    } else if (deviceToken != null) {
      ClerkLog.d("Device token update skipped for a stale shared-session response")
    }

    return response
  }
}
