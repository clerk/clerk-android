package com.clerk.footprint

import android.app.Activity
import com.clerk.api.*
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*

suspend fun startOwner(activity: Activity): AutoCloseable {
  // Explicit manual live mode keeps the production OS host and its dependencies reachable by R8.
  // Automated measurements always use the fixture branch and make no external requests.
  val liveKey = activity.intent.getStringExtra("publishableKey")
  val key =
    liveKey
      ?: "pk_test_" +
        Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
  val configuration = ClerkConfiguration(key, "clerk-footprint://callback")
  val clerk =
    if (liveKey == null) {
      Clerk.connect(activity, configuration, StartupFixtures(activity))
    } else {
      Clerk.connect(activity, configuration, activity = { activity })
    }
  try {
    check(clerk.loaded)
    val oldGroup = clerk.signIn.emailCode
    clerk.signIn.reset()
    check(oldGroup.isInvalidated)
  } catch (error: Exception) {
    clerk.close()
    throw error
  }
  return AutoCloseable { clerk.close() }
}

private class StartupFixtures(activity: Activity) : NativeCapabilities {
  override val supported = setOf("http", "storage", "timer", "random")
  private val fixtures =
    Json.parseToJsonElement(
        activity.assets.open("fapi.json").bufferedReader().use { it.readText() }
      )
      .jsonObject
  private var credential: JsonElement = JsonNull

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    val args = arguments.jsonObject
    return when (capability) {
      "storage.read" -> credential
      "storage.write" -> {
        credential = args.getValue("value")
        JsonNull
      }
      "storage.remove" -> {
        credential = JsonNull
        JsonNull
      }
      "timer" -> {
        delay(args.getValue("milliseconds").jsonPrimitive.long)
        JsonNull
      }
      "http" -> {
        check(args.getValue("method").jsonPrimitive.content == "GET")
        val path = URI(args.getValue("url").jsonPrimitive.content).path
        val name =
          when {
            path.endsWith("/environment") -> "environment"
            path.endsWith("/client") -> "client"
            else -> error("Unexpected fixture path: $path")
          }
        buildJsonObject {
          put("status", 200)
          put(
            "headers",
            buildJsonObject {
              if (name == "client") put("authorization", "fixture-client-credential")
            },
          )
          put("body", buildJsonObject { put("response", fixtures.getValue(name)) }.toString())
        }
      }
      else -> error("Unexpected fixture capability: $capability")
    }
  }
}
