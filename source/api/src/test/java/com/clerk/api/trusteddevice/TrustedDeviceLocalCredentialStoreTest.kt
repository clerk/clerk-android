package com.clerk.api.trusteddevice

import android.util.Base64
import com.clerk.api.storage.StorageCipher
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TrustedDeviceLocalCredentialStoreTest {

  private val store = DefaultTrustedDeviceLocalCredentialStore

  @Before
  fun setup() {
    StorageHelper.storageCipherFactoryOverride = { PassthroughStorageCipher() }
    StorageHelper.reset(RuntimeEnvironment.getApplication())
    store.deleteAll()
  }

  @After
  fun tearDown() {
    store.deleteAll()
    StorageHelper.reset()
    StorageHelper.storageCipherFactoryOverride = null
  }

  @Test
  fun `save and load round trips credentials`() {
    val credential = credential(id = "td_1", appIdentifier = "com.example.app")
    store.save(credential)

    assertEquals(listOf(credential), store.all())
    assertEquals(credential, store.credential("td_1"))
  }

  @Test
  fun `save replaces an existing credential with the same id`() {
    store.save(credential(id = "td_1", userId = "user_1"))
    store.save(credential(id = "td_1", userId = "user_2"))

    assertEquals(1, store.all().size)
    assertEquals("user_2", store.credential("td_1")?.userId)
  }

  @Test
  fun `all filters by app identifier`() {
    store.save(credential(id = "td_1", appIdentifier = "com.example.app"))
    store.save(credential(id = "td_2", appIdentifier = "com.other.app"))

    assertEquals(listOf("td_1"), store.all("com.example.app").map { it.id })
  }

  @Test
  fun `delete removes only the matching credential`() {
    store.save(credential(id = "td_1"))
    store.save(credential(id = "td_2"))

    store.delete("td_1")

    assertNull(store.credential("td_1"))
    assertEquals(listOf("td_2"), store.all().map { it.id })
  }

  @Test
  fun `malformed storage payload is cleared instead of crashing`() {
    StorageHelper.saveValue(StorageKey.TRUSTED_DEVICE_CREDENTIALS, "not-json")

    assertTrue(store.all().isEmpty())
    assertNull(StorageHelper.loadValue(StorageKey.TRUSTED_DEVICE_CREDENTIALS))
  }

  @Test
  fun `malformed record is dropped while valid records load`() {
    val valid = credential(id = "td_1")
    store.save(valid)
    val storedJson = StorageHelper.loadValue(StorageKey.TRUSTED_DEVICE_CREDENTIALS)!!
    val withMalformed = storedJson.removeSuffix("]") + "," + """{"id":"td_bad"}]"""
    StorageHelper.saveValue(StorageKey.TRUSTED_DEVICE_CREDENTIALS, withMalformed)

    assertEquals(listOf(valid), store.all())
  }

  @Test
  fun `matches ignores case and whitespace in identifier hints`() {
    val credential = credential(id = "td_1", identifierHint = "user@example.com")

    assertTrue(credential.matches("  USER@example.com "))
    assertTrue(credential.matches(null))
    assertTrue(credential.matches(""))
    assertEquals(false, credential.matches("other@example.com"))
  }

  private fun credential(
    id: String,
    userId: String = "user_1",
    appIdentifier: String = "com.example.app",
    identifierHint: String? = null,
  ): TrustedDeviceLocalCredential {
    return TrustedDeviceLocalCredential(
      id = id,
      localKeyId = "tdlk_$id",
      userId = userId,
      appIdentifier = appIdentifier,
      identifierHint = TrustedDeviceLocalCredential.normalizedIdentifierHint(identifierHint),
      policy = TrustedDevicePolicy.BIOMETRY_OR_DEVICE_PASSCODE,
      createdAt = 1L,
      updatedAt = 2L,
    )
  }

  private class PassthroughStorageCipher : StorageCipher {
    override fun encrypt(plaintext: String): String {
      return Base64.encodeToString(plaintext.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    override fun decrypt(encrypted: String): String {
      return String(Base64.decode(encrypted, Base64.NO_WRAP), Charsets.UTF_8)
    }
  }
}
