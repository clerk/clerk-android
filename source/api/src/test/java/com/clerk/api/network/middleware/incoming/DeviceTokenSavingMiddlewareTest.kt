package com.clerk.api.network.middleware.incoming

import android.content.Context
import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.network.middleware.ResponseGuard
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DeviceTokenSavingMiddlewareTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.reset(context)
  }

  @After
  fun tearDown() {
    StorageHelper.reset(context)
  }

  @Test
  fun `response token replaces token used by request`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "request-token")

    DeviceTokenSavingMiddleware()
      .intercept(chain(requestToken = "request-token", responseToken = "response-token"))

    assertEquals("response-token", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `flow response token is saved even when response guard rejects client sync`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "request-token")

    DeviceTokenSavingMiddleware()
      .intercept(
        chain(
          requestToken = "request-token",
          responseToken = "response-token",
          responseGuard = ResponseGuard { _ -> },
        )
      )

    assertEquals("response-token", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `response started with stale token cannot overwrite shared token`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "shared-token")

    DeviceTokenSavingMiddleware()
      .intercept(chain(requestToken = "stale-token", responseToken = "rotated-stale-token"))

    assertEquals("shared-token", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `response without token leaves stored token unchanged`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "request-token")

    DeviceTokenSavingMiddleware()
      .intercept(chain(requestToken = "request-token", responseToken = null))

    assertEquals("request-token", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  private fun chain(
    requestToken: String?,
    responseToken: String?,
    responseGuard: ResponseGuard? = null,
  ): Interceptor.Chain {
    val requestBuilder = Request.Builder().url("https://example.com")
    requestToken?.let { requestBuilder.header(AUTHORIZATION_HEADER, it) }
    responseGuard?.let { requestBuilder.tag(ResponseGuard::class.java, it) }
    val request = requestBuilder.build()
    val responseBuilder =
      Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
    responseToken?.let { responseBuilder.header(AUTHORIZATION_HEADER, it) }
    val response = responseBuilder.build()
    return mockk {
      every { request() } returns request
      every { proceed(request) } returns response
    }
  }
}
