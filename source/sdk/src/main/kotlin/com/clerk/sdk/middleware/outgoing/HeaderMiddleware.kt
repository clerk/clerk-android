package com.clerk.sdk.middleware.outgoing

import okhttp3.Interceptor
import okhttp3.Response

class HeaderMiddleware : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val newRequest = request.newBuilder().addHeader("X-Clerk-Client", "android").build()
    return chain.proceed(newRequest)
  }
}
