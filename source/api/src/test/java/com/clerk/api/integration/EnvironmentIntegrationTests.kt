package com.clerk.api.integration

import com.clerk.api.Clerk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EnvironmentIntegrationTests {

  @Test
  fun `SDK initializes and fetches environment from real Clerk instance`() = runBlocking {
    val pk = requirePublishableKey()
    initializeClerkAndWait(pk)

    assertTrue("Clerk should be initialized", Clerk.isInitialized.value)
    assertNotNull(
      "applicationName should be set after environment is fetched",
      Clerk.applicationName,
    )
    assertTrue("applicationName should not be blank", Clerk.applicationName!!.isNotBlank())
  }
}
