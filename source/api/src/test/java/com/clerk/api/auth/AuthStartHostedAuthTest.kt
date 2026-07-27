package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthStartHostedAuthTest {
  private lateinit var auth: Auth
  private val clientApi = mockk<ClientApi>()

  @Before
  fun setup() {
    auth = Auth()
    mockkObject(HostedAuthService)
    mockkObject(Clerk)
    mockkObject(ClerkApi)
    every { ClerkApi.client } returns clientApi
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `startHostedAuth activates the created session via setActive`() = runTest {
    val session = session().copy(lastActiveOrganizationId = "org_123")
    coEvery { HostedAuthService.start(any(), any()) } returns ClerkResult.success(session)
    every { Clerk.organizationSelectionIsForced } returns false
    every { Clerk.clientInitialized } returns false
    justRun { Clerk.updateClient(any()) }
    coEvery { clientApi.setActive(SESSION_ID, "org_123", any()) } returns
      ClerkResult.success(session)
    coEvery { clientApi.get() } returns
      ClerkResult.success(
        Client(id = "client_123", sessions = listOf(session), lastActiveSessionId = SESSION_ID)
      )

    val result = auth.startHostedAuth()

    assertTrue(result is ClerkResult.Success)
    assertEquals(SESSION_ID, (result as ClerkResult.Success).value.id)
    coVerify(exactly = 1) { clientApi.setActive(SESSION_ID, "org_123", any()) }
  }

  @Test
  fun `startHostedAuth succeeds under forced organization selection using the applied session`() =
    runTest {
      // With the redeemed client applied locally, the forced-organization-selection early return
      // in setActive resolves against the fresh Clerk.session instead of stale pre-auth state.
      val session = session()
      coEvery { HostedAuthService.start(any(), any()) } returns ClerkResult.success(session)
      every { Clerk.organizationSelectionIsForced } returns true
      every { Clerk.clientInitialized } returns false
      every { Clerk.session } returns session

      val result = auth.startHostedAuth()

      assertTrue(result is ClerkResult.Success)
      assertEquals(SESSION_ID, (result as ClerkResult.Success).value.id)
      coVerify(exactly = 0) { clientApi.setActive(any(), any(), any()) }
    }

  @Test
  fun `startHostedAuth propagates hosted auth failures without activation`() = runTest {
    val failure = ClerkResult.unknownFailure(Exception("Authentication cancelled"))
    coEvery { HostedAuthService.start(any(), any()) } returns failure

    val result = auth.startHostedAuth()

    assertTrue(result is ClerkResult.Failure)
    coVerify(exactly = 0) { clientApi.setActive(any(), any(), any()) }
  }

  private fun session(): Session =
    Session(
      id = SESSION_ID,
      expireAt = 10_000L,
      lastActiveAt = 1_000L,
      createdAt = 1_000L,
      updatedAt = 1_000L,
    )

  private companion object {
    const val SESSION_ID = "sess_123"
  }
}
