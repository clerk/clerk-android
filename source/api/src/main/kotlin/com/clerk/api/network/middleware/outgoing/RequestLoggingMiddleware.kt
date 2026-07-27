package com.clerk.api.network.middleware.outgoing

import com.clerk.api.network.middleware.SensitiveRequest
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

internal class RequestLoggingMiddleware(private val loggingInterceptor: HttpLoggingInterceptor) :
  Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    return if (chain.request().tag(SensitiveRequest::class.java) != null) {
      chain.proceed(chain.request())
    } else {
      loggingInterceptor.intercept(chain)
    }
  }
}
