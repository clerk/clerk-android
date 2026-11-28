package com.clerk.api.session

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionTokenFetcherDeviceBindingTest {
  private val session = mockk<Session>(relaxed = true)
  private val sessionApi = mockk<SessionApi>(relaxed = true)
  private val sessionTokenFetcher = SessionTokenFetcher(mockk(relaxed = true))

  @Before
  fun setUp() {
    every { session.id } returns "session_123"
    every { session.status } returns Session.SessionStatus.ACTIVE
    mockkObject(ClerkApi, Clerk)
    every { ClerkApi.session } returns sessionApi
    every { Clerk.session } returns session
    every { Clerk.clearSessionAndUserState() } returns Unit
    every { Clerk.environment } returns null
    every { Clerk.clientFlow } returns MutableStateFlow(null)
    SessionTokensCache.clear()
  }

  @After
  fun tearDown() {
    unmockkAll()
    SessionTokensCache.clear()
  }

  @Test
  fun `a token bound to another device key clears local session state`() = runTest {
    for (code in listOf("device_binding_key_mismatch", "device_binding_proof_missing")) {
      rejectTokenRequestWith(code)

      assertNull(sessionTokenFetcher.getToken(session))
    }

    verify(exactly = 2) { Clerk.clearSessionAndUserState() }
  }

  @Test
  fun `a proof rejected as invalid or replayed keeps local session state`() = runTest {
    for (code in listOf("device_binding_proof_invalid", "device_binding_proof_replayed")) {
      rejectTokenRequestWith(code)

      assertNull(sessionTokenFetcher.getToken(session))
    }

    verify(exactly = 0) { Clerk.clearSessionAndUserState() }
  }

  private fun rejectTokenRequestWith(code: String) {
    coEvery { sessionApi.tokens("session_123") } returns
      ClerkResult.httpFailure(
        code = 401,
        error = ClerkErrorResponse(errors = listOf(Error(code = code))),
      )
  }
}
