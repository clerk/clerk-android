package com.clerk.api.organizations

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.middleware.incoming.ClientSyncingMiddleware
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.user.User
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
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
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(RobolectricTestRunner::class)
class OrganizationReloadTest {
  private lateinit var fetchedClient: Client
  private var whileFetchIsInFlight: () -> Unit = {}

  @Before
  fun setup() {
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
    Clerk.stateStore.reset()
    unmockkAll()
  }

  @Test
  fun `reload applies the fetched organization`() = runTest {
    val stale = organization(name = "Stale")
    Clerk.updateClient(clientWith(stale))
    fetchedClient = clientWith(organization(name = "Fresh"))

    val result = stale.reload()

    assertEquals("Fresh", (result as ClerkResult.Success).value.name)
    assertEquals("Fresh", Clerk.organization?.name)
  }

  @Test
  fun `reload returns the fetched organization when a newer update kept it from applying`() =
    runTest {
      val stale = organization(name = "Stale")
      Clerk.updateClient(clientWith(stale))
      fetchedClient = clientWith(organization(name = "Fresh"))
      whileFetchIsInFlight = { Clerk.mutateClient { it.copy(signIn = null) } }

      val result = stale.reload()

      assertEquals("Fresh", (result as ClerkResult.Success).value.name)
      assertEquals("Stale", Clerk.organization?.name)
    }

  private fun clientWith(organization: Organization): Client {
    val membership =
      OrganizationMembership(
        id = "orgmem_1",
        publicMetadata = JsonObject(emptyMap()),
        role = "org:admin",
        roleName = "Admin",
        organization = organization,
        createdAt = 0L,
        updatedAt = 0L,
      )
    val user =
      User(
        id = "user_1",
        imageUrl = "",
        hasImage = false,
        passkeys = emptyList(),
        passwordEnabled = false,
        phoneNumbers = emptyList(),
        totpEnabled = false,
        twoFactorEnabled = false,
        updatedAt = 0L,
        organizationMemberships = listOf(membership),
      )
    val session =
      Session(
        id = "sess_1",
        status = Session.SessionStatus.ACTIVE,
        expireAt = 0L,
        lastActiveAt = 0L,
        createdAt = 0L,
        updatedAt = 0L,
        user = user,
        lastActiveOrganizationId = organization.id,
      )
    return Client(id = "client_1", sessions = listOf(session), lastActiveSessionId = session.id)
  }

  private fun organization(name: String): Organization =
    Organization(
      id = "org_1",
      name = name,
      slug = null,
      imageUrl = "",
      maxAllowedMemberships = 5,
      adminDeleteEnabled = false,
      createdAt = 0L,
      updatedAt = 0L,
      publicMetadata = JsonObject(emptyMap()),
    )

  private companion object {
    val JSON = "application/json; charset=utf-8".toMediaType()
  }
}
