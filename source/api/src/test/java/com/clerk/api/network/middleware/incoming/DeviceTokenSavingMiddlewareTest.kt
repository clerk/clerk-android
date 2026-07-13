package com.clerk.api.network.middleware.incoming

import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.network.middleware.ResponseGuard
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Test

class DeviceTokenSavingMiddlewareTest {
  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `intercept saves device token even when request no longer owns the flow`() {
    mockkObject(StorageHelper)
    justRun { StorageHelper.saveValue(any<StorageKey>(), any<String>()) }
    val request =
      Request.Builder()
        .url("https://api.clerk.com/v1/client")
        .tag(ResponseGuard::class.java, ResponseGuard { _ -> })
        .build()
    val response =
      Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .header(AUTHORIZATION_HEADER, "device_token_rotated")
        .build()
    val chain = mockk<Interceptor.Chain>()
    every { chain.request() } returns request
    every { chain.proceed(request) } returns response

    DeviceTokenSavingMiddleware().intercept(chain)

    verify(exactly = 1) { StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_rotated") }
  }

  @Test
  fun `intercept does not save when response has no authorization header`() {
    mockkObject(StorageHelper)
    justRun { StorageHelper.saveValue(any<StorageKey>(), any<String>()) }
    val request = Request.Builder().url("https://api.clerk.com/v1/client").build()
    val response =
      Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .build()
    val chain = mockk<Interceptor.Chain>()
    every { chain.request() } returns request
    every { chain.proceed(request) } returns response

    DeviceTokenSavingMiddleware().intercept(chain)

    verify(exactly = 0) { StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, any<String>()) }
  }
}
