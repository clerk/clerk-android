package com.clerk.configuration

/** Number of threads to use for concurrency testing */
import com.clerk.Constants.Test.CONCURRENCY_TEST_THREAD_COUNT
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
    // Only one read; never a save
    verify(exactly = 1) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
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
    val firstId = results.first()
    results.forEach { deviceId ->
      assertEquals("All threads should return the same device ID", firstId, deviceId)
    }
    assertNotNull("Device ID should not be null", firstId)
    assertFalse("Device ID should not be empty", firstId.isEmpty())
    UUID.fromString(firstId) // Will throw if invalid
    assertEquals("Only one device ID should be saved", 1, savedDeviceIds.size)
    assertEquals("Saved ID should match the generated ID", firstId, savedDeviceIds.first())
  }

  @Test
  fun `getDeviceId automatically initializes when not previously initialized`() {
    every { StorageHelper.loadValue(StorageKey.DEVICE_ID) } returns null
    every { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) } returns Unit

    val result = DeviceIdGenerator.getDeviceId()

    assertNotNull("Device ID should not be null", result)
    assertFalse("Device ID should not be empty", result.isEmpty())
    UUID.fromString(result) // Will throw if invalid
    verify(exactly = 1) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
    verify(exactly = 1) { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) }
  }

  @Test
  fun `consecutive calls to getDeviceId return same ID with caching`() {
    val existingDeviceId = "existing-device-id-789"
    every { StorageHelper.loadValue(StorageKey.DEVICE_ID) } returns existingDeviceId

    DeviceIdGenerator.initialize()
    val firstCall = DeviceIdGenerator.getDeviceId()
    val secondCall = DeviceIdGenerator.getDeviceId()
    val thirdCall = DeviceIdGenerator.getDeviceId()
    assertEquals("All calls should return same device ID", firstCall, secondCall)
    assertEquals("All calls should return same device ID", secondCall, thirdCall)
    assertEquals("Should return existing device ID", existingDeviceId, firstCall)
    verify(exactly = 1) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
    verify(exactly = 0) { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) }
  }

  @Test
  fun `device IDs are cached and reused after initialization`() {
    every { StorageHelper.loadValue(StorageKey.DEVICE_ID) } returns null
    every { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) } returns Unit
    DeviceIdGenerator.initialize()
    val firstDeviceId = DeviceIdGenerator.getDeviceId()
    val secondDeviceId = DeviceIdGenerator.getDeviceId()
    assertNotNull("First device ID should not be null", firstDeviceId)
    assertNotNull("Second device ID should not be null", secondDeviceId)
    UUID.fromString(firstDeviceId)
    UUID.fromString(secondDeviceId)
    assertEquals(
      "Both calls should return the same cached device ID",
      firstDeviceId,
      secondDeviceId,
    )
    verify(exactly = 1) { StorageHelper.loadValue(StorageKey.DEVICE_ID) }
    verify(exactly = 1) { StorageHelper.saveValue(StorageKey.DEVICE_ID, any<String>()) }
  }
}
