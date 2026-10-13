package com.clerk.api.network.middleware.incoming

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientSyncingMiddlewareOrderingTest {

  @After
  fun tearDown() {
    Clerk.stateStore.reset()
  }

  @Test
  fun `intercept drops a client response fetched before already applied state`() {
    val middleware = ClientSyncingMiddleware(json = ClerkApi.json)

    middleware.intercept(
      chainFor(piggybackedClientResponse("client_newer", date = "Mon, 13 Jul 2026 18:00:05 GMT"))
    )
    middleware.intercept(
      chainFor(piggybackedClientResponse("client_older", date = "Mon, 13 Jul 2026 18:00:00 GMT"))
    )

    assertEquals("client_newer", Clerk.client.id)
    assertEquals(1_783_965_605_000L, Clerk.lastClientServerFetchAtMillis)
  }

  @Test
  fun `refresh result sharing a Date second with a newer sign-in keeps the sign-in`() {
    val middleware = ClientSyncingMiddleware(json = ClerkApi.json)
    val updateCountAtStart = Clerk.clientUpdateCount
    val clientFetch = Request.Builder().url("https://api.clerk.com/v1/client").build()
    val fetchedClientJson = """{"object": "client", "id": "client_refreshed", "sessions": []}"""
    middleware.intercept(
      chainFor(
        Response.Builder()
          .request(clientFetch)
          .protocol(Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .header("Date", "Mon, 13 Jul 2026 18:00:00 GMT")
          .body(
            """{"response": $fetchedClientJson, "client": null}"""
              .toResponseBody("application/json".toMediaType())
          )
          .build()
      )
    )
    middleware.intercept(
      chainFor(
        piggybackedClientResponse("client_signed_in", date = "Mon, 13 Jul 2026 18:00:00 GMT")
      )
    )

    assertFalse(
      Clerk.updateClientIfUnchangedSince(
        updateCountAtStart,
        ClerkApi.json.decodeFromString<Client>(fetchedClientJson),
      )
    )
    assertEquals("client_signed_in", Clerk.client.id)
  }

  @Test
  fun `older piggybacked response arriving after an applied refresh is dropped`() {
    val middleware = ClientSyncingMiddleware(json = ClerkApi.json)
    val updateCountAtStart = Clerk.clientUpdateCount
    val refreshResponse =
      middleware.intercept(
        chainFor(clientFetchResponse("client_refreshed", date = "Mon, 13 Jul 2026 18:00:05 GMT"))
      )
    val refreshResult =
      ClerkResult.success(Client(id = "client_refreshed"))
        .withTags(mapOf(Response::class to refreshResponse))

    assertTrue(Clerk.updateClientIfUnchangedSince(updateCountAtStart, refreshResult))
    middleware.intercept(
      chainFor(piggybackedClientResponse("client_older", date = "Mon, 13 Jul 2026 18:00:00 GMT"))
    )

    assertEquals("client_refreshed", Clerk.client.id)
  }

  private fun clientFetchResponse(clientId: String, date: String): Response =
    Response.Builder()
      .request(Request.Builder().url("https://api.clerk.com/v1/client").build())
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .header("Date", date)
      .body(
        """{"response": {"object": "client", "id": "$clientId", "sessions": []}, "client": null}"""
          .toResponseBody("application/json".toMediaType())
      )
      .build()

  private fun piggybackedClientResponse(clientId: String, date: String): Response {
    val request =
      Request.Builder()
        .url("https://api.clerk.com/v1/client/sessions/sess_123/touch")
        .post("".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
        .build()
    return Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .header("Date", date)
      .body(
        """{"response": {"object": "session"}, "client": {"id": "$clientId", "sessions": []}}"""
          .toResponseBody("application/json".toMediaType())
      )
      .build()
  }

  private fun chainFor(response: Response): Interceptor.Chain {
    val chain = mockk<Interceptor.Chain>()
    every { chain.request() } returns response.request
    every { chain.proceed(response.request) } returns response
    return chain
  }
}
