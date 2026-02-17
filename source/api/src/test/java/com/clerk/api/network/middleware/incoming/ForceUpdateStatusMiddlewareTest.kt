package com.clerk.api.network.middleware.incoming

import com.clerk.api.Clerk
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ForceUpdateStatusMiddlewareTest {
  @Test
  fun `unsupported_app_version 426 updates force update status`() {
    val middleware = ForceUpdateStatusMiddleware()
    Clerk.refreshForceUpdateStatus()

    val request = Request.Builder().url("https://example.com/v1/client").build()
    val response =
      Response
        .Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(426)
        .message("Upgrade Required")
        .body(
          """
          {
            "errors": [
              {
                "code": "unsupported_app_version",
                "message": "unsupported app version",
                "meta": {
                  "platform": "android",
                  "app_identifier": "com.example.app",
                  "current_version": "1.0.0",
                  "minimum_version": "2.0.0",
                  "update_url": "https://play.google.com/store/apps/details?id=com.example.app"
                }
              }
            ]
          }
          """
            .trimIndent()
            .toResponseBody("application/json".toMediaTypeOrNull())
        )
        .build()

    val output = middleware.intercept(TestInterceptorChain(request, response))

    assertEquals(426, output.code)
    assertFalse(Clerk.forceUpdateStatus.value.isSupported)
    assertEquals("2.0.0", Clerk.forceUpdateStatus.value.minimumVersion)
  }
}

private class TestInterceptorChain(private val request: Request, private val response: Response) :
  Interceptor.Chain {
  override fun request(): Request = request

  override fun proceed(request: Request): Response = response

  override fun connection(): Connection? = null

  override fun call(): Call = throw UnsupportedOperationException("Not required for this test")

  override fun connectTimeoutMillis(): Int = 0

  override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

  override fun readTimeoutMillis(): Int = 0

  override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

  override fun writeTimeoutMillis(): Int = 0

  override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
}
