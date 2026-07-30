package com.clerk.api.session

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class SessionDeleteTest {
  private lateinit var sessionApi: SessionApi

  @Before
  fun setup() {
    sessionApi = mockk()
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `delete removes only the target session`() = runTest {
    val session = session("sess_target")
    val result = ClerkResult.success(session)
    coEvery { sessionApi.removeSession("sess_target") } returns result

    assertSame(result, session.delete())
    coVerify(exactly = 1) { sessionApi.removeSession("sess_target") }
    coVerify(exactly = 0) { sessionApi.deleteSessions() }
  }

  private fun session(id: String): Session =
    Session(
      id = id,
      expireAt = 0L,
      lastActiveAt = 0L,
      createdAt = 0L,
      updatedAt = 0L,
    )
}
