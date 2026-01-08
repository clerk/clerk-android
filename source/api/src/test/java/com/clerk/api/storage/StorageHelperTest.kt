package com.clerk.api.storage

import android.content.Context
import android.content.SharedPreferences
import com.clerk.api.Constants.Test.CONCURRENCY_TEST_THREAD_COUNT
import io.mockk.unmockkAll
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.After
import org.junit.Assert.assertEquals
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

  private lateinit var context: Context

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    // Clear any existing SharedPreferences data
    clearSharedPreferences()
  }

  @After
  fun tearDown() {
    unmockkAll()
    // Clear SharedPreferences after each test
    clearSharedPreferences()
  }

  /**
   * Clears SharedPreferences data. For tests that require uninitialized state, prefer
   * [StorageHelper.reset] with no context.
   */
  private fun clearSharedPreferences() {
    try {
      val field = StorageHelper::class.java.getDeclaredField("secureStorage")
      field.isAccessible = true
      val prefs = field.get(StorageHelper) as? SharedPreferences
      prefs?.edit()?.clear()?.commit()
      field.isAccessible = false
    } catch (_: Exception) {}
  }

  @Test
  fun `initialize called concurrently from multiple threads initializes exactly once with no exceptions`() {
    // Given
    val executor = Executors.newFixedThreadPool(CONCURRENCY_TEST_THREAD_COUNT)
    val latch = CountDownLatch(CONCURRENCY_TEST_THREAD_COUNT)
    val exceptions = mutableListOf<Throwable>()
    val initializedPreferences = mutableSetOf<SharedPreferences>()

    // When - Execute multiple threads calling initialize() simultaneously
    repeat(CONCURRENCY_TEST_THREAD_COUNT) {
      executor.submit {
        try {
          StorageHelper.initialize(context)
          // Capture the initialized SharedPreferences instance
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

    // Wait for all threads to complete
    latch.await()
    executor.shutdown()

    // Then
    assertTrue("No exceptions should occur", exceptions.isEmpty())
    assertEquals(
      "All threads should reference the same SharedPreferences instance",
      1,
      initializedPreferences.size,
    )
    // Verify storage is initialized and functional
    StorageHelper.saveValue(StorageKey.DEVICE_ID, "test-value")
    assertEquals("test-value", StorageHelper.loadValue(StorageKey.DEVICE_ID))
  }

  @Test
  fun `saveValue with empty string does not write to SharedPreferences`() {
    // Given
    StorageHelper.initialize(context)
    val testKey = StorageKey.DEVICE_ID
    val testValue = "initial-value"
    StorageHelper.saveValue(testKey, testValue)
    assertEquals("Initial value should be saved", testValue, StorageHelper.loadValue(testKey))

    // When - Save empty string
    StorageHelper.saveValue(testKey, "")

    // Then - Value should remain unchanged (empty strings are not saved)
    assertEquals(
      "Empty string should not overwrite existing value",
      testValue,
      StorageHelper.loadValue(testKey),
    )
  }

  @Test
  fun `saveValue with empty string when no existing value does not write to SharedPreferences`() {
    // Given
    StorageHelper.initialize(context)
    val testKey = StorageKey.DEVICE_TOKEN

    // When - Save empty string to a key with no existing value
    StorageHelper.saveValue(testKey, "")

    // Then - Value should remain null
    assertNull("Empty string should not be saved", StorageHelper.loadValue(testKey))
  }

  @Test
  fun `after initialize isInitialized returns true and values can be saved loaded and deleted`() {
    // Given
    StorageHelper.initialize(context)
    val testKey = StorageKey.DEVICE_ID
    val testValue = "test-device-id-123"

    // Verify initialization state using reflection (since isInitialized is private)
    val field = StorageHelper::class.java.getDeclaredField("secureStorage")
    field.isAccessible = true
    val prefs = field.get(StorageHelper)
    field.isAccessible = false
    assertNotNull("secureStorage should be initialized", prefs)

    // When - Save value
    StorageHelper.saveValue(testKey, testValue)

    // Then - Value should be saved
    assertEquals("Value should be saved", testValue, StorageHelper.loadValue(testKey))

    // When - Update value
    val updatedValue = "updated-device-id-456"
    StorageHelper.saveValue(testKey, updatedValue)

    // Then - Value should be updated
    assertEquals("Value should be updated", updatedValue, StorageHelper.loadValue(testKey))

    // When - Delete value
    StorageHelper.deleteValue(testKey)

    // Then - Value should be deleted
    assertNull("Value should be deleted", StorageHelper.loadValue(testKey))
  }

  @Test
  fun `multiple save and load operations work correctly after initialization`() {
    // Given
    StorageHelper.initialize(context)
    val deviceIdKey = StorageKey.DEVICE_ID
    val deviceTokenKey = StorageKey.DEVICE_TOKEN

    // When - Save multiple values
    StorageHelper.saveValue(deviceIdKey, "device-id-1")
    StorageHelper.saveValue(deviceTokenKey, "device-token-1")

    // Then - Both values should be retrievable
    assertEquals("device-id-1", StorageHelper.loadValue(deviceIdKey))
    assertEquals("device-token-1", StorageHelper.loadValue(deviceTokenKey))

    // When - Update one value
    StorageHelper.saveValue(deviceIdKey, "device-id-2")

    // Then - Updated value should be new, other value unchanged
    assertEquals("device-id-2", StorageHelper.loadValue(deviceIdKey))
    assertEquals("device-token-1", StorageHelper.loadValue(deviceTokenKey))

    // When - Delete one value
    StorageHelper.deleteValue(deviceIdKey)

    // Then - Deleted value should be null, other value unchanged
    assertNull(StorageHelper.loadValue(deviceIdKey))
    assertEquals("device-token-1", StorageHelper.loadValue(deviceTokenKey))
  }

  @Test
  fun `load save and delete never throw when racing with initialize`() {
    // Given
    StorageHelper.reset()
    val executor = Executors.newFixedThreadPool(CONCURRENCY_TEST_THREAD_COUNT)
    val latch = CountDownLatch(CONCURRENCY_TEST_THREAD_COUNT)
    val exceptions = mutableListOf<Throwable>()

    // When
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

    // Then
    assertTrue("No exceptions should occur", exceptions.isEmpty())
  }
}
