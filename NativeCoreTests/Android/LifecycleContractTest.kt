package com.clerk.api

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.lang.ref.WeakReference
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifecycleContractTest {
  private class Owner : LifecycleOwner {
    override val lifecycle = LifecycleRegistry(this)
  }

  private suspend fun connect(capabilities: NativeCapabilities): Clerk {
    val key =
      "pk_test_" +
        Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
    return Clerk.connect(
      InstrumentationRegistry.getInstrumentation().targetContext,
      ClerkConfiguration(key, "clerk-test://sso-callback"),
      capabilities,
    )
  }

  private suspend fun eventually(condition: () -> Boolean) =
    withTimeout(3000) {
      while (!condition()) delay(5)
    }

  // Keep the weak-reference read off the coroutine frame that requests collection.
  private fun isCollected(reference: WeakReference<CoreRuntime>) = reference.get() == null

  private suspend fun abandonOwner(
    fixtures: NativeCapabilities,
    owner: Owner,
    stopped: () -> Unit,
  ): WeakReference<CoreRuntime> {
    val key =
      "pk_test_" +
        Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
    val clerk =
      connectCore(
        InstrumentationRegistry.getInstrumentation().targetContext,
        ClerkConfiguration(key, "clerk-test://sso-callback"),
        fixtures,
      ) { runtime ->
        observeNetworkConnectivity(runtime) { stopped }
      }
    val runtime = clerk.context.requireRuntime()
    observeApplicationLifecycle(runtime, owner.lifecycle)
    return WeakReference(runtime)
  }

  @Test
  fun releasingTheLastOwnerUnsubscribesLifecycleAndConnectivity() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val fixtures = PackagedFixtures(InstrumentationRegistry.getInstrumentation().context)
      val owner = Owner()
      owner.lifecycle.currentState = Lifecycle.State.CREATED
      var stopped = false
      val reference = abandonOwner(fixtures, owner) { stopped = true }
      try {
        withTimeout(5000) {
          while (!isCollected(reference)) {
            System.gc()
            delay(20)
          }
        }
        // Collection and cleanup are asynchronous; observe the OS boundary outcome.
        eventually { stopped && owner.lifecycle.observerCount == 0 }
        val reads = fixtures.clientReads
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        delay(30)
        assertEquals(reads, fixtures.clientReads)
      } finally {
        reference.get()?.close()
        owner.lifecycle.currentState = Lifecycle.State.DESTROYED
      }
    }
  }

  @Test
  fun lifecycleEventsRefreshResourcesAndStopAfterClose() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val fixtures = PackagedFixtures(InstrumentationRegistry.getInstrumentation().context)
      val clerk = connect(fixtures)
      val owner = Owner()
      owner.lifecycle.currentState = Lifecycle.State.CREATED
      observeApplicationLifecycle(clerk.context.requireRuntime(), owner.lifecycle)
      try {
        val before = fixtures.clientReads
        clerk.signIn.reset()
        assertEquals(before, fixtures.clientReads)
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        eventually { clerk.sessions.any { it.id == "sess_native" } }
        assertNull(clerk.session)
        val after = fixtures.clientReads
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        clerk.signIn.reset()
        assertEquals(after, fixtures.clientReads)
        clerk.close()
        owner.lifecycle.currentState = Lifecycle.State.CREATED
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        delay(30)
        assertEquals(after, fixtures.clientReads)
      } finally {
        clerk.close()
        owner.lifecycle.currentState = Lifecycle.State.DESTROYED
      }
    }
  }

  @Test
  fun networkRestorationRefreshesTheOwnerAndStopsAfterClose() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val fixtures = PackagedFixtures(InstrumentationRegistry.getInstrumentation().context)
      var receive: ((Boolean) -> Unit)? = null
      var stopped = false
      val key =
        "pk_test_" +
          Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk =
        connectCore(
          InstrumentationRegistry.getInstrumentation().targetContext,
          ClerkConfiguration(key, "clerk-test://sso-callback"),
          fixtures,
        ) { runtime ->
          observeNetworkConnectivity(runtime) { callback ->
            receive = callback
            val stop: () -> Unit = { stopped = true }
            stop
          }
        }
      val runtime = clerk.context.requireRuntime()
      try {
        val report = requireNotNull(receive)
        runtime.setApplicationActive(true)
        delay(30)
        val before = fixtures.clientReads
        report(false)
        report(true)
        eventually { fixtures.clientReads > before }
        eventually { clerk.sessions.any { it.id == "sess_native" } }
        assertNull(clerk.session)
        val after = fixtures.clientReads
        report(true)
        delay(30)
        assertEquals(after, fixtures.clientReads)
        clerk.close()
        eventually { stopped }
        report(false)
        report(true)
        delay(30)
        assertEquals(after, fixtures.clientReads)
      } finally {
        clerk.close()
      }
    }
  }

  @Test
  fun failedForegroundReloadDoesNotDisableTheOwnerAndCanRecover() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val fixtures = PackagedFixtures(InstrumentationRegistry.getInstrumentation().context)
      var failReload = false
      val capabilities =
        object : NativeCapabilities {
          override val supported = fixtures.supported

          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            if (
              failReload &&
                capability == "http" &&
                URI(arguments.jsonObject.getValue("url").requireString()).path.endsWith("/client")
            ) {
              return buildJsonObject {
                put("status", 422)
                put("headers", buildJsonObject {})
                put(
                  "body",
                  """{"errors":[{"code":"fixture_reload_failed","message":"Fixture reload failed"}]}""",
                )
              }
            }
            return fixtures.perform(capability, arguments)
          }
        }
      val clerk = connect(capabilities)
      val runtime = clerk.context.requireRuntime()
      val owner = Owner()
      owner.lifecycle.currentState = Lifecycle.State.CREATED
      observeApplicationLifecycle(runtime, owner.lifecycle)
      try {
        failReload = true
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        eventually { runtime.lastLifecycleError.value != null }
        assertTrue(runtime.isAvailable)
        clerk.signIn.reset()
        assertNull(clerk.session)
        failReload = false
        owner.lifecycle.currentState = Lifecycle.State.CREATED
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        eventually { clerk.sessions.any { it.id == "sess_native" } }
        assertTrue(runtime.isAvailable)
        assertNull(clerk.session)
      } finally {
        clerk.close()
        owner.lifecycle.currentState = Lifecycle.State.DESTROYED
      }
    }
  }

  @Test
  fun foregroundDuringOAuthRedemptionWaitsForAuthentication() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val fixtures = PackagedFixtures(InstrumentationRegistry.getInstrumentation().context)
      val redeeming = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      var clientReads = 0
      val complete =
        JsonObject(
          fixtures.fixtures.getValue("signIn").jsonObject +
            mapOf(
              "status" to JsonPrimitive("complete"),
              "created_session_id" to JsonPrimitive("sess_native"),
            )
        )
      val capabilities =
        object : NativeCapabilities {
          override val supported = fixtures.supported

          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            if (capability == "http") {
              val args = arguments.jsonObject
              val path = URI(args.getValue("url").requireString()).path
              if (path.contains("/sign_ins") && args["method"] == JsonPrimitive("GET")) {
                redeeming.complete(Unit)
                release.await()
                fixtures.clientResponse =
                  JsonObject(
                    fixtures.fixtures.getValue("client").jsonObject + ("sign_in" to complete)
                  )
              }
              if (path.endsWith("/client") && ++clientReads > 1) {
                val result = fixtures.perform(capability, arguments).jsonObject
                return JsonObject(
                  result +
                    ("headers" to
                      buildJsonObject { put("authorization", "foreground_rotated_credential") })
                )
              }
            }
            return fixtures.perform(capability, arguments)
          }
        }
      val clerk = connect(capabilities)
      val owner = Owner()
      owner.lifecycle.currentState = Lifecycle.State.CREATED
      observeApplicationLifecycle(clerk.context.requireRuntime(), owner.lifecycle)
      try {
        val attempt = async {
          clerk.signIn.sso(SignInSSOParams(SignInSSOParamsStrategy.OauthGoogle))
        }
        withTimeout(3000) { redeeming.await() }
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        delay(30)
        assertEquals(1, clientReads)
        release.complete(Unit)
        withTimeout(3000) { attempt.await() }
        assertEquals("complete", clerk.signIn.status.rawValue)
        assertNull(clerk.session)
        eventually { fixtures.credential == "foreground_rotated_credential" }
        assertEquals(2, clientReads)
        assertEquals("complete", clerk.signIn.status.rawValue)
        assertNull(clerk.context.requireRuntime().lastLifecycleError.value)
      } finally {
        release.complete(Unit)
        clerk.close()
        owner.lifecycle.currentState = Lifecycle.State.DESTROYED
      }
    }
  }
}
