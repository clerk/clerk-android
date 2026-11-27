package com.clerk.api.auth

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.middleware.ManualClientSyncRequest
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.restorecredentials.RestoreCredentials
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTokenFetcher
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AuthSignOutTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var mockSessionApi: SessionApi
  private lateinit var mockClientApi: ClientApi

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    context = RuntimeEnvironment.getApplication()
    StorageHelper.initialize(context)
    StorageHelper.reset(context)

    mockSessionApi = mockk()
    mockClientApi = mockk()

    mockkObject(ClerkApi)
    mockkObject(RestoreCredentials)
    every { ClerkApi.session } returns mockSessionApi
    every { ClerkApi.client } returns mockClientApi
    coEvery { RestoreCredentials.clearSilently() } just runs
    coEvery { RestoreCredentials.clearSilentlyAfterSessionSignOut(any()) } just runs
    Clerk.updateClient(Client())
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
    StorageHelper.reset(context)
    Clerk.updateClient(Client())
    SessionTokenFetcher.shared.reset()
    SessionTokenFetcher.shared.clearSignedOutSessions()
  }

  @Test
  fun `signOut with session ID drops device token and local client when single-session request fails`() =
    runTest {
      val session = testSession("sess_1")
      Clerk.updateClient(
        Client(id = "client_123", sessions = listOf(session), lastActiveSessionId = session.id)
      )
      StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "test_device_token")
      val failure =
        ClerkResult.httpFailure(code = 500, error = ClerkErrorResponse(errors = emptyList()))
      coEvery { mockSessionApi.deleteSessions(any()) } returns failure
      coEvery { mockSessionApi.removeSession(session.id) } returns failure
      coEvery { mockClientApi.getSkippingClientId(any(), any()) } returns failure

      val result = Auth().signOut(sessionId = session.id)

      assertSame(failure, result)
      assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
      assertNull(Clerk.client.id)
      assertTrue(Clerk.client.sessions.isEmpty())
      assertNull(Clerk.session)
      assertFalse(Clerk.isSignedIn)
      coVerify(exactly = 1) { mockSessionApi.deleteSessions(any()) }
      coVerify(exactly = 0) { mockSessionApi.removeSession(any()) }
    }

  @Test
  fun `signOut with session ID keeps device token and client when single-session request succeeds`() =
    runTest {
      val session = testSession("sess_1")
      Clerk.updateClient(
        Client(id = "client_123", sessions = listOf(session), lastActiveSessionId = session.id)
      )
      StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "test_device_token")
      coEvery { mockSessionApi.deleteSessions(any()) } coAnswers
        {
          firstArg<ManualClientSyncRequest>()
            .recordResponse(requestDeviceToken = "test_device_token", responseDeviceToken = null)
          ClerkResult.success(Client(id = "client_123"))
        }
      coEvery { mockSessionApi.removeSession(session.id) } returns ClerkResult.success(session)
      coEvery { mockClientApi.get(any()) } returns
        ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))
      coEvery { mockClientApi.getSkippingClientId(any(), any()) } returns
        ClerkResult.success(Client(id = "new_client_id"))

      val result = Auth().signOut(sessionId = session.id)

      assertTrue(result is ClerkResult.Success)
      assertEquals("test_device_token", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
      assertEquals("client_123", Clerk.client.id)
      assertNull(Clerk.session)
      assertFalse(Clerk.isSignedIn)
      coVerify(exactly = 1) { mockSessionApi.deleteSessions(any()) }
      coVerify(exactly = 0) { mockSessionApi.removeSession(any()) }
    }

  @Test
  fun `signOut with session ID drops device token and local client when pending-session request fails`() =
    runTest {
      val pendingSession = testSession("sess_pending", status = Session.SessionStatus.PENDING)
      Clerk.updateClient(
        Client(
          id = "client_123",
          sessions = listOf(pendingSession),
          lastActiveSessionId = pendingSession.id,
        )
      )
      StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "test_device_token")
      val failure =
        ClerkResult.httpFailure(code = 500, error = ClerkErrorResponse(errors = emptyList()))
      coEvery { mockSessionApi.deleteSessions(any()) } returns failure
      coEvery { mockSessionApi.removeSession(pendingSession.id) } returns failure
      coEvery { mockClientApi.getSkippingClientId(any(), any()) } returns failure

      val result = Auth().signOut(sessionId = pendingSession.id)

      assertSame(failure, result)
      assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
      assertNull(Clerk.client.id)
      assertNull(Clerk.session)
      assertFalse(Clerk.isSignedIn)
      coVerify(exactly = 1) { mockSessionApi.deleteSessions(any()) }
      coVerify(exactly = 0) { mockSessionApi.removeSession(any()) }
    }

  @Test
  fun `signOut with session ID keeps device token and both sessions when multi-session request fails`() =
    runTest {
      val firstSession = testSession("sess_1")
      val secondSession = testSession("sess_2")
      Clerk.updateClient(
        Client(
          id = "client_123",
          sessions = listOf(firstSession, secondSession),
          lastActiveSessionId = firstSession.id,
        )
      )
      StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "test_device_token")
      val failure =
        ClerkResult.httpFailure(code = 500, error = ClerkErrorResponse(errors = emptyList()))
      coEvery { mockSessionApi.removeSession(firstSession.id) } returns failure

      val result = Auth().signOut(sessionId = firstSession.id)

      assertSame(failure, result)
      assertEquals("test_device_token", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
      assertEquals("client_123", Clerk.client.id)
      assertEquals(listOf(firstSession, secondSession), Clerk.client.sessions)
      assertEquals(firstSession, Clerk.session)
      coVerify(exactly = 1) { mockSessionApi.removeSession(firstSession.id) }
      coVerify(exactly = 0) { mockSessionApi.deleteSessions(any()) }
    }

  @Test
  fun `signOut with session ID removes only that session when an active and a pending session are signed in`() =
    runTest {
      val activeSession = testSession("sess_active")
      val pendingSession = testSession("sess_pending", status = Session.SessionStatus.PENDING)
      Clerk.updateClient(
        Client(
          id = "client_123",
          sessions = listOf(activeSession, pendingSession),
          lastActiveSessionId = activeSession.id,
        )
      )
      StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "test_device_token")
      val failure =
        ClerkResult.httpFailure(code = 500, error = ClerkErrorResponse(errors = emptyList()))
      coEvery { mockSessionApi.deleteSessions(any()) } returns failure
      coEvery { mockSessionApi.removeSession(pendingSession.id) } returns failure
      coEvery { mockClientApi.getSkippingClientId(any(), any()) } returns failure

      val result = Auth().signOut(sessionId = pendingSession.id)

      assertSame(failure, result)
      assertEquals("test_device_token", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
      assertEquals(listOf(activeSession, pendingSession), Clerk.client.sessions)
      coVerify(exactly = 1) { mockSessionApi.removeSession(pendingSession.id) }
      coVerify(exactly = 0) { mockSessionApi.deleteSessions(any()) }
    }

  private fun testSession(
    id: String,
    status: Session.SessionStatus = Session.SessionStatus.ACTIVE,
  ): Session =
    Session(
      id = id,
      status = status,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      createdAt = 1_000,
      updatedAt = 1_000,
    )
}
