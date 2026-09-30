package com.clerk.api.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.Constants.Test.CONCURRENCY_TEST_THREAD_COUNT
import io.mockk.unmockkAll
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StorageHelperTest {
  private companion object {
    const val ENCRYPTED_VALUE_PREFIX = "clerk:v1:"
  }

  private lateinit var context: Context

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.storageCipherFactoryOverride = { TestStorageCipher() }
    StorageHelper.reset()
    preferences().edit { clear() }
  }

  @After
  fun tearDown() {
    unmockkAll()
    StorageHelper.reset()
    StorageHelper.storageCipherFactoryOverride = null
    preferences().edit { clear() }
  }

  private fun preferences(): SharedPreferences {
    return context.getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
  }

  private class TestStorageCipher : StorageCipher {
    override fun encrypt(plaintext: String): String {
      return Base64.encodeToString(plaintext.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    override fun decrypt(ciphertext: String): String {
      return String(Base64.decode(ciphertext, Base64.NO_WRAP), Charsets.UTF_8)
    }
  }

  @Test
  fun `initialize called concurrently from multiple threads initializes exactly once with no exceptions`() {
    val executor = Executors.newFixedThreadPool(CONCURRENCY_TEST_THREAD_COUNT)
    val latch = CountDownLatch(CONCURRENCY_TEST_THREAD_COUNT)
    val exceptions = mutableListOf<Throwable>()
    val initializedPreferences = mutableSetOf<SharedPreferences>()

    repeat(CONCURRENCY_TEST_THREAD_COUNT) {
      executor.submit {
        try {
          StorageHelper.initialize(context)
          val field = StorageHelper::class.java.getDeclaredField("secureStorage")
          field.isAccessible = true
          val prefs = field.get(StorageHelper) as? SharedPreferences
          requireNotNull(prefs) { "secureStorage should be initialized after initialize()" }
          synchronized(initializedPreferences) { initializedPreferences.add(prefs) }
          field.isAccessible = false
        } catch (e: Throwable) {
          synchronized(exceptions) { exceptions.add(e) }
        } finally {
          latch.countDown()
        }
      }
    }

    latch.await()
    executor.shutdown()

    assertTrue("No exceptions should occur", exceptions.isEmpty())
    assertEquals(
      "All threads should reference the same SharedPreferences instance",
      1,
      initializedPreferences.size,
    )
    StorageHelper.saveValue(StorageKey.DEVICE_ID, "test-value")
    assertEquals("test-value", StorageHelper.loadValue(StorageKey.DEVICE_ID))
  }

  @Test
  fun `saveValue with empty string does not write to SharedPreferences`() {
    StorageHelper.initialize(context)
    val testKey = StorageKey.DEVICE_ID
    val testValue = "initial-value"
    StorageHelper.saveValue(testKey, testValue)
    assertEquals("Initial value should be saved", testValue, StorageHelper.loadValue(testKey))

    StorageHelper.saveValue(testKey, "")

    assertEquals(
      "Empty string should not overwrite existing value",
      testValue,
      StorageHelper.loadValue(testKey),
    )
  }

  @Test
  fun `saveValue stores encrypted value in SharedPreferences`() {
    StorageHelper.initialize(context)

    StorageHelper.saveValue(StorageKey.DEVICE_ID, "plain-device-id")

    val storedValue = preferences().getString(StorageKey.DEVICE_ID.name, null)
    assertNotNull("Encrypted value should be written to SharedPreferences", storedValue)
    assertTrue(
      "Stored value should be envelope-prefixed",
      storedValue!!.startsWith(ENCRYPTED_VALUE_PREFIX),
    )
    assertTrue("Stored value should not equal plaintext", storedValue != "plain-device-id")
  }

  @Test
  fun `saveValue with empty string when no existing value does not write to SharedPreferences`() {
    StorageHelper.initialize(context)
    val testKey = StorageKey.DEVICE_TOKEN

    StorageHelper.saveValue(testKey, "")

    assertNull("Empty string should not be saved", StorageHelper.loadValue(testKey))
  }

  @Test
  fun `loadValue migrates legacy plaintext values to encrypted storage`() {
    preferences().edit(commit = true) { putString(StorageKey.DEVICE_TOKEN.name, "legacy-token") }
    StorageHelper.initialize(context)

    val loadedValue = StorageHelper.loadValue(StorageKey.DEVICE_TOKEN)
    val storedValue = preferences().getString(StorageKey.DEVICE_TOKEN.name, null)

    assertEquals("legacy-token", loadedValue)
    assertNotNull("Migrated value should still be stored", storedValue)
    assertTrue(
      "Legacy plaintext value should be rewritten in encrypted form",
      storedValue!!.startsWith(ENCRYPTED_VALUE_PREFIX),
    )
    assertTrue("Migrated value should no longer be plaintext", storedValue != "legacy-token")
  }

  @Test
  fun `loadValue deletes malformed encrypted values and returns null`() {
    StorageHelper.reset()
    StorageHelper.storageCipherFactoryOverride = {
      object : StorageCipher {
        override fun encrypt(plaintext: String): String = plaintext

        override fun decrypt(ciphertext: String): String {
          error("malformed ciphertext")
        }
      }
    }
    preferences().edit(commit = true) {
      putString(StorageKey.DEVICE_ID.name, "${ENCRYPTED_VALUE_PREFIX}opaque")
    }
    StorageHelper.initialize(context)

    val loadedValue = StorageHelper.loadValue(StorageKey.DEVICE_ID)

    assertNull("Malformed encrypted value should not be returned", loadedValue)
    assertFalse(
      "Malformed encrypted value should be removed from SharedPreferences",
      preferences().contains(StorageKey.DEVICE_ID.name),
    )
  }

  @Test
  fun `after initialize isInitialized returns true and values can be saved loaded and deleted`() {
    StorageHelper.initialize(context)
    val testKey = StorageKey.DEVICE_ID
    val testValue = "test-device-id-123"

    // Verify initialization state using reflection (since isInitialized is private)
    val field = StorageHelper::class.java.getDeclaredField("secureStorage")
    field.isAccessible = true
    val prefs = field.get(StorageHelper)
    field.isAccessible = false
    assertNotNull("secureStorage should be initialized", prefs)

    StorageHelper.saveValue(testKey, testValue)

    assertEquals("Value should be saved", testValue, StorageHelper.loadValue(testKey))

    val updatedValue = "updated-device-id-456"
    StorageHelper.saveValue(testKey, updatedValue)

    assertEquals("Value should be updated", updatedValue, StorageHelper.loadValue(testKey))

    StorageHelper.deleteValue(testKey)

    assertNull("Value should be deleted", StorageHelper.loadValue(testKey))
  }

  @Test
  fun `multiple save and load operations work correctly after initialization`() {
    StorageHelper.initialize(context)
    val deviceIdKey = StorageKey.DEVICE_ID
    val deviceTokenKey = StorageKey.DEVICE_TOKEN

    StorageHelper.saveValue(deviceIdKey, "device-id-1")
    StorageHelper.saveValue(deviceTokenKey, "device-token-1")

    assertEquals("device-id-1", StorageHelper.loadValue(deviceIdKey))
    assertEquals("device-token-1", StorageHelper.loadValue(deviceTokenKey))

    StorageHelper.saveValue(deviceIdKey, "device-id-2")

    assertEquals("device-id-2", StorageHelper.loadValue(deviceIdKey))
    assertEquals("device-token-1", StorageHelper.loadValue(deviceTokenKey))

    StorageHelper.deleteValue(deviceIdKey)

    assertNull(StorageHelper.loadValue(deviceIdKey))
    assertEquals("device-token-1", StorageHelper.loadValue(deviceTokenKey))
  }

  @Test
  fun `load save and delete never throw when racing with initialize`() {
    StorageHelper.reset()
    val executor = Executors.newFixedThreadPool(CONCURRENCY_TEST_THREAD_COUNT)
    val latch = CountDownLatch(CONCURRENCY_TEST_THREAD_COUNT)
    val exceptions = mutableListOf<Throwable>()

    repeat(CONCURRENCY_TEST_THREAD_COUNT) { index ->
      executor.submit {
        try {
          repeat(50) { iteration ->
            if ((index + iteration) % 3 == 0) {
              StorageHelper.initialize(context)
            } else {
              StorageHelper.saveValue(StorageKey.DEVICE_ID, "value-$index-$iteration")
              StorageHelper.loadValue(StorageKey.DEVICE_ID)
              StorageHelper.deleteValue(StorageKey.DEVICE_ID)
            }
          }
        } catch (e: Throwable) {
          synchronized(exceptions) { exceptions.add(e) }
        } finally {
          latch.countDown()
        }
      }
    }

    latch.await()
    executor.shutdown()

    assertTrue("No exceptions should occur", exceptions.isEmpty())
  }

  @Test
  fun `compareAndSetDeviceToken lets exactly one concurrent writer replace the expected token`() {
    // Given
    StorageHelper.initialize(context)
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token-initial")
    val executor = Executors.newFixedThreadPool(CONCURRENCY_TEST_THREAD_COUNT)
    val latch = CountDownLatch(CONCURRENCY_TEST_THREAD_COUNT)
    val winners = mutableListOf<String>()

    // When
    repeat(CONCURRENCY_TEST_THREAD_COUNT) { index ->
      executor.submit {
        try {
          val token = "token-$index"
          if (StorageHelper.compareAndSetDeviceToken(expected = "token-initial", value = token)) {
            synchronized(winners) { winners.add(token) }
          }
        } finally {
          latch.countDown()
        }
      }
    }

    latch.await()
    executor.shutdown()

    // Then
    assertEquals(1, winners.size)
    assertEquals(winners.single(), StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `compareAndSetDeviceToken notifies the change listener only when the token changes`() {
    // Given
    StorageHelper.initialize(context)
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token-a")
    val changes = mutableListOf<Pair<String?, String?>>()
    StorageHelper.valueChangeListener = { _, previous, value -> changes.add(previous to value) }

    // When
    StorageHelper.compareAndSetDeviceToken(expected = "token-stale", value = "token-x")
    StorageHelper.compareAndSetDeviceToken(expected = "token-a", value = "token-a")
    StorageHelper.compareAndSetDeviceToken(expected = "token-a", value = "token-b")
    StorageHelper.compareAndSetDeviceToken(expected = "token-b", value = null)

    // Then
    assertEquals(listOf("token-a" to "token-b", "token-b" to null), changes)
  }

  @Test
  fun `compareAndSetDeviceToken reports failure without side effects when the commit fails`() {
    // Given
    StorageHelper.initialize(context)
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token-a")
    val changes = mutableListOf<Pair<String?, String?>>()
    StorageHelper.valueChangeListener = { _, previous, value -> changes.add(previous to value) }
    var onChangedCalls = 0
    val restoreCommits = StorageHelper.failCommitsForTesting()

    // When
    val replaced =
      try {
        StorageHelper.compareAndSetDeviceToken(expected = "token-a", value = "token-b") {
          onChangedCalls += 1
        }
      } finally {
        restoreCommits()
      }

    // Then
    assertFalse(replaced)
    assertEquals(0, onChangedCalls)
    assertEquals(emptyList<Pair<String?, String?>>(), changes)
    assertEquals("token-a", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `compareAndSetDeviceToken reports failure without side effects when a delete commit fails`() {
    // Given
    StorageHelper.initialize(context)
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token-a")
    val changes = mutableListOf<Pair<String?, String?>>()
    StorageHelper.valueChangeListener = { _, previous, value -> changes.add(previous to value) }
    var onChangedCalls = 0
    val restoreCommits = StorageHelper.failCommitsForTesting()

    // When
    val cleared =
      try {
        StorageHelper.compareAndSetDeviceToken(expected = "token-a", value = null) {
          onChangedCalls += 1
        }
      } finally {
        restoreCommits()
      }

    // Then
    assertFalse(cleared)
    assertEquals(0, onChangedCalls)
    assertEquals(emptyList<Pair<String?, String?>>(), changes)
    assertEquals("token-a", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `compareAndSetDeviceToken treats a swap to the current token as a successful no-op`() {
    // Given
    StorageHelper.initialize(context)
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token-a")
    val changes = mutableListOf<Pair<String?, String?>>()
    StorageHelper.valueChangeListener = { _, previous, value -> changes.add(previous to value) }
    var onChangedCalls = 0

    // When
    val sameToken =
      StorageHelper.compareAndSetDeviceToken(expected = "token-a", value = "token-a") {
        onChangedCalls += 1
      }
    StorageHelper.deleteValue(StorageKey.DEVICE_TOKEN)
    changes.clear()
    val bothAbsent =
      StorageHelper.compareAndSetDeviceToken(expected = null, value = null) { onChangedCalls += 1 }

    // Then
    assertTrue(sameToken)
    assertTrue(bothAbsent)
    assertEquals(0, onChangedCalls)
    assertEquals(emptyList<Pair<String?, String?>>(), changes)
    assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }
}
