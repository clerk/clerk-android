package com.clerk.api.sdk

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.middleware.incoming.ClientSyncingMiddleware
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.every
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(RobolectricTestRunner::class)
class ClerkRefreshClientTest {
  private var fetchedClient = Client(id = "client_fetched")
  private var whileFetchIsInFlight: () -> Unit = {}

  @Before
  fun setUp() {
    StorageHelper.resetToUninitializedForTesting()
    StorageHelper.initialize(RuntimeEnvironment.getApplication())
    Clerk.updateClient(Client(id = "client_baseline"))
    val httpClient =
      OkHttpClient.Builder()
        .addInterceptor(ClientSyncingMiddleware(ClerkApi.json))
        .addInterceptor { chain ->
          whileFetchIsInFlight()
          val response = ClerkApi.json.encodeToString(Client.serializer(), fetchedClient)
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"response":$response,"client":null}""".toResponseBody(JSON))
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
    unmockkAll()
    Clerk.updateClient(Client())
    Clerk.clearSessionAndUserState()
    StorageHelper.reset(RuntimeEnvironment.getApplication())
  }

  @Test
  fun `refreshClient applies the fetched client`() = runTest {
    val result = Clerk.refreshClient()

    assertEquals(fetchedClient, Clerk.client)
    assertEquals(fetchedClient, (result as ClerkResult.Success).value)
  }

  @Test
  fun `refreshClient returns the current client when a newer update landed in flight`() = runTest {
    val newer = Client(id = "client_newer")
    whileFetchIsInFlight = { Clerk.updateClient(newer) }

    val result = Clerk.refreshClient()

    assertEquals(newer, Clerk.client)
    assertEquals(newer, (result as ClerkResult.Success).value)
  }

  @Test
  fun `refreshClient does not apply a response whose device token went stale in flight`() =
    runTest {
      whileFetchIsInFlight = {
        StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_after_switch")
      }

      val result = Clerk.refreshClient()

      assertEquals("client_baseline", Clerk.client.id)
      assertEquals("client_baseline", (result as ClerkResult.Success).value.id)
    }

  private companion object {
    val JSON = "application/json".toMediaType()
  }
}
