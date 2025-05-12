package com.clerk.sdk

import android.content.Context
import com.clerk.sdk.error.ClerkClientError
import com.clerk.sdk.model.environment.InstanceEnvironmentType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

private const val PUBLISHABLE_TEST_KEY = "pk_test_dmFsaWQtY2hhbW9pcy04Ni5jbGVyay5hY2NvdW50cy5kZXYk"
private const val PUBLISHABLE_LIVE_KEY = "pk_live_dmFsaWQtY2hhbW9pcy04Ni5jbGVyay5hY2NvdW50cy5kZXYk"

@RunWith(RobolectricTestRunner::class)
internal class ClerkTest {

  private lateinit var mockContext: Context

  @Before
  fun setUp() {
    mockContext = Mockito.mock(Context::class.java)
    Clerk.client = null
    Clerk.context = null
  }

  @Test(expected = ClerkClientError::class)
  fun `initialize should throw error when publishable key is empty`() {
    Clerk.initialize(mockContext, "")
  }

  @Test
  fun `initialize should correctly set publishable key and context`() {
    Clerk.initialize(mockContext, PUBLISHABLE_TEST_KEY, debugMode = true)

    Assert.assertEquals(PUBLISHABLE_TEST_KEY, Clerk.publishableKey)
    Assert.assertEquals(true, Clerk.debugMode)
    Assert.assertNotNull(Clerk.context?.get())
  }

  @Test
  fun `frontendApiUrl should be extracted from publishable key`() {

    Clerk.initialize(mockContext, PUBLISHABLE_TEST_KEY)

    Assert.assertEquals(Clerk.frontendApiUrl, "https://valid-chamois-86.clerk.accounts.dev")
  }

  @Test
  fun `instanceType should return DEVELOPMENT for test key`() {
    Clerk.initialize(mockContext, PUBLISHABLE_TEST_KEY)

    Assert.assertEquals(InstanceEnvironmentType.DEVELOPMENT, Clerk.instanceType)
  }

  @Test
  fun `instanceType should return PRODUCTION for live key`() {
    Clerk.initialize(mockContext, PUBLISHABLE_LIVE_KEY)

    Assert.assertEquals(InstanceEnvironmentType.PRODUCTION, Clerk.instanceType)
  }
}
