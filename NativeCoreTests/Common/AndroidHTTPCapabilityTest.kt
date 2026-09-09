package com.clerk.api

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.*
import org.junit.Test

class AndroidHTTPCapabilityTest {
  private val storage = object : CredentialStorage {
    override suspend fun read(): String? = error("Unexpected storage")
    override suspend fun write(value: String) { error("Unexpected storage") }
    override suspend fun remove() { error("Unexpected storage") }
  }

  private fun host(interceptor: Interceptor): AndroidCapabilities = AndroidCapabilities(
    publishableKey = "fixture", frontendAPI = "https://example.com", storage = storage,
    http = OkHttpClient.Builder().addInterceptor(interceptor).build(),
  )

  private fun args(method: String = "GET", body: String? = null) = buildJsonObject {
    put("url", "https://example.com/v1/test?value=a%26b")
    put("method", method)
    put("headers", buildJsonObject { put("Authorization", "fixture-token") })
    if (body != null) put("body", body)
  }

  private fun response(request: Request, body: ResponseBody, code: Int = 200) = Response.Builder()
    .request(request).protocol(Protocol.HTTP_1_1).code(code).message("Fixture")
    .header("X-Clerk-Trace-Id", "fixture-trace").header("Set-Cookie", "fixture=ignored")
    .body(body).build()

  @Test fun preservesMethodsQueryBodiesAndErrorResponse() = runBlocking {
    for (method in listOf("GET", "POST", "PATCH", "DELETE")) {
      val host = host { chain ->
        val request = chain.request()
        assertEquals(method, request.method)
        assertEquals("value=a%26b", request.url.encodedQuery)
        assertEquals("fixture-token", request.header("Authorization"))
        val bytes = Buffer().also { request.body?.writeTo(it) }.readUtf8()
        assertEquals(if (method == "GET") "" else "key=a%26b", bytes)
        response(request, "{\"errors\":[]}".toResponseBody(), 422)
      }
      val result = host.perform("http", args(method, if (method == "GET") null else "key=a%26b")).jsonObject
      assertEquals(JsonPrimitive(422), result["status"])
      assertEquals(JsonPrimitive("{\"errors\":[]}"), result["body"])
      assertEquals(JsonPrimitive("fixture-trace"), result.getValue("headers").jsonObject["x-clerk-trace-id"])
      assertNull(result.getValue("headers").jsonObject["set-cookie"])
    }
  }

  @Test fun invalidUTF8DoesNotSilentlyBecomeReplacementCharacters() = runBlocking {
    val host = host { chain -> response(chain.request(), byteArrayOf(0xff.toByte()).toResponseBody()) }
    try {
      host.perform("http", args())
      fail("Invalid UTF-8 must fail")
    } catch (error: CoreException) { assertEquals("invalid_http_response", error.code) }
  }

  @Test fun failuresBeforeAndAfterHeadersUseTheSameStructuredNetworkError() = runBlocking {
    val hosts = listOf(
      host { throw IOException("Fixture connection failure") },
      host { chain -> response(chain.request(), object : ResponseBody() {
        override fun contentType(): MediaType? = null
        override fun contentLength() = -1L
        override fun source(): BufferedSource = object : ForwardingSource(Buffer()) {
          override fun read(sink: Buffer, byteCount: Long): Long { throw IOException("Fixture body failure") }
        }.buffer()
      }) },
    )
    for (host in hosts) {
      try { host.perform("http", args()); fail("Expected network failure") }
      catch (error: CoreException) { assertEquals("network_error", error.code) }
    }
  }

  @Test fun cancellationRemainsConnectedWhileReadingResponseBody() = runBlocking {
    val reading = CountDownLatch(1)
    val stopped = CountDownLatch(1)
    val host = host { chain ->
      val body = object : ResponseBody() {
        override fun contentType(): MediaType? = null
        override fun contentLength() = -1L
        override fun source(): BufferedSource = object : ForwardingSource(Buffer()) {
          override fun read(sink: Buffer, byteCount: Long): Long {
            reading.countDown()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (!chain.call().isCanceled() && System.nanoTime() < deadline) Thread.sleep(5)
            if (chain.call().isCanceled()) stopped.countDown()
            throw IOException("Fixture body stopped")
          }
        }.buffer()
      }
      response(chain.request(), body)
    }
    val request = launch { host.perform("http", args()) }
    assertTrue("Response body was never read", withContext(Dispatchers.IO) { reading.await(2, TimeUnit.SECONDS) })
    request.cancel()
    assertTrue("Cancelling after headers left the response read active", withContext(Dispatchers.IO) { stopped.await(1, TimeUnit.SECONDS) })
    request.join()
  }
}
