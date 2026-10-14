package com.clerk.e2e

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

const val VERIFY_LOG_TAG = "ClerkVerify"

inline fun <reified T> serialName(value: T): String =
  Json.encodeToJsonElement(value).jsonPrimitive.content

data class VerifyFailure(val code: String, val message: String)

enum class VerifyTicket(val wireName: String) {
  None("none"),
  Pending("pending"),
  Succeeded("succeeded"),
  Failed("failed"),
}

data class VerifyState(
  val runId: String?,
  val launchId: String?,
  val screen: HostScreen,
  val environmentLoaded: Boolean,
  val userId: String?,
  val sessionId: String?,
  val sessionStatus: String?,
  val pendingTasks: List<String>,
  val orgId: String?,
  val signInStatus: String?,
  val signUpStatus: String?,
  val ticket: VerifyTicket,
  val lastError: VerifyFailure?,
) {
  val line: String
    get() {
      val fields =
        mapOf(
          "v" to JsonPrimitive(1),
          "runId" to JsonPrimitive(runId),
          "launchId" to JsonPrimitive(launchId),
          "screen" to JsonPrimitive(screen.wireName),
          "environmentLoaded" to JsonPrimitive(environmentLoaded),
          "signedIn" to JsonPrimitive(userId != null),
          "userId" to JsonPrimitive(userId),
          "sessionId" to JsonPrimitive(sessionId),
          "sessionStatus" to JsonPrimitive(sessionStatus),
          "pendingTasks" to JsonArray(pendingTasks.map(::JsonPrimitive)),
          "orgId" to JsonPrimitive(orgId),
          "signInStatus" to JsonPrimitive(signInStatus),
          "signUpStatus" to JsonPrimitive(signUpStatus),
          "ticket" to JsonPrimitive(ticket.wireName),
          "lastError" to (lastError?.toJson() ?: JsonNull),
        )
      return "verify ${JsonObject(fields.toSortedMap())}"
    }

  private fun VerifyFailure.toJson(): JsonElement =
    JsonObject(sortedMapOf("code" to JsonPrimitive(code), "message" to JsonPrimitive(message)))
}
