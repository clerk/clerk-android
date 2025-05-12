package com.clerk.sdk.middleware.incoming

import com.clerk.sdk.storage.StorageHelper
import com.clerk.sdk.storage.StorageKey
import okhttp3.Interceptor
import okhttp3.Response

private const val AUTHORIZATION_HEADER = "Authorization"

/** Middleware to save the device token from the response header. */
class DeviceTokenSavingMiddleware : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())
    val deviceToken = response.header(AUTHORIZATION_HEADER)

    // Save the device token to storage whenever it's present in the response
    deviceToken?.let { token -> StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, token) }

    return response
  }
}
