package com.clerk.api.network.middleware.outgoing

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.configuration.ConfigurationManager
import com.clerk.api.network.model.client.Client
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import kotlinx.coroutines.Job
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class VersioningUserAgentMiddlewareTest {
  private lateinit var context: Context

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.reset(context)
    resetClerkState()
    configureInitializedClerk()
  }

  @After
  fun tearDown() {
    unmockkAll()
    StorageHelper.reset(context)
    resetClerkState()
  }

  @Test
  fun `intercept adds client id and authorization for normal requests`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_123")
    val capturedRequest = captureRequest(request = Request.Builder().url(TEST_URL).get().build())

    assertEquals("client_123", capturedRequest.header("x-clerk-client-id"))
    assertEquals("device_token_123", capturedRequest.header("Authorization"))
  }

  @Test
  fun `intercept omits client id and strips internal header for marked requests`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_123")
    val capturedRequest =
      captureRequest(
        request =
          Request.Builder()
            .url(TEST_URL)
            .get()
            .header(INTERNAL_HEADER_SKIP_CLIENT_ID, INTERNAL_HEADER_TRUE)
            .build()
      )

    assertNull(capturedRequest.header("x-clerk-client-id"))
    assertNull(capturedRequest.header(INTERNAL_HEADER_SKIP_CLIENT_ID))
    assertEquals("device_token_123", capturedRequest.header("Authorization"))
  }

  private fun captureRequest(request: Request): Request {
    val chain = mockk<Interceptor.Chain>()
    val requestSlot = slot<Request>()
    every { chain.request() } returns request
    every { chain.proceed(capture(requestSlot)) } answers { buildResponse(requestSlot.captured) }

    VersioningUserAgentMiddleware().intercept(chain)

    return requestSlot.captured
  }

  private fun buildResponse(request: Request): Response {
    return Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .body("{}".toResponseBody("application/json".toMediaType()))
      .build()
  }

  private fun configureInitializedClerk() {
    val configurationManager = configurationManager()
    setField(configurationManager, "context", WeakReference(context))
    setField(configurationManager, "hasConfigured", true)
    setField(configurationManager, "storageInitialized", false)
    mutableStateFlow<Boolean>(configurationManager, "_isInitialized").value = true
    Clerk.updateClient(Client(id = "client_123"))
  }

  private fun resetClerkState() {
    val configurationManager = configurationManager()
    cancelJobField(configurationManager, "refreshJob")
    cancelJobField(configurationManager, "attestationJob")
    cancelJobField(configurationManager, "initializationJob")
    setField(configurationManager, "context", null)
    setField(configurationManager, "hasConfigured", false)
    setField(configurationManager, "storedOptions", null)
    setField(configurationManager, "storageInitialized", false)
    mutableStateFlow<Boolean>(configurationManager, "_isInitialized").value = false
    mutableStateFlow<Throwable?>(configurationManager, "_initializationError").value = null
    Clerk.clearSessionAndUserState()
    Clerk.updateClient(Client())
  }

  private fun configurationManager(): ConfigurationManager {
    val field = Clerk::class.java.getDeclaredField("configurationManager")
    field.isAccessible = true
    return field.get(Clerk) as ConfigurationManager
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> mutableStateFlow(
    target: Any,
    name: String,
  ): kotlinx.coroutines.flow.MutableStateFlow<T> {
    val field = target.javaClass.getDeclaredField(name)
    field.isAccessible = true
    return field.get(target) as kotlinx.coroutines.flow.MutableStateFlow<T>
  }

  private fun setField(target: Any, name: String, value: Any?) {
    val field = target.javaClass.getDeclaredField(name)
    field.isAccessible = true
    field.set(target, value)
  }

  private fun cancelJobField(target: Any, name: String) {
    val field = target.javaClass.getDeclaredField(name)
    field.isAccessible = true
    (field.get(target) as? Job)?.cancel()
    field.set(target, null)
  }

  companion object {
    private const val TEST_URL = "https://example.com/v1/client"
  }
}
