package com.clerk.api.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clerk.api.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnvironmentIntegrationTests {
  @Test
  fun fetchesEnvironmentThroughPackagedCore() = runBlocking {
    val key = requirePublishableKey()
    withTimeout(30_000) {
      withContext(Dispatchers.Main.immediate) {
        val clerk = connectForIntegrationTesting(key)
        try {
          check(clerk.loaded && !clerk.isInvalidated)
          check(clerk.session == null && clerk.user == null)
          val environment = clerk.environment.reload()
          check(!environment.isInvalidated)
          check(environment.displayConfig.applicationName.isNotBlank())
          check(environment.isDevelopmentOrStaging())
        } finally {
          clerk.close()
        }
      }
    }
  }
}
