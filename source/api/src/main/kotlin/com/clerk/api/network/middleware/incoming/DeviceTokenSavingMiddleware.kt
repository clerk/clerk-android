package com.clerk.api.network.middleware.incoming

import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.network.middleware.ResponseGuard
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
    val request = chain.request()
    val response = chain.proceed(request)
    val deviceToken = response.header(AUTHORIZATION_HEADER)

    // Save the device token to storage whenever it's present in the response
    deviceToken?.let { token ->
      val saveToken = { StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, token) }
      request.tag(ResponseGuard::class.java)?.runIfAllowed(saveToken) ?: saveToken()
    }

    return response
  }
}
