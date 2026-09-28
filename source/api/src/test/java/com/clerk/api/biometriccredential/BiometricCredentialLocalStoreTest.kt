package com.clerk.api.biometriccredential

import java.io.File
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BiometricCredentialLocalStoreTest {

  @get:Rule val temporaryFolder = TemporaryFolder()

  private val store = DefaultBiometricCredentialLocalStore
  private lateinit var fileStore: BiometricCredentialFileStore

  @Before
  fun setup() {
    fileStore = BiometricCredentialFileStore(File(temporaryFolder.root, "clerk"))
    BiometricCredentialStorage.fileStore = fileStore
  }

  @After
  fun tearDown() {
    BiometricCredentialStorage.fileStore = null
  }

  @Test
  fun `save and load round trips credentials`() {
    val credential = credential(id = "td_1", appIdentifier = "com.example.app")
    store.save(credential)

    assertEquals(listOf(credential), store.all())
    assertEquals(credential, store.credential("td_1"))
  }

  @Test
  fun `every policy is persisted explicitly`() {
    val passcode = credential(id = "td_passcode")
    val strict =
      credential(id = "td_strict").copy(policy = BiometricCredentialPolicy.BIOMETRY_CURRENT_SET)
    store.save(passcode)
    store.save(strict)

    val json = fileStore.dataFile.readText()
    assertTrue(json.contains("\"policy\":\"biometry_or_device_passcode\""))
    assertTrue(json.contains("\"policy\":\"biometry_current_set\""))
    assertEquals(listOf(passcode, strict), store.all())
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
  fun `delete all keeps the pending cleanup queue`() {
    store.save(credential(id = "td_1"))
    BiometricCredentialPendingCleanupStore.add("user_1")

    store.deleteAll()

    assertTrue(store.all().isEmpty())
    assertEquals(setOf("user_1"), BiometricCredentialPendingCleanupStore.all())
  }

  @Test
  fun `malformed store file is read as empty and replaced on the next write`() {
    fileStore.dataFile.parentFile?.mkdirs()
    fileStore.dataFile.writeText("not-json")

    assertTrue(store.all().isEmpty())

    val credential = credential(id = "td_1")
    store.save(credential)
    assertEquals(listOf(credential), store.all())
  }

  @Test
  fun `pending cleanup queue is sorted and de-duplicated`() {
    BiometricCredentialPendingCleanupStore.add("user_2")
    BiometricCredentialPendingCleanupStore.add("user_1")
    BiometricCredentialPendingCleanupStore.add("user_2")

    assertEquals(setOf("user_1", "user_2"), BiometricCredentialPendingCleanupStore.all())
    assertTrue(fileStore.dataFile.readText().contains("[\"user_1\",\"user_2\"]"))

    BiometricCredentialPendingCleanupStore.remove("user_1")
    BiometricCredentialPendingCleanupStore.remove("user_2")

    assertTrue(BiometricCredentialPendingCleanupStore.all().isEmpty())
  }

  @Test
  fun `writes fail when storage is not initialized`() {
    BiometricCredentialStorage.fileStore = null

    assertTrue(store.all().isEmpty())
    assertFailsWith<IllegalStateException> { store.save(credential(id = "td_1")) }
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
  ): BiometricCredentialLocalRecord {
    return BiometricCredentialLocalRecord(
      id = id,
      localKeyId = "tdlk_$id",
      userId = userId,
      appIdentifier = appIdentifier,
      identifierHintSha256 = BiometricCredentialLocalRecord.identifierHintSha256(identifierHint),
      policy = BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE,
      createdAt = 1L,
      updatedAt = 2L,
    )
  }
}
