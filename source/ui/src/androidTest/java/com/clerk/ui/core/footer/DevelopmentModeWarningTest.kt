package com.clerk.ui.core.footer

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clerk.api.*
import com.clerk.ui.R
import com.clerk.ui.core.composition.ClerkProvider
import java.io.File
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevelopmentModeWarningTest {
  @get:Rule val compose = createComposeRule()

  @Test fun developmentShowsRequestedWarning() = verify(Step("development", true, true))

  @Test fun productionNeverShowsDevelopmentWarning() = verify(Step("production", true, false))

  @Test fun disabledWarningPreservesNormalBranding() = verify(Step("development", false, false))

  @Test
  fun environmentReloadSwitchesWarningAndRestoresBranding() =
    verify(
      Step("development", true, true),
      Step("production", true, false),
      Step("development", false, false),
    )

  private data class Step(val type: String, val warning: Boolean, val visible: Boolean)

  private fun verify(vararg steps: Step) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    val fixtures =
      Json.parseToJsonElement(
          instrumentation.context.assets.open("fapi.json").bufferedReader().use { it.readText() }
        )
        .jsonObject
    var step = steps.first()
    val requests = mutableListOf<String>()
    var credential: JsonElement = JsonNull
    val host =
      object : NativeCapabilities {
        override val supported = setOf("http", "storage", "timer", "random")

        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          when (capability) {
            "storage.read" -> return credential
            "storage.write" -> {
              credential = arguments.jsonObject.getValue("value")
              return JsonNull
            }
            "storage.remove" -> {
              credential = JsonNull
              return JsonNull
            }
          }
          if (capability == "timer") {
            delay(arguments.jsonObject.getValue("milliseconds").jsonPrimitive.double.toLong())
            return JsonNull
          }
          check(capability == "http")
          val args = arguments.jsonObject
          val path = URI(args.getValue("url").jsonPrimitive.content).path
          requests += "${args["method"]} $path"
          check(args["method"] == JsonPrimitive("GET"))
          val response =
            when (path) {
              "/v1/environment" -> {
                val environment = fixtures.getValue("environment").jsonObject
                val display =
                  JsonObject(
                    environment.getValue("display_config").jsonObject +
                      mapOf(
                        "instance_environment_type" to JsonPrimitive(step.type),
                        "show_devmode_warning" to JsonPrimitive(step.warning),
                        "branded" to JsonPrimitive(true),
                      )
                  )
                JsonObject(environment + ("display_config" to display))
              }
              "/v1/client" -> fixtures.getValue("client")
              else -> error("Unexpected footer request")
            }
          return buildJsonObject {
            put("status", 200)
            put(
              "headers",
              buildJsonObject {
                if (path == "/v1/client") put("authorization", "fixture-credential")
              },
            )
            put("body", buildJsonObject { put("response", response) }.toString())
          }
        }
      }
    val clerk = runBlocking {
      withContext(Dispatchers.Main.immediate) {
        try {
          Clerk.connect(
            context,
            ClerkConfiguration(
              "pk_test_" +
                Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray()),
              "clerk-test://sso-callback",
            ),
            host,
          )
        } catch (failure: CoreException) {
          throw AssertionError("Core setup ${failure.code}; requests=$requests", failure)
        }
      }
    }
    val mounted = mutableStateOf(true)
    try {
      compose.setContent {
        if (mounted.value)
          ClerkProvider(clerk) {
            Column(Modifier.fillMaxSize()) {
              DevelopmentModeWarningBox(
                Modifier.fillMaxWidth().height(240.dp),
                showBranding = false,
              ) {
                Text("Account content")
              }
              DevelopmentModeWarning(showBranding = false)
              Box(Modifier.testTag("normal-branding")) { SecuredByClerkView() }
            }
          }
      }
      for ((index, next) in steps.withIndex()) {
        if (index > 0) {
          step = next
          runBlocking { withContext(Dispatchers.Main.immediate) { clerk.environment.reload() } }
        }
        compose.waitForIdle()
        capture("${steps.size}-${index}-${next.type}-${next.warning}")
        compose
          .onAllNodesWithText(context.getString(R.string.development_mode))
          .assertCountEquals(if (next.visible) 2 else 0)
        compose.onNodeWithText("Account content").assertIsDisplayed()
        val branding =
          compose.onNode(
            hasText(context.getString(R.string.secured_by)) and
              hasAnyAncestor(hasTestTag("normal-branding"))
          )
        if (next.visible) branding.assertDoesNotExist() else branding.assertIsDisplayed()
      }
    } finally {
      compose.runOnIdle { mounted.value = false }
      compose.waitForIdle()
      runBlocking { withContext(Dispatchers.Main.immediate) { clerk.close() } }
    }
  }

  private fun capture(name: String) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
    val root = output?.let(::File) ?: context.getExternalFilesDir(null)
    val directory = File(root, "development-warning").apply { mkdirs() }
    File(directory, "$name.png").outputStream().use {
      check(
        compose
          .onRoot()
          .captureToImage()
          .asAndroidBitmap()
          .compress(Bitmap.CompressFormat.PNG, 100, it)
      )
    }
  }
}
