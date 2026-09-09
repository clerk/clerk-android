package com.clerk.ui.core.composition

import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clerk.api.Clerk
import com.clerk.api.CoreException
import com.clerk.api.CoreRuntime
import com.clerk.api.CoreTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClerkModelLifetimeTest {
  @get:Rule val compose = createComposeRule()

  private fun pendingCore(): PendingCore {
    lateinit var core: PendingCore
    compose.runOnIdle { core = PendingCore(InstrumentationRegistry.getInstrumentation().targetContext) }
    return core
  }

  @Test
  fun removingAScreenCancelsItsPendingCoreRequest() {
    val core = pendingCore()
    val mounted = mutableStateOf(true)
    try {
      compose.setContent {
        ClerkProvider(core.clerk) {
          if (mounted.value) {
            clerkViewModel { ReloadingModel(it) }
            Button(onClick = { mounted.value = false }) { Text("Close profile") }
          } else {
            Text("Profile closed")
          }
        }
      }
      compose.waitForIdle()
      compose.runOnIdle { assertEquals(listOf("User.reload"), core.invocations.values.toList()) }
      compose.onNodeWithText("Close profile").performClick()
      compose.waitForIdle()
      compose.runOnIdle { assertEquals(core.invocations.keys.toList(), core.cancellations) }
    } finally {
      compose.runOnIdle { core.close() }
    }
  }

  @Test
  fun changingCoreCancelsOldRequestsButRecompositionKeepsCurrentWork() {
    val first = pendingCore()
    val second = pendingCore()
    val selected = mutableStateOf(first.clerk)
    val count = mutableIntStateOf(0)
    try {
      compose.setContent {
        ClerkProvider(selected.value) {
          clerkViewModel { ReloadingModel(it) }
          Button(onClick = { count.intValue++ }) { Text("Refresh ${count.intValue}") }
        }
      }
      compose.onNodeWithText("Refresh 0").performClick()
      compose.waitForIdle()
      compose.runOnIdle {
        assertEquals(1, first.invocations.size)
        assertEquals(0, first.cancellations.size)
        selected.value = second.clerk
      }
      compose.waitForIdle()
      compose.runOnIdle {
        assertEquals(first.invocations.keys.toList(), first.cancellations)
        assertEquals(listOf("User.reload"), second.invocations.values.toList())
        assertEquals(0, second.cancellations.size)
      }
    } finally {
      compose.runOnIdle { first.close(); second.close() }
    }
  }
}

private class ReloadingModel(clerk: Clerk) : ViewModel() {
  init {
    viewModelScope.launch {
      try { clerk.user!!.reload() }
      catch (_: CoreException) { /* A host can close while its request is pending. */ }
    }
  }
}

/** Source-generated projection, with a transport that leaves requests pending until cancellation. */
private class PendingCore(context: Context) : AutoCloseable {
  val invocations = linkedMapOf<String, String>()
  val cancellations = mutableListOf<String>()
  private val transport = object : CoreTransport {
    override var receive: ((JsonElement) -> Unit)? = null
    override fun send(message: JsonElement) {
      val value = message.jsonObject
      when (value["kind"]?.jsonPrimitive?.content) {
        "invoke" -> invocations[value.getValue("id").jsonPrimitive.content] = value.getValue("operation").jsonPrimitive.content
        "cancel" -> cancellations += value.getValue("id").jsonPrimitive.content
      }
    }
    override fun close() { receive = null }
  }
  private val runtime = CoreRuntime(transport, Dispatchers.Main.immediate)
  val clerk: Clerk
  init {
    val document = context.assets.open("clerk-preview/default.json").bufferedReader().use {
      Json.parseToJsonElement(it.readText()).jsonObject
    }
    transport.receive!!.invoke(buildJsonObject {
      put("kind", "ready"); put("id", "fixture")
      put("manifest", document.getValue("manifest")); put("state", document.getValue("state"))
    })
    clerk = runtime.resource(runtime.roots.getValue("clerk")) as Clerk
  }
  override fun close() = runtime.close()
}
