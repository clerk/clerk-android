package com.clerk.api.network.middleware.outgoing

import com.clerk.api.Constants.Http.IS_NATIVE_QUERY_PARAM
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Internal OkHttp interceptor that appends native client query parameters to requests.
 *
 * This middleware automatically adds the `_is_native` query parameter to all outgoing HTTP requests
 * to identify them as coming from a native mobile client. This allows the Clerk API to provide
 * mobile-specific responses and behavior.
 *
 * The interceptor modifies the request URL by adding the query parameter before forwarding the
 * request to the next interceptor in the chain.
 */
internal class UrlAppendingMiddleware : Interceptor {
  /**
   * Intercepts the request and appends the native client query parameter.
   *
   * This method modifies the original request URL by adding the `_is_native=true` query parameter,
   * then proceeds with the modified request.
   *
   * @param chain The interceptor chain to proceed with
   * @return The response from the next interceptor in the chain
   */
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val originalUrl = originalRequest.url

    val appendedUrl =
      originalUrl.newBuilder().addQueryParameter(IS_NATIVE_QUERY_PARAM, true.toString()).build()

    val newRequest = originalRequest.newBuilder().url(appendedUrl).build()
    return chain.proceed(newRequest)
  }
}
