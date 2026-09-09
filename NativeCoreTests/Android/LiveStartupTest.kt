package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in service smoke test: GET only, no persistent credentials or OS presenters. */
@RunWith(AndroidJUnit4::class)
class LiveStartupTest {
  private class MemoryStorage : CredentialStorage {
    private var value: String? = null
    override suspend fun read(): String? = value
    override suspend fun write(value: String) { this.value = value }
    override suspend fun remove() { value = null }
  }

  private class ReadOnlyCapabilities(configuration: ClerkConfiguration) : NativeCapabilities {
    private val delegate = AndroidCapabilities(
      configuration.publishableKey, configuration.frontendAPI, MemoryStorage(),
      authStorage = MemoryStorage(),
    )
    override val supported get() = delegate.supported
    var reads = 0
      private set
    override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
      if (capability == "http") {
        val args = arguments.jsonObject
        val method = args.getValue("method").requireString()
        val path = URI(args.getValue("url").requireString()).path
        if (method != "GET" || path !in setOf("/v1/environment", "/v1/client")) {
          throw CoreException("live_proof_write_forbidden")
        }
        reads++
      }
      return delegate.perform(capability, arguments)
    }
  }

  @Test fun startsPackagedCoreAgainstDevelopmentServiceWhenRequested() = runBlocking {
    val key = InstrumentationRegistry.getArguments().getString("clerkLivePublishableKey")
    assumeTrue(key != null)
    check(key!!.startsWith("pk_test_"))
    withTimeout(30000) {
      withContext(Dispatchers.Main.immediate) {
        val configuration = ClerkConfiguration(key, "clerk-live-proof://oauth/callback")
        val capabilities = ReadOnlyCapabilities(configuration)
        val clerk = Clerk.connect(InstrumentationRegistry.getInstrumentation().targetContext, configuration, capabilities)
        try {
          check(clerk.loaded && !clerk.isInvalidated)
          check(clerk.user == null && clerk.session == null)
          check(capabilities.reads >= 2)
        } finally { clerk.close() }
      }
    }
  }
}
