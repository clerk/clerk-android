package com.clerk.configuration

import com.clerk.storage.StorageHelper
import com.clerk.storage.StorageKey
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Number of threads to use for concurrency testing */
private const val CONCURRENCY_TEST_THREAD_COUNT = 10

/** Expected number of times storage should be called during initialization */
private const val EXPECTED_STORAGE_LOAD_CALLS = 1

/** Expected number of times storage should be called when saving new device ID */
private const val EXPECTED_STORAGE_SAVE_CALLS = 1

/** Expected number of device IDs that should be saved during initialization */
private const val EXPECTED_SAVED_DEVICE_ID_COUNT = 1

@RunWith(RobolectricTestRunner::class)
class DeviceIdGeneratorTest {

  @Before
  fun setup() {
    // Mock StorageHelper to control its behavior
    mockkObject(StorageHelper)

    // Clear cached device ID to ensure clean state for each test
    DeviceIdGenerator.clearCache()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `initialize and getDeviceId returns existing device ID when one exists`() {
    // Given
    val existingDeviceId = "existing-device-id-123"
    every { StorageHelper.loadValue(StorageKey.DEVICE_ID) } returns existingDeviceId

    // When
    DeviceIdGenerator.initialize()
    val result = DeviceIdGenerator.getDeviceId()

    // Then
    assertEquals(existingDeviceId, result)
    verify(exactly = EXPECTED_STORAGE_LOAD_CALLS) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
    verify(exactly = 0) { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) }
  }

  @Test
  fun `initialize is thread safe and generates only one device ID`() {
    // Given - Simulate no existing device ID in storage
    every { StorageHelper.loadValue(StorageKey.DEVICE_ID) } returns null
    val savedDeviceIds = mutableListOf<String>()
    every { StorageHelper.saveValue(StorageKey.DEVICE_ID, capture(savedDeviceIds)) } returns Unit

    val executor = Executors.newFixedThreadPool(CONCURRENCY_TEST_THREAD_COUNT)
    val latch = CountDownLatch(CONCURRENCY_TEST_THREAD_COUNT)
    val results = mutableListOf<String>()

    // When - Execute multiple threads simultaneously
    repeat(CONCURRENCY_TEST_THREAD_COUNT) {
      executor.submit {
        try {
          DeviceIdGenerator.initialize()
          val deviceId = DeviceIdGenerator.getDeviceId()
          synchronized(results) { results.add(deviceId) }
        } finally {
          latch.countDown()
        }
      }
    }

    // Wait for all threads to complete
    latch.await()
    executor.shutdown()

    // Then
    assertEquals(
      "All threads should return a device ID",
      CONCURRENCY_TEST_THREAD_COUNT,
      results.size,
    )

    // All threads should return the same ID due to proper synchronization and caching
    val firstId = results.first()
    results.forEach { deviceId ->
      assertEquals("All threads should return the same device ID", firstId, deviceId)
    }

    // Verify it's a valid UUID format
    assertNotNull("Device ID should not be null", firstId)
    assertFalse("Device ID should not be empty", firstId.isEmpty())
    UUID.fromString(firstId) // Will throw if invalid

    // Verify that only one ID was saved
    assertEquals(
      "Only one device ID should be saved",
      EXPECTED_SAVED_DEVICE_ID_COUNT,
      savedDeviceIds.size,
    )
    assertEquals("Saved ID should match the generated ID", firstId, savedDeviceIds.first())
  }

  @Test
  fun `getDeviceId automatically initializes when not previously initialized`() {
    // Given - Simulate no existing device ID in storage but storage is working
    every { StorageHelper.loadValue(StorageKey.DEVICE_ID) } returns null
    every { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) } returns Unit

    // When - Call getDeviceId without explicit initialization
    val result = DeviceIdGenerator.getDeviceId()

    // Then - Should return a valid device ID
    assertNotNull("Device ID should not be null", result)
    assertFalse("Device ID should not be empty", result.isEmpty())
    UUID.fromString(result) // Will throw if invalid

    // Should have attempted to load and save to storage
    verify(exactly = EXPECTED_STORAGE_LOAD_CALLS) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
    verify(exactly = EXPECTED_STORAGE_SAVE_CALLS) {
      StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>())
    }
  }

  @Test
  fun `consecutive calls to getDeviceId return same ID with caching`() {
    // Given
    val existingDeviceId = "existing-device-id-789"
    every { StorageHelper.loadValue(StorageKey.DEVICE_ID) } returns existingDeviceId

    // When
    DeviceIdGenerator.initialize()
    val firstCall = DeviceIdGenerator.getDeviceId()
    val secondCall = DeviceIdGenerator.getDeviceId()
    val thirdCall = DeviceIdGenerator.getDeviceId()

    // Then
    assertEquals("All calls should return same device ID", firstCall, secondCall)
    assertEquals("All calls should return same device ID", secondCall, thirdCall)
    assertEquals("Should return existing device ID", existingDeviceId, firstCall)

    // With caching, storage should only be called once during initialization
    verify(exactly = EXPECTED_STORAGE_LOAD_CALLS) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
    verify(exactly = 0) { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) }
  }

  @Test
  fun `device IDs are cached and reused after initialization`() {
    // Given - Simulate no existing device ID initially
    every { StorageHelper.loadValue(StorageKey.DEVICE_ID) } returns null
    every { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) } returns Unit

    // When
    DeviceIdGenerator.initialize()
    val firstDeviceId = DeviceIdGenerator.getDeviceId()

    // Second call should return the same cached ID without hitting storage again
    val secondDeviceId = DeviceIdGenerator.getDeviceId()

    // Then
    assertNotNull("First device ID should not be null", firstDeviceId)
    assertNotNull("Second device ID should not be null", secondDeviceId)

    // Both should be valid UUIDs
    UUID.fromString(firstDeviceId)
    UUID.fromString(secondDeviceId)

    // Both calls should return the same ID due to caching
    assertEquals(
      "Both calls should return the same cached device ID",
      firstDeviceId,
      secondDeviceId,
    )

    // Storage should only be accessed once during initialization
    verify(exactly = EXPECTED_STORAGE_LOAD_CALLS) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
    verify(exactly = EXPECTED_STORAGE_SAVE_CALLS) {
      StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>())
    }
  }
}
