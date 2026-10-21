package com.clerk.api.network.middleware.incoming

import com.clerk.api.appversion.AppBuild
import com.clerk.api.appversion.AppVersionSupport
import com.clerk.api.appversion.TEST_PACKAGE_NAME
import com.clerk.api.network.ClerkApi
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppVersionStatusMiddlewareTest {
  @Before
  fun setUp() {
    AppVersionSupport.reset()
    AppVersionSupport.configure(AppBuild(TEST_PACKAGE_NAME, versionCode = 41))
  }

  @After
  fun tearDown() {
    AppVersionSupport.reset()
  }

  @Test
  fun `an unsupported app version response blocks the app and keeps its body`() {
    val response = intercept(code = 403, body = UNSUPPORTED_APP_VERSION_BODY)

    val status = AppVersionSupport.status.value
    assertFalse(status.isSupported)
    assertEquals(42L, status.minimumVersion)
    assertEquals(UNSUPPORTED_APP_VERSION_BODY, response.body.string())
  }

  @Test
  fun `another 403 leaves the app supported`() {
    intercept(code = 403, body = """{"errors":[{"code":"authorization_invalid"}]}""")
    intercept(code = 403, body = "not json")

    assertTrue(AppVersionSupport.status.value.isSupported)
  }

  @Test
  fun `the supported header lifts a block`() {
    intercept(code = 403, body = UNSUPPORTED_APP_VERSION_BODY)

    intercept(code = 200, body = "{}", appVersionStatus = "supported")

    assertTrue(AppVersionSupport.status.value.isSupported)
  }

  @Test
  fun `a response without the supported header keeps a block`() {
    intercept(code = 403, body = UNSUPPORTED_APP_VERSION_BODY)

    intercept(code = 200, body = "{}")

    assertFalse(AppVersionSupport.status.value.isSupported)
  }

  @Test
  fun `a response to a request sent before reset is ignored`() {
    val chain = chain(code = 403, body = UNSUPPORTED_APP_VERSION_BODY)
    every { chain.proceed(any()) } answers
      {
        AppVersionSupport.reset()
        response(code = 403, body = UNSUPPORTED_APP_VERSION_BODY, appVersionStatus = null)
      }

    AppVersionStatusMiddleware(ClerkApi.json).intercept(chain)

    assertTrue(AppVersionSupport.status.value.isSupported)
  }

  private fun intercept(code: Int, body: String, appVersionStatus: String? = null): Response =
    AppVersionStatusMiddleware(ClerkApi.json)
      .intercept(chain(code = code, body = body, appVersionStatus = appVersionStatus))

  private fun chain(code: Int, body: String, appVersionStatus: String? = null): Interceptor.Chain {
    val chain = mockk<Interceptor.Chain>()
    every { chain.request() } returns REQUEST
    every { chain.proceed(any()) } returns response(code, body, appVersionStatus)
    return chain
  }

  private fun response(code: Int, body: String, appVersionStatus: String?): Response =
    Response.Builder()
      .request(REQUEST)
      .protocol(Protocol.HTTP_1_1)
      .code(code)
      .message("")
      .apply { appVersionStatus?.let { header("X-Clerk-App-Version-Status", it) } }
      .body(body.toResponseBody("application/json".toMediaType()))
      .build()

  private companion object {
    val REQUEST = Request.Builder().url("https://clerk.example.com/v1/client").build()
    const val UNSUPPORTED_APP_VERSION_BODY =
      """{"errors":[{"code":"unsupported_app_version","meta":{"platform":"android",""" +
        """"app_identifier":"com.example.app","current_version":"41","minimum_version":"42",""" +
        """"update_url":""}}]}"""
  }
}
