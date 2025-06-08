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
    verify(exactly = 1) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
    verify(exactly = 0) { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) }
  }

  @Test
  fun `initialize is thread safe and generates only one device ID`() {
    // Given - Simulate no existing device ID in storage
    every { StorageHelper.loadValue(StorageKey.DEVICE_ID) } returns null
    val savedDeviceIds = mutableListOf<String>()
    every { StorageHelper.saveValue(StorageKey.DEVICE_ID, capture(savedDeviceIds)) } returns Unit

    val numberOfThreads = 10
    val executor = Executors.newFixedThreadPool(numberOfThreads)
    val latch = CountDownLatch(numberOfThreads)
    val results = mutableListOf<String>()

    // When - Execute multiple threads simultaneously
    repeat(numberOfThreads) {
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
    assertEquals("All threads should return a device ID", numberOfThreads, results.size)

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
    assertEquals("Only one device ID should be saved", 1, savedDeviceIds.size)
    assertEquals("Saved ID should match the generated ID", firstId, savedDeviceIds.first())
  }

  @Test
  fun `getDeviceId throws exception when not initialized`() {
    // Given - DeviceIdGenerator is not initialized

    // When & Then
    try {
      DeviceIdGenerator.getDeviceId()
      throw AssertionError("Expected IllegalStateException to be thrown")
    } catch (e: IllegalStateException) {
      assertEquals("DeviceIdGenerator not initialized", e.message)
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
    verify(exactly = 1) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
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
    verify(exactly = 1) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
    verify(exactly = 1) { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) }
  }
}
