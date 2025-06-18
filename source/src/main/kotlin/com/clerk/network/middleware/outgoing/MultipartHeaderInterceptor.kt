package com.clerk.network.middleware.outgoing

import com.clerk.network.paths.Paths
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.Response

internal class MultipartHeaderInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()

    // Only modify requests to your profile image endpoint
    if (
      request.url.encodedPath.contains(Paths.UserPath.PROFILE_IMAGE) &&
        request.body is MultipartBody
    ) {

      val multipartBody = request.body as MultipartBody
      // Extract the boundary that MultipartBody generated
      val boundary = multipartBody.boundary

      // Create new request with custom Content-Type using the same boundary
      val newRequest =
        request
          .newBuilder()
          .header("Content-Type", "multipart/form-data; boundary=$boundary")
          .build()

      return chain.proceed(newRequest)
    }

    return chain.proceed(request)
  }
}
