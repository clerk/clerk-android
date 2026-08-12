package com.clerk.api.trusteddevice

import android.util.Base64
import com.clerk.api.Clerk
import com.clerk.api.locale.LocaleProvider
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.storage.StorageCipher
import com.clerk.api.storage.StorageHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TrustedDevicesTest {

  private lateinit var previousCredentialStore: TrustedDeviceLocalCredentialStore
  private lateinit var previousKeyManager: TrustedDeviceKeyManager
  private lateinit var credentialStore: InMemoryCredentialStore
  private lateinit var keyManager: FakeKeyManager
  private lateinit var previousLocale: Locale

  @Before
  fun setUp() {
    previousCredentialStore = TrustedDevices.credentialStore
    previousKeyManager = TrustedDevices.keyManager
    previousLocale = Locale.getDefault()
    credentialStore = InMemoryCredentialStore()
    keyManager = FakeKeyManager()
    TrustedDevices.credentialStore = credentialStore
    TrustedDevices.keyManager = keyManager
    Clerk.applicationId = APP_IDENTIFIER
    StorageHelper.storageCipherFactoryOverride = { PassthroughStorageCipher() }
    StorageHelper.reset(RuntimeEnvironment.getApplication())
  }

  @After
  fun tearDown() {
    TrustedDevices.credentialStore = previousCredentialStore
    TrustedDevices.keyManager = previousKeyManager
    Clerk.applicationId = null
    Clerk.environment = null
    Clerk.updateClient(com.clerk.api.network.model.client.Client())
    Locale.setDefault(previousLocale)
    LocaleProvider.cleanup()
    StorageHelper.reset()
    StorageHelper.storageCipherFactoryOverride = null
    unmockkAll()
  }

  @Test
  fun `enrollment cleanup preserves credentials belonging to another user`() {
    credentialStore.credentials += credential(id = "td_keep", userId = "user_1")
    credentialStore.credentials += credential(id = "td_old", userId = "user_1")
    credentialStore.credentials += credential(id = "td_other", userId = "user_2")

    TrustedDevices.removeOtherLocalCredentialsForCurrentApp(
      userId = "user_1",
      keeping = trustedDevice(id = "td_keep"),
    )

    assertEquals(setOf("td_keep", "td_other"), credentialStore.credentials.map { it.id }.toSet())
    assertEquals(listOf("tdlk_td_old"), keyManager.deletedKeyIds)
  }

  @Test
  fun `failed account cleanup remains queued until retry succeeds`() {
    credentialStore.credentials += credential(id = "td_1", userId = "user_1")
    credentialStore.deleteFails = true

    assertFailsWith<IllegalStateException> {
      TrustedDevices.forgetLocalCredentialsAfterAccountDeletion("user_1")
    }
    assertEquals(setOf("user_1"), TrustedDevicePendingCleanupStore.all())

    credentialStore.deleteFails = false
    TrustedDevices.retryPendingLocalCredentialCleanup()

    assertTrue(TrustedDevicePendingCleanupStore.all().isEmpty())
    assertTrue(credentialStore.credentials.isEmpty())
  }

  @Test
  fun `failed key deletion retains credential and queued cleanup until retry succeeds`() {
    credentialStore.credentials += credential(id = "td_1", userId = "user_1")
    keyManager.deleteFailuresRemaining = 1

    assertFailsWith<IllegalStateException> {
      TrustedDevices.forgetLocalCredentialsAfterAccountDeletion("user_1")
    }
    assertEquals(setOf("user_1"), TrustedDevicePendingCleanupStore.all())
    assertEquals(listOf("td_1"), credentialStore.credentials.map { it.id })

    TrustedDevices.retryPendingLocalCredentialCleanup()

    assertTrue(TrustedDevicePendingCleanupStore.all().isEmpty())
    assertTrue(credentialStore.credentials.isEmpty())
    assertEquals(listOf("tdlk_td_1", "tdlk_td_1"), keyManager.deleteAttempts)
  }

  @Test
  fun `trusted device sign in includes the configured locale`() = runTest {
    credentialStore.credentials += credential(id = "td_1", userId = "user_1")
    Clerk.environment =
      mockk<Environment> {
        every { authConfig } returns
          AuthConfig(
            singleSessionMode = false,
            nativeSettings =
              AuthConfig.NativeSettings(apiEnabled = true, trustedDeviceSignInEnabled = true),
          )
      }
    Locale.setDefault(Locale.CANADA_FRENCH)
    LocaleProvider.initialize()
    val signInApi = mockk<SignInApi>()
    val createParams = slot<Map<String, String>>()
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    coEvery { signInApi.createSignIn(capture(createParams)) } returns
      ClerkResult.success(
        SignIn(
          id = "si_1",
          firstFactorVerification =
            Verification(
              trustedDeviceChallenge =
                TrustedDeviceChallenge(
                  challenge = "challenge",
                  challengeId = "challenge_1",
                  trustedDeviceId = "td_1",
                  clientData = "client-data",
                  expiresAt = 1_000,
                )
            ),
        )
      )
    coEvery { signInApi.attemptFirstFactor(any(), any()) } returns
      ClerkResult.success(SignIn(id = "si_1", status = SignIn.Status.COMPLETE))

    val result = TrustedDevices.signIn()

    assertTrue(result is ClerkResult.Success)
    assertEquals(LocaleProvider.locale.value.orEmpty(), createParams.captured["locale"])
  }

  private fun credential(id: String, userId: String) =
    TrustedDeviceLocalCredential(
      id = id,
      localKeyId = "tdlk_$id",
      userId = userId,
      appIdentifier = APP_IDENTIFIER,
      createdAt = 1,
      updatedAt = 1,
    )

  private fun trustedDevice(id: String) =
    TrustedDevice(id = id, appIdentifier = APP_IDENTIFIER, createdAt = 1, updatedAt = 1)

  private class InMemoryCredentialStore : TrustedDeviceLocalCredentialStore {
    val credentials = mutableListOf<TrustedDeviceLocalCredential>()
    var deleteFails = false

    override fun all(): List<TrustedDeviceLocalCredential> = credentials.toList()

    override fun save(credential: TrustedDeviceLocalCredential) {
      credentials.removeAll { it.id == credential.id }
      credentials += credential
    }

    override fun delete(id: String) {
      check(!deleteFails) { "delete failed" }
      credentials.removeAll { it.id == id }
    }

    override fun deleteAll() {
      credentials.clear()
    }
  }

  private class FakeKeyManager : TrustedDeviceKeyManager {
    val deletedKeyIds = mutableListOf<String>()
    val deleteAttempts = mutableListOf<String>()
    var deleteFailuresRemaining = 0

    override fun isSupported(policy: TrustedDevicePolicy): Boolean = true

    override fun createKey(policy: TrustedDevicePolicy): TrustedDeviceLocalKey = error("Not used")

    override suspend fun sign(
      clientData: String,
      localKeyId: String,
      policy: TrustedDevicePolicy,
      promptTitle: String,
      promptSubtitle: String?,
    ): TrustedDeviceKeySignature =
      TrustedDeviceKeySignature(clientData = clientData, signature = "signature")

    override fun hasKey(localKeyId: String): Boolean = true

    override fun deleteKey(localKeyId: String) {
      deleteAttempts += localKeyId
      if (deleteFailuresRemaining > 0) {
        deleteFailuresRemaining -= 1
        error("key deletion failed")
      }
      deletedKeyIds += localKeyId
    }
  }

  private class PassthroughStorageCipher : StorageCipher {
    override fun encrypt(plaintext: String): String =
      Base64.encodeToString(plaintext.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    override fun decrypt(encrypted: String): String =
      String(Base64.decode(encrypted, Base64.NO_WRAP), Charsets.UTF_8)
  }

  private companion object {
    const val APP_IDENTIFIER = "com.example.app"
  }
}
