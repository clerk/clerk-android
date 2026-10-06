package com.clerk.api.user

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.middleware.incoming.ClientSyncingMiddleware
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(RobolectricTestRunner::class)
class UserReloadTest {
  private lateinit var fetchedClient: Client

  @Before
  fun setup() {
    val httpClient =
      OkHttpClient.Builder()
        .addInterceptor(ClientSyncingMiddleware(ClerkApi.json))
        .addInterceptor { chain ->
          val response = ClerkApi.json.encodeToString(Client.serializer(), fetchedClient)
          val body = """{"response":$response,"client":null}"""
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody(JSON))
            .build()
        }
        .build()
    val clientApi =
      Retrofit.Builder()
        .baseUrl("https://clerk.example.com/v1/")
        .client(httpClient)
        .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
        .addConverterFactory(ClerkApiResultConverterFactory)
        .addConverterFactory(ClerkApi.json.asConverterFactory(JSON))
        .build()
        .create(ClientApi::class.java)
    mockkObject(ClerkApi)
    every { ClerkApi.client } returns clientApi
  }

  @After
  fun tearDown() {
    Clerk.updateClient(Client())
    Clerk.clearSessionAndUserState()
    unmockkAll()
  }

  @Test
  fun `reload applies the fetched client to Clerk user`() = runTest {
    val stale = user(firstName = "Stale")
    Clerk.updateClient(clientWith(session("sess_1", stale)))
    fetchedClient = clientWith(session("sess_1", user(firstName = "Fresh")))

    stale.reload()

    assertEquals("Fresh", Clerk.user?.firstName)
  }

  @Test
  fun `reload does not apply a client from a stale device token response`() = runTest {
    val stale = user(firstName = "Stale")
    Clerk.updateClient(clientWith(session("sess_1", stale)))
    fetchedClient = clientWith(session("sess_1", user(firstName = "Fresh")))
    mockkObject(Clerk)
    every { Clerk.isClientResponseCurrent(any(), any()) } returns false

    stale.reload()

    assertEquals("Stale", Clerk.user?.firstName)
  }

  @Test
  fun `reload returns the fetched user instead of the stale Clerk user`() = runTest {
    val stale = user(firstName = "Stale")
    Clerk.updateClient(clientWith(session("sess_1", stale)))
    fetchedClient = clientWith(session("sess_1", user(firstName = "Fresh")))

    val result = stale.reload()

    assertTrue(result is ClerkResult.Success)
    assertEquals("Fresh", (result as ClerkResult.Success).value.firstName)
  }

  @Test
  fun `reload returns the receiver's user when another session is active`() = runTest {
    val stale = user(id = "user_other", firstName = "Stale")
    fetchedClient =
      Client(
        sessions =
          listOf(
            session("sess_active", user(firstName = "Active")),
            session("sess_other", user(id = "user_other", firstName = "Fresh")),
          ),
        lastActiveSessionId = "sess_active",
      )

    val result = stale.reload()

    assertTrue(result is ClerkResult.Success)
    val reloaded = (result as ClerkResult.Success).value
    assertEquals("user_other", reloaded.id)
    assertEquals("Fresh", reloaded.firstName)
  }

  @Test
  fun `reload falls back to me when the client omits the user`() = runTest {
    val stale = user(firstName = "Stale")
    fetchedClient = Client(id = "client_1", sessions = emptyList())
    val fresh = user(firstName = "Fresh")
    val userApi = mockk<UserApi>()
    coEvery { userApi.getUser(any()) } returns ClerkResult.success(fresh)
    every { ClerkApi.user } returns userApi

    val result = stale.reload()

    assertSame(fresh, (result as ClerkResult.Success).value)
  }

  @Test
  fun `reload fails instead of returning a different user from the me fallback`() = runTest {
    val stale = user(id = "user_other", firstName = "Stale")
    fetchedClient = Client(id = "client_1", sessions = emptyList())
    val userApi = mockk<UserApi>()
    coEvery { userApi.getUser(any()) } returns ClerkResult.success(user(firstName = "Active"))
    every { ClerkApi.user } returns userApi

    val result = stale.reload()

    assertTrue(result is ClerkResult.Failure)
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, (result as ClerkResult.Failure).errorType)
  }

  private fun clientWith(session: Session): Client =
    Client(id = "client_1", sessions = listOf(session), lastActiveSessionId = session.id)

  private fun session(id: String, user: User): Session =
    Session(
      id = id,
      status = Session.SessionStatus.ACTIVE,
      expireAt = 0L,
      lastActiveAt = 0L,
      createdAt = 0L,
      updatedAt = 0L,
      user = user,
    )

  private fun user(id: String = "user_1", firstName: String): User =
    User(
      id = id,
      imageUrl = "",
      hasImage = false,
      passkeys = emptyList(),
      passwordEnabled = false,
      phoneNumbers = emptyList(),
      totpEnabled = false,
      twoFactorEnabled = false,
      updatedAt = 0L,
      firstName = firstName,
    )

  private companion object {
    val JSON = "application/json; charset=utf-8".toMediaType()
  }
}
