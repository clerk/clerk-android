package com.clerk.api.network.middleware.outgoing

import com.clerk.api.forceupdate.AppInfoProvider
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class VersioningUserAgentMiddlewareTest {
  @Before
  fun setup() {
    mockkObject(AppInfoProvider)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `adds app version and package headers when metadata exists`() {
    every { AppInfoProvider.appVersion() } returns "1.2.3"
    every { AppInfoProvider.packageName() } returns "com.example.app"

    val middleware = VersioningUserAgentMiddleware()
    val request = Request.Builder().url("https://example.com/v1/client").build()
    var capturedRequest: Request? = null
    val chain =
      object : Interceptor.Chain {
        override fun request(): Request = request

        override fun proceed(request: Request): Response {
          capturedRequest = request
          return Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
            .build()
        }

        override fun connection(): Connection? = null

        override fun call(): Call = throw UnsupportedOperationException("Not required")

        override fun connectTimeoutMillis(): Int = 0

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun readTimeoutMillis(): Int = 0

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 0

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
      }

    middleware.intercept(chain)

    assertEquals("1.2.3", capturedRequest?.header("x-app-version"))
    assertEquals("com.example.app", capturedRequest?.header("x-package-name"))
  }

  @Test
  fun `omits app version and package headers when metadata missing`() {
    every { AppInfoProvider.appVersion() } returns null
    every { AppInfoProvider.packageName() } returns null

    val middleware = VersioningUserAgentMiddleware()
    val request = Request.Builder().url("https://example.com/v1/client").build()
    var capturedRequest: Request? = null
    val chain =
      object : Interceptor.Chain {
        override fun request(): Request = request

        override fun proceed(request: Request): Response {
          capturedRequest = request
          return Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
            .build()
        }

        override fun connection(): Connection? = null

        override fun call(): Call = throw UnsupportedOperationException("Not required")

        override fun connectTimeoutMillis(): Int = 0

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun readTimeoutMillis(): Int = 0

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 0

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
      }

    middleware.intercept(chain)

    assertNull(capturedRequest?.header("x-app-version"))
    assertNull(capturedRequest?.header("x-package-name"))
  }
}
