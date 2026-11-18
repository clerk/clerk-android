package com.clerk.api.restorecredentials

import android.content.Context
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.exceptions.CreateCredentialUnknownException
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.Passkey
import com.clerk.api.session.Session
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RestoreCredentialOwnerTest {

  private lateinit var context: Context
  private lateinit var credentialManager: RestoreCredentialManager
  private lateinit var userApi: UserApi
  private val session =
    Session(
      id = "session_1",
      status = Session.SessionStatus.ACTIVE,
      expireAt = 0,
      lastActiveAt = 0,
      createdAt = 0,
      updatedAt = 0,
    )

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    credentialManager = mockk()
    userApi = mockk()

    Clerk.stateStore.reset()
    mockkObject(Clerk)
    every { Clerk.applicationContext } returns WeakReference(context)
    every { Clerk.passkeyFirstFactorIsEnabled } returns true
    every { Clerk.activeSession } returns session
    every { Clerk.session } returns session
    every { Clerk.user } returns mockk<User>(relaxed = true) { every { id } returns "user_2" }
    every { Clerk.locale } returns MutableStateFlow(null)
    every { Clerk.clientInitialized } returns false

    mockkObject(ClerkApi)
    every { ClerkApi.user } returns userApi
    coEvery { userApi.createPasskey(any()) } returns ClerkResult.success(preparedPasskey())
    coEvery { credentialManager.createCredential(eq(context), any()) } returns
      CreateRestoreCredentialResponse(REGISTRATION_RESPONSE_JSON)
    coEvery { userApi.attemptPasskeyVerification(any(), any(), any(), any()) } returns
      ClerkResult.success(preparedPasskey())
    coEvery { userApi.updatePasskey(any(), any(), any()) } returns
      ClerkResult.success(preparedPasskey())
    coEvery { userApi.deletePasskey(any(), any()) } returns ClerkResult.success(DeletedObject())

    RestoreCredentials.credentialManager = credentialManager
    RestoreCredentialStore.reset()
    RestoreCredentialStore.update { it.copy(deviceKeyUserId = "user_1") }
  }

  @After
  fun tearDown() {
    RestoreCredentialStore.reset()
    unmockkAll()
    Clerk.stateStore.reset()
    RestoreCredentials.credentialManager = RestoreCredentialManagerImpl()
  }

  @Test
  fun `cancellation while naming the passkey keeps the verified user as the owner`() = runTest {
    coEvery { userApi.updatePasskey(any(), any(), any()) } throws CancellationException("cancelled")

    assertCancels { RestoreCredentials.createIfMissing() }

    assertEquals("user_2", RestoreCredentialStore.read().deviceKeyUserId)
  }

  @Test
  fun `cancellation after the platform credential was replaced leaves the owner unknown`() =
    runTest {
      coEvery { userApi.attemptPasskeyVerification(any(), any(), any(), any()) } throws
        CancellationException("cancelled")

      assertCancels { RestoreCredentials.createIfMissing() }

      assertNull(RestoreCredentialStore.read().deviceKeyUserId)
    }

  @Test
  fun `cancellation while the platform credential is requested leaves the owner unknown`() =
    runTest {
      coEvery { credentialManager.createCredential(eq(context), any()) } throws
        CancellationException("cancelled")

      assertCancels { RestoreCredentials.createIfMissing() }

      assertNull(RestoreCredentialStore.read().deviceKeyUserId)
    }

  @Test
  fun `a platform credential request that fails restores the previous owner`() = runTest {
    coEvery { credentialManager.createCredential(eq(context), any()) } throws
      CreateCredentialUnknownException("failed")

    val result = RestoreCredentials.createIfMissing()

    assertTrue(result is ClerkResult.Failure)
    assertEquals("user_1", RestoreCredentialStore.read().deviceKeyUserId)
  }

  @Test
  fun `cancellation before the platform credential is requested restores the previous owner`() =
    runTest {
      coEvery { userApi.createPasskey(any()) } throws CancellationException("cancelled")

      assertCancels { RestoreCredentials.createIfMissing() }

      assertEquals("user_1", RestoreCredentialStore.read().deviceKeyUserId)
    }

  @Test
  fun `the owner is unknown while a creation is pending and the new user only once verified`() =
    runTest {
      var ownerWhilePending: String? = "unset"
      coEvery { userApi.createPasskey(any()) } coAnswers
        {
          ownerWhilePending = RestoreCredentialStore.read().deviceKeyUserId
          ClerkResult.success(preparedPasskey())
        }

      val result = RestoreCredentials.createIfMissing()

      assertTrue(result is ClerkResult.Success)
      assertNull(ownerWhilePending)
      assertEquals("user_2", RestoreCredentialStore.read().deviceKeyUserId)
    }

  @Test
  fun `create records the owner the way createIfMissing does`() = runTest {
    var ownerWhilePending: String? = "unset"
    coEvery { userApi.createPasskey(any()) } coAnswers
      {
        ownerWhilePending = RestoreCredentialStore.read().deviceKeyUserId
        ClerkResult.success(preparedPasskey())
      }

    val result = RestoreCredentials.create()

    assertTrue(result is ClerkResult.Success)
    assertNull(ownerWhilePending)
    assertEquals("user_2", RestoreCredentialStore.read().deviceKeyUserId)
  }

  @Test
  fun `cancelling create after the platform credential was replaced leaves the owner unknown`() =
    runTest {
      coEvery { userApi.attemptPasskeyVerification(any(), any(), any(), any()) } throws
        CancellationException("cancelled")

      assertCancels { RestoreCredentials.create() }

      assertNull(RestoreCredentialStore.read().deviceKeyUserId)
    }

  @Test
  fun `signing out during create after the platform credential was replaced clears it`() =
    runTest {
      every { Clerk.clientOrNull } returns Client(id = "client_1", sessions = listOf(session))
      coEvery { userApi.attemptPasskeyVerification(any(), any(), any(), any()) } coAnswers
        {
          awaitCancellation()
        }
      coEvery { credentialManager.clearCredentialState(eq(context), any()) } returns Unit

      val creation = async { RestoreCredentials.create() }
      runCurrent()
      RestoreCredentials.clearSilentlyAfterSessionSignOut("user_2")

      assertTrue(creation.await() is ClerkResult.Failure)
      assertNull(RestoreCredentialStore.read().deviceKeyUserId)
      coVerify(exactly = 1) { credentialManager.clearCredentialState(eq(context), any()) }
    }

  @Test
  fun `signing out during create before the platform credential is requested keeps the owner's`() =
    runTest {
      every { Clerk.clientOrNull } returns Client(id = "client_1", sessions = listOf(session))
      coEvery { userApi.createPasskey(any()) } coAnswers { awaitCancellation() }
      coEvery { credentialManager.clearCredentialState(eq(context), any()) } returns Unit

      val creation = async { RestoreCredentials.create() }
      runCurrent()
      RestoreCredentials.clearSilentlyAfterSessionSignOut("user_2")

      assertTrue(creation.await() is ClerkResult.Failure)
      assertEquals("user_1", RestoreCredentialStore.read().deviceKeyUserId)
      coVerify(exactly = 0) { credentialManager.clearCredentialState(any(), any()) }
    }

  private suspend fun assertCancels(block: suspend () -> ClerkResult<*, *>): CancellationException {
    val result =
      try {
        block()
      } catch (e: CancellationException) {
        return e
      }
    throw AssertionError("Expected CancellationException, got $result")
  }

  private fun preparedPasskey(): Passkey =
    Passkey(
      id = "passkey_1",
      name = "Restore credential",
      verification = Verification(nonce = REGISTRATION_REQUEST_JSON),
      createdAt = 0,
      updatedAt = 0,
    )

  private companion object {
    const val REGISTRATION_REQUEST_JSON = """{"challenge":"challenge","user":{"id":"dXNlcl8x"}}"""
    const val REGISTRATION_RESPONSE_JSON =
      """{"id":"credential_1","type":"public-key","response":{}}"""
  }
}
