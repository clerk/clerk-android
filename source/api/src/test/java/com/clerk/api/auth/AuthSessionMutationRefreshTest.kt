package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.api.SET_ACTIVE_INTENT_SELECT_ORG
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.middleware.ManualClientSyncRequest
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.restorecredentials.RestoreCredentials
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTokenFetcher
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthSessionMutationRefreshTest {

  @Before
  fun setup() {
    Clerk.updateClient(Client())
  }

  @After
  fun tearDown() {
    unmockkAll()
    Clerk.updateClient(Client())
    SessionTokenFetcher.shared.reset()
    SessionTokenFetcher.shared.clearSignedOutSessions()
  }

  @Test
  fun `signOut with session ID does not apply a refresh whose device token went stale`() = runTest {
    val firstSession = testSession("sess_1")
    val secondSession = testSession("sess_2")
    val sessionApi = mockk<SessionApi>()
    val clientApi = mockk<ClientApi>()
    mockkObject(ClerkApi)
    mockkObject(RestoreCredentials)
    every { ClerkApi.session } returns sessionApi
    every { ClerkApi.client } returns clientApi
    coEvery { RestoreCredentials.clearSilently() } returns Unit
    coEvery { sessionApi.removeSession(firstSession.id) } returns ClerkResult.success(firstSession)
    coEvery { clientApi.get(any()) } coAnswers
      {
        arg<ManualClientSyncRequest?>(0)
          ?.recordResponse(
            requestDeviceToken = "device_token_before_switch",
            responseDeviceToken = "device_token_before_switch",
          )
        ClerkResult.success(Client(id = "client_fetched_with_old_token"))
      }
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    Auth().signOut(sessionId = firstSession.id)

    assertEquals("client_123", Clerk.client.id)
    assertEquals(listOf(secondSession), Clerk.client.sessions)
  }

  @Test
  fun `setActive keeps active session when follow-up client refresh omits it`() = runTest {
    val firstSession = testSession("sess_1")
    val secondSession = testSession("sess_2", lastActiveOrganizationId = "org_2")
    val clientApi = mockk<ClientApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.client } returns clientApi
    coEvery { clientApi.setActive(secondSession.id, "org_2", SET_ACTIVE_INTENT_SELECT_ORG) } returns
      ClerkResult.success(secondSession.copy(lastActiveOrganizationId = null))
    coEvery { clientApi.get(any()) } coAnswers
      {
        firstArg<ManualClientSyncRequest?>()?.recordResponse(null, null)
        ClerkResult.success(Client(id = "client_123", lastAuthenticationStrategy = "oauth_google"))
      }
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    val result = Auth().setActive(sessionId = secondSession.id, organizationId = "org_2")

    assertTrue(result is ClerkResult.Success)
    assertEquals(listOf(firstSession, secondSession), Clerk.client.sessions)
    assertEquals(secondSession.id, Clerk.client.lastActiveSessionId)
    assertEquals(secondSession, Clerk.sessionFlow.value)
    assertEquals("org_2", Clerk.sessionFlow.value?.lastActiveOrganizationId)
    assertEquals("oauth_google", Clerk.client.lastAuthenticationStrategy)
  }

  @Test
  fun `setActive keeps active session when follow-up refresh returns stale lastActiveSessionId`() =
    runTest {
      val firstSession = testSession("sess_1")
      val secondSession = testSession("sess_2")
      val clientApi = mockk<ClientApi>()
      mockkObject(ClerkApi)
      every { ClerkApi.client } returns clientApi
      coEvery { clientApi.setActive(secondSession.id, "", SET_ACTIVE_INTENT_SELECT_ORG) } returns
        ClerkResult.success(secondSession)
      // Read replica returns a fully-hydrated client but with the previous
      // lastActiveSessionId — read-after-write lag.
      coEvery { clientApi.get(any()) } coAnswers
        {
          firstArg<ManualClientSyncRequest?>()?.recordResponse(null, null)
          ClerkResult.success(
            Client(
              id = "client_123",
              sessions = listOf(firstSession, secondSession),
              lastActiveSessionId = firstSession.id,
              lastAuthenticationStrategy = "oauth_google",
            )
          )
        }
      Clerk.updateClient(
        Client(
          id = "client_123",
          sessions = listOf(firstSession, secondSession),
          lastActiveSessionId = firstSession.id,
        )
      )

      val result = Auth().setActive(sessionId = secondSession.id)

      assertTrue(result is ClerkResult.Success)
      assertEquals(listOf(firstSession, secondSession), Clerk.client.sessions)
      assertEquals(secondSession.id, Clerk.client.lastActiveSessionId)
      assertEquals(secondSession, Clerk.sessionFlow.value)
      assertEquals("oauth_google", Clerk.client.lastAuthenticationStrategy)
    }

  private fun testSession(id: String, lastActiveOrganizationId: String? = null): Session =
    Session(
      id = id,
      status = Session.SessionStatus.ACTIVE,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      lastActiveOrganizationId = lastActiveOrganizationId,
      createdAt = 1_000,
      updatedAt = 1_000,
    )
}
