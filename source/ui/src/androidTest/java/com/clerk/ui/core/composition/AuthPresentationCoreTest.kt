package com.clerk.ui.core.composition

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clerk.api.*
import com.clerk.ui.auth.AuthDestination
import com.clerk.ui.auth.AuthState
import com.clerk.ui.auth.handleSessionTaskCompletion
import java.net.URI
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthPresentationCoreTest {
  private suspend fun verify(
    status: String?,
    hasUser: Boolean = true,
    block: suspend (Clerk, PresentationHost) -> Unit,
  ) {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val fixtures =
        Json.parseToJsonElement(
            instrumentation.context.assets.open("fapi.json").bufferedReader().use { it.readText() }
          )
          .jsonObject
      val host = PresentationHost(fixtures, status, hasUser)
      val key =
        "pk_test_" +
          Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk =
        Clerk.connect(
          instrumentation.targetContext,
          ClerkConfiguration(key, "clerk-test://sso-callback"),
          host,
        )
      try {
        block(clerk, host)
      } finally {
        clerk.close()
      }
    }
  }

  @Test
  fun registrationDoesNotHoldAnExistingActiveUser() = runBlocking {
    verify("active") { clerk, _ ->
      val presentation = AuthPresentationState(clerk)
      assertTrue(presentation.isComplete)
      val registration = presentation.register()
      try {
        assertTrue(presentation.isComplete)
        presentation.pending()
        assertTrue(presentation.isComplete)
      } finally {
        registration.close()
      }
    }
  }

  @Test
  fun foregroundRefreshPublishesSessionsWithoutImplicitlyCompletingAuthentication() = runBlocking {
    verify(null) { clerk, host ->
      val presentation = AuthPresentationState(clerk)
      val registration = presentation.register()
      try {
        assertFalse(presentation.isComplete)
        val observed =
          async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) {
              clerk.changes.first { it.sessions.any { session -> session.id == "sess_native" } }
            }
          }
        host.setSession("active")
        clerk.context.requireRuntime().setApplicationActive(false)
        clerk.context.requireRuntime().setApplicationActive(true)
        val published = observed.await()
        assertEquals(listOf("sess_native"), clerk.sessions.map { it.id })
        assertSame(clerk.sessions.single(), published.sessions.single())
        assertNull(clerk.session)
        assertNull(clerk.user)
        assertFalse(presentation.isComplete)
        val available = clerk.sessions.single()
        clerk.signIn.finalize()
        assertSame(available, clerk.session)
        assertNotNull(clerk.user)
        assertFalse(presentation.isComplete)
        presentation.complete()
        assertTrue(presentation.isComplete)
      } finally {
        registration.close()
      }
    }
  }

  @Test
  fun activeSnapshotWithoutUserCannotCompletePresentation() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val document =
        Json.parseToJsonElement(
            context.assets.open("clerk-preview/default.json").bufferedReader().use { it.readText() }
          )
          .jsonObject
      val original = document.getValue("state").jsonObject
      val roots = JsonObject(original.getValue("roots").jsonObject + ("user" to JsonNull))
      val resources =
        original.getValue("resources").jsonArray.map { resource ->
          val value = resource.jsonObject
          if (value.getValue("handle").jsonObject["type"] == JsonPrimitive("Clerk"))
            JsonObject(
              value +
                ("state" to JsonObject(value.getValue("state").jsonObject + ("user" to JsonNull)))
            )
          else resource
        }
      val transport =
        object : CoreTransport {
          override var receive: ((JsonElement) -> Unit)? = null

          override fun send(message: JsonElement) {}

          override fun close() {}
        }
      val runtime = CoreRuntime(transport, Dispatchers.Main.immediate)
      transport.receive!!.invoke(
        buildJsonObject {
          put("kind", "ready")
          put("id", "fixture")
          put("manifest", document.getValue("manifest"))
          put(
            "state",
            JsonObject(original + mapOf("roots" to roots, "resources" to JsonArray(resources))),
          )
        }
      )
      val clerk = runtime.resource(runtime.roots.getValue("clerk")) as Clerk
      try {
        assertNotNull(clerk.session)
        assertNull(clerk.user)
        val presentation = AuthPresentationState(clerk)
        assertFalse(presentation.isComplete)
        presentation.complete()
        assertFalse(presentation.isComplete)
        withAuthState(clerk) { state ->
          var completions = 0
          state.completePresentation { completions++ }
          assertEquals(0, completions)
        }
      } finally {
        clerk.close()
      }
    }
  }

  @Test
  fun pendingSessionCannotBeMarkedCompleteBeforeItsTaskFinishes() = runBlocking {
    verify("pending") { clerk, host ->
      val presentation = AuthPresentationState(clerk)
      val registration = presentation.register()
      try {
        assertFalse(presentation.isComplete)
        presentation.complete()
        host.setSession("active")
        clerk.session!!.reload()
        assertFalse(presentation.isComplete)
        presentation.complete()
        assertTrue(presentation.isComplete)
      } finally {
        registration.close()
      }
    }
  }

  @Test
  fun releasingAnOlderRegistrationDoesNotReleaseTheRemainingHold() = runBlocking {
    verify("pending") { clerk, host ->
      val presentation = AuthPresentationState(clerk)
      val first = presentation.register()
      val second = presentation.register()
      first.close()
      first.close()
      host.setSession("active")
      clerk.session!!.reload()
      assertFalse(presentation.isComplete)
      second.close()
      assertTrue(presentation.isComplete)
    }
  }

  @Test
  fun aNewPendingRegistrationCannotReuseThePreviousCompletion() = runBlocking {
    verify("pending") { clerk, host ->
      val presentation = AuthPresentationState(clerk)
      val first = presentation.register()
      host.setSession("active")
      clerk.session!!.reload()
      presentation.complete()
      assertTrue(presentation.isComplete)
      first.close()
      host.setSession("pending")
      clerk.session!!.reload()
      val second = presentation.register()
      try {
        host.setSession("active")
        clerk.session!!.reload()
        assertFalse(presentation.isComplete)
        presentation.complete()
        assertTrue(presentation.isComplete)
      } finally {
        second.close()
      }
    }
  }

  @Test
  fun prebuiltCompletionFinalizesExactlyOnceAndWaitsForPendingTasks() = runBlocking {
    for (status in listOf("active", "pending")) {
      verify(null) { clerk, host ->
        val presentation = AuthPresentationState(clerk)
        val registration = presentation.register()
        try {
          assertFalse(presentation.isComplete)
          assertEquals("complete", clerk.signIn.status.rawValue)
          assertNull(clerk.session)
          host.setSession(status)
          withAuthState(clerk) { state ->
            var completions = 0
            val complete = {
              presentation.complete()
              completions++
              Unit
            }
            assertTrue(state.setToStepForStatus(clerk.signIn, complete))
            assertEquals(status, clerk.session?.status?.rawValue)
            if (status == "pending") {
              assertEquals(0, completions)
              assertFalse(presentation.isComplete)
              assertEquals(AuthDestination.SessionTaskChooseOrganization, state.backStack.last())
              host.setSession("active")
              clerk.session!!.reload()
              state.completePresentation(complete)
            }
            assertEquals(1, completions)
            assertTrue(presentation.isComplete)
            val touches = host.touches
            assertTrue(state.setToStepForStatus(clerk.signIn, complete))
            assertEquals(touches, host.touches)
            assertEquals(1, completions)
          }
        } finally {
          registration.close()
        }
      }
    }
  }

  @Test
  fun taskKeysPreserveCanonicalSpellingAndPreventPrematureCompletion() = runBlocking {
    val cases =
      listOf(
        Triple("setup-mfa", SessionTaskKey.SetupMfa, AuthDestination.SessionTaskMfa),
        Triple(
          "reset-password",
          SessionTaskKey.ResetPassword,
          AuthDestination.SessionTaskResetPassword,
        ),
        Triple(
          "choose-organization",
          SessionTaskKey.ChooseOrganization,
          AuthDestination.SessionTaskChooseOrganization,
        ),
      ) +
        listOf(
            "mfa_required",
            "mfa-required",
            "setup_mfa",
            "reset_password",
            "choose_organization",
            "future-task",
          )
          .map {
            Triple(it, SessionTaskKey.Unrecognized(it), AuthDestination.SignInGetHelp)
          }
    for (status in listOf("active", "pending")) {
      verify("pending") { clerk, host ->
        val presentation = AuthPresentationState(clerk)
        val registration = presentation.register()
        try {
          val session = clerk.session!!
          for ((key, expectedKey, destination) in cases) {
            host.setSession(status, taskKeys = listOf(key))
            session.reload()
            assertSame(session, clerk.session)
            assertEquals(status, session.status.rawValue)
            assertEquals(listOf(expectedKey), session.tasks?.map { it.key })
            assertEquals(expectedKey, session.currentTask?.key)
            assertEquals(destination, com.clerk.ui.auth.pendingSessionTaskDestination(expectedKey))
            withAuthState(clerk) { state ->
              var completions = 0
              assertTrue(state.setToStepForStatus(clerk.signIn) { completions++ })
              assertEquals(destination, state.backStack.last())
              state.completePresentation { completions++ }
              assertEquals(0, completions)
            }
            presentation.complete()
            assertFalse(presentation.isComplete)
          }
        } finally {
          registration.close()
        }
      }
    }
  }

  @Test
  fun orderedTasksDriveRoutingUntilTheLastRequirementClears() = runBlocking {
    verify("pending") { clerk, host ->
      val presentation = AuthPresentationState(clerk)
      val registration = presentation.register()
      try {
        val session = clerk.session!!
        withAuthState(clerk) { state ->
          var completions = 0
          val complete = {
            presentation.complete()
            completions++
            Unit
          }
          for ((keys, destination) in
            listOf(
              listOf("reset-password", "setup-mfa") to AuthDestination.SessionTaskResetPassword,
              listOf("setup-mfa", "reset-password") to AuthDestination.SessionTaskMfa,
              listOf("future-task", "setup-mfa") to AuthDestination.SignInGetHelp,
              listOf("choose-organization") to AuthDestination.SessionTaskChooseOrganization,
            )) {
            // The old native DTO accepted a separate currentTask. The core derives it from tasks.
            host.setSession("pending", taskKeys = keys, reportedCurrentTask = "reset-password")
            session.reload()
            assertSame(session, clerk.session)
            assertEquals(keys, session.tasks?.map { it.key.rawValue })
            assertEquals(keys.first(), session.currentTask?.key?.rawValue)
            state.handleSessionTaskCompletion(session, complete)
            assertEquals(destination, state.backStack.last())
            assertFalse(presentation.isComplete)
            assertEquals(0, completions)
          }
          host.setSession("pending", taskKeys = emptyList())
          session.reload()
          assertNull(session.currentTask)
          state.handleSessionTaskCompletion(session, complete)
          assertFalse(presentation.isComplete)
          assertEquals(0, completions)
          host.setSession("active", taskKeys = emptyList())
          session.reload()
          state.handleSessionTaskCompletion(session, complete)
          assertTrue(presentation.isComplete)
          assertEquals(1, completions)
          state.handleSessionTaskCompletion(session, complete)
          assertEquals(1, completions)
        }
      } finally {
        registration.close()
      }
    }
  }

  private suspend fun withAuthState(clerk: Clerk, block: suspend (AuthState) -> Unit) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val name = "auth-presentation-fixture-${UUID.randomUUID()}"
    val preferences = context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)
    val state =
      AuthState(
        clerk,
        backStack = NavBackStack<NavKey>(AuthDestination.AuthStart),
        sharedPreferences = preferences,
      )
    try {
      block(state)
    } finally {
      context.deleteSharedPreferences(name)
    }
  }
}

private class PresentationHost(
  private val fixtures: JsonObject,
  status: String?,
  hasUser: Boolean,
) : NativeCapabilities {
  override val supported = setOf("http", "storage", "timer", "random")
  private var credential: JsonElement = JsonNull
  private var client = fixtures.getValue("client").jsonObject
  var touches = 0
    private set

  init {
    setSession(status, hasUser)
  }

  fun setSession(
    status: String?,
    hasUser: Boolean = true,
    taskKeys: List<String> =
      if (status == "pending") listOf("choose-organization") else emptyList(),
    reportedCurrentTask: String? = null,
  ) {
    val session =
      JsonObject(
        fixtures.getValue("session").jsonObject +
          mapOf(
            "status" to JsonPrimitive(status ?: "active"),
            "user" to
              if (hasUser) fixtures.getValue("session").jsonObject.getValue("user") else JsonNull,
            "tasks" to JsonArray(taskKeys.map { key -> buildJsonObject { put("key", key) } }),
            "current_task" to
              (reportedCurrentTask?.let { key -> buildJsonObject { put("key", key) } } ?: JsonNull),
          )
      )
    val signIn =
      JsonObject(
        fixtures.getValue("signIn").jsonObject +
          mapOf(
            "status" to JsonPrimitive("complete"),
            "created_session_id" to JsonPrimitive("sess_native"),
          )
      )
    client =
      JsonObject(
        fixtures.getValue("client").jsonObject +
          mapOf(
            "sessions" to JsonArray(if (status == null) emptyList() else listOf(session)),
            "last_active_session_id" to
              if (status == null) JsonNull else JsonPrimitive("sess_native"),
            "sign_in" to signIn,
          )
      )
  }

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    val args = arguments.jsonObject
    when (capability) {
      "storage.read" -> return credential
      "storage.write" -> {
        credential = args.getValue("value")
        return JsonNull
      }
      "storage.remove" -> {
        credential = JsonNull
        return JsonNull
      }
      "timer" -> {
        delay(args.getValue("milliseconds").jsonPrimitive.double.toLong())
        return JsonNull
      }
    }
    check(capability == "http")
    val path = URI(args.getValue("url").jsonPrimitive.content).path
    val body =
      when {
        path.endsWith("/environment") ->
          buildJsonObject { put("response", fixtures.getValue("environment")) }
        path.endsWith("/client") -> buildJsonObject { put("response", client) }
        path.endsWith("/tokens") -> fixtures.getValue("token")
        path.endsWith("/touch") || path.endsWith("/sessions/sess_native") -> {
          if (path.endsWith("/touch")) touches++
          buildJsonObject {
            put("response", client.getValue("sessions").jsonArray.first())
            put("client", client)
          }
        }
        else -> error("Unexpected fixture path: $path")
      }
    return buildJsonObject {
      put("status", 200)
      put(
        "headers",
        buildJsonObject {
          if (!path.endsWith("/environment")) put("authorization", "fixture-credential")
        },
      )
      put("body", body.toString())
    }
  }
}
