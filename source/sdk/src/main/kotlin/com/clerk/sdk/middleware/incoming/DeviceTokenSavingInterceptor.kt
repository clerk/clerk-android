package com.clerk.sdk.middleware.incoming

import okhttp3.Interceptor
import okhttp3.Response

class DeviceTokenSavingInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())
    return response
  }
}
