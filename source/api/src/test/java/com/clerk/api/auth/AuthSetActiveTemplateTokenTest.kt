package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.api.SET_ACTIVE_INTENT_SELECT_ORG
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTokenFetcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthSetActiveTemplateTokenTest {
  private val session =
    Session(
      id = "sess_1",
      status = Session.SessionStatus.ACTIVE,
      expireAt = 4_000_000_000_000,
      lastActiveAt = 1_000,
      lastActiveOrganizationId = "org_a",
      createdAt = 1_000,
      updatedAt = 1_000,
    )
  private val clientApi = mockk<ClientApi>()
  private val sessionApi = mockk<SessionApi>()

  @Before
  fun setup() {
    SessionTokenFetcher.shared.reset()
    mockkObject(ClerkApi)
    every { ClerkApi.client } returns clientApi
    every { ClerkApi.session } returns sessionApi
    Clerk.updateClient(
      Client(id = "client_123", sessions = listOf(session), lastActiveSessionId = session.id)
    )
  }

  @After
  fun tearDown() {
    unmockkAll()
    SessionTokenFetcher.shared.reset()
    Clerk.updateClient(Client())
  }

  @Test
  fun `template token is fetched again after setActive switches organization`() = runTest {
    val orgAToken = templateToken(organizationId = "org_a")
    val orgBToken = templateToken(organizationId = "org_b")
    coEvery { sessionApi.tokens(session.id, "t") } returnsMany
      listOf(ClerkResult.success(orgAToken), ClerkResult.success(orgBToken))
    coEvery { clientApi.setActive(session.id, "org_b", SET_ACTIVE_INTENT_SELECT_ORG) } returns
      ClerkResult.success(session.copy(lastActiveOrganizationId = "org_b"))
    coEvery { clientApi.get() } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))

    val orgAResult = Auth().getToken(GetTokenOptions(template = "t"))
    Auth().setActive(sessionId = session.id, organizationId = "org_b")
    val orgBResult = Auth().getToken(GetTokenOptions(template = "t"))

    assertEquals(orgAToken.jwt, (orgAResult as ClerkResult.Success).value)
    assertEquals(orgBToken.jwt, (orgBResult as ClerkResult.Success).value)
    coVerify(exactly = 2) { sessionApi.tokens(session.id, "t") }
  }

  @Test
  fun `template token is served from cache while the organization is unchanged`() = runTest {
    val orgAToken = templateToken(organizationId = "org_a")
    coEvery { sessionApi.tokens(session.id, "t") } returns ClerkResult.success(orgAToken)

    Auth().getToken(GetTokenOptions(template = "t"))
    val cachedResult = Auth().getToken(GetTokenOptions(template = "t"))

    assertEquals(orgAToken.jwt, (cachedResult as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.tokens(session.id, "t") }
  }

  private fun templateToken(organizationId: String): TokenResource {
    val expiresAt = System.currentTimeMillis() / 1_000 + 3_600
    val header = base64Url("""{"alg":"none","typ":"JWT"}""")
    val payload =
      base64Url("""{"sid":"${session.id}","org_id":"$organizationId","exp":$expiresAt}""")
    return TokenResource("$header.$payload.signature")
  }

  private fun base64Url(value: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
}
