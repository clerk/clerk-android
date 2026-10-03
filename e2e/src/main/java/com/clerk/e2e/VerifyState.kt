package com.clerk.e2e

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

const val VERIFY_STATE_TAG = "verify.state"
const val VERIFY_SIGN_OUT_TAG = "verify.signOut"
const val VERIFY_LOG_TAG = "ClerkVerify"

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

@Composable
fun VerifyStateFooter(state: VerifyState) {
  Text(
    text = state.line,
    modifier =
      Modifier.fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .testTag(VERIFY_STATE_TAG),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontFamily = FontFamily.Monospace,
    fontSize = 9.sp,
    lineHeight = 11.sp,
  )
}
