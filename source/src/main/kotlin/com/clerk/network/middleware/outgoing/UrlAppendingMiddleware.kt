package com.clerk.network.middleware.outgoing

import com.clerk.Constants.Http.IS_NATIVE_QUERY_PARAM
import okhttp3.Interceptor
import okhttp3.Response

internal class UrlAppendingMiddleware : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val originalUrl = originalRequest.url

    val appendedUrl =
      originalUrl.newBuilder().addQueryParameter(IS_NATIVE_QUERY_PARAM, true.toString()).build()

    val newRequest = originalRequest.newBuilder().url(appendedUrl).build()
    return chain.proceed(newRequest)
  }
}
