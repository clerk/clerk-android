package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.restorecredentials.RestoreCredentialStore
import com.clerk.api.restorecredentials.RestoreCredentials
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTokenFetcher
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AuthSignOutRestoreCredentialTest {

  private val restoreStateDirectory: File by lazy {
    Files.createTempDirectory("restore_state").toFile().apply { deleteOnExit() }
  }

  @Before
  fun setup() {
    Clerk.updateClient(Client())
    RestoreCredentialStore.directory = { restoreStateDirectory }
  }

  @After
  fun tearDown() {
    unmockkAll()
    RestoreCredentialStore.reset()
    RestoreCredentialStore.directory = { Clerk.applicationContext?.get()?.noBackupFilesDir }
    Clerk.updateClient(Client())
    SessionTokenFetcher.shared.reset()
    SessionTokenFetcher.shared.clearSignedOutSessions()
  }

  @Test
  fun `signOut with session ID keeps the restore credential of a user who stays signed in`() =
    runTest {
      val firstSession = testSession("sess_1", userId = "user_1")
      val secondSession = testSession("sess_2", userId = "user_2")
      stubSignOut(secondSession)
      RestoreCredentialStore.update { it.copy(deviceKeyUserId = "user_1") }
      Clerk.updateClient(
        Client(
          id = "client_123",
          sessions = listOf(firstSession, secondSession),
          lastActiveSessionId = firstSession.id,
        )
      )

      Auth().signOut(sessionId = secondSession.id)

      assertEquals(listOf(firstSession), Clerk.client.sessions)
      coVerify(exactly = 0) { RestoreCredentials.clearSilently() }
      assertEquals("user_1", RestoreCredentialStore.read().deviceKeyUserId)
    }

  @Test
  fun `signOut with session ID clears the restore credential of the user who signed out`() =
    runTest {
      val firstSession = testSession("sess_1", userId = "user_1")
      val secondSession = testSession("sess_2", userId = "user_2")
      stubSignOut(secondSession)
      RestoreCredentialStore.update { it.copy(deviceKeyUserId = "user_2") }
      Clerk.updateClient(
        Client(
          id = "client_123",
          sessions = listOf(firstSession, secondSession),
          lastActiveSessionId = firstSession.id,
        )
      )

      Auth().signOut(sessionId = secondSession.id)

      coVerify(exactly = 1) { RestoreCredentials.clearSilently() }
    }

  @Test
  fun `signOut with session ID clears the credential of a user whose session the response drops`() =
    runTest {
      val firstSession = testSession("sess_1", userId = "user_1")
      val secondSession = testSession("sess_2", userId = "user_2")
      stubSignOut(secondSession) {
        Clerk.updateClient(
          Client(
            id = "client_123",
            sessions = listOf(firstSession),
            lastActiveSessionId = firstSession.id,
          )
        )
      }
      RestoreCredentialStore.update { it.copy(deviceKeyUserId = "user_2") }
      Clerk.updateClient(
        Client(
          id = "client_123",
          sessions = listOf(firstSession, secondSession),
          lastActiveSessionId = firstSession.id,
        )
      )

      Auth().signOut(sessionId = secondSession.id)

      coVerify(exactly = 1) { RestoreCredentials.clearSilently() }
    }

  @Test
  fun `signOut with session ID clears a restore credential whose owner is unknown`() = runTest {
    val firstSession = testSession("sess_1", userId = "user_1")
    val secondSession = testSession("sess_2", userId = "user_2")
    stubSignOut(secondSession)
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    Auth().signOut(sessionId = secondSession.id)

    assertEquals(listOf(firstSession), Clerk.client.sessions)
    coVerify(exactly = 1) { RestoreCredentials.clearSilently() }
  }

  private fun stubSignOut(removedSession: Session, onRemove: () -> Unit = {}) {
    val sessionApi = mockk<SessionApi>()
    val clientApi = mockk<ClientApi>()
    mockkObject(ClerkApi)
    mockkObject(RestoreCredentials)
    every { ClerkApi.session } returns sessionApi
    every { ClerkApi.client } returns clientApi
    coEvery { RestoreCredentials.clearSilently() } returns Unit
    coEvery { sessionApi.removeSession(removedSession.id) } coAnswers
      {
        onRemove()
        ClerkResult.success(removedSession)
      }
    coEvery { clientApi.get(any()) } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))
  }

  private fun testSession(id: String, userId: String): Session =
    Session(
      id = id,
      status = Session.SessionStatus.ACTIVE,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      createdAt = 1_000,
      updatedAt = 1_000,
      user =
        User(
          hasImage = false,
          id = userId,
          imageUrl = "",
          passkeys = emptyList(),
          passwordEnabled = false,
          phoneNumbers = emptyList(),
          totpEnabled = false,
          twoFactorEnabled = false,
          updatedAt = 0,
        ),
    )
}
