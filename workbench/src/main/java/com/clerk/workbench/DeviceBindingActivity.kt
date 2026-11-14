package com.clerk.workbench

import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.FrameworkIntegrationApi
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken
import com.clerk.workbench.ui.theme.WorkbenchTheme
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DeviceBindingActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { WorkbenchTheme { DeviceBindingScreen() } }
  }
}

@OptIn(ExperimentalMaterial3Api::class, FrameworkIntegrationApi::class)
@Composable
private fun DeviceBindingScreen() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  var deviceToken by remember { mutableStateOf(Clerk.getDeviceToken()) }
  var status by remember { mutableStateOf("") }
  val keystoreFault = StorageHelper.loadValue(StorageKey.KEYSTORE_FAULT) == FAULT_ON

  Scaffold(topBar = { TopAppBar(title = { Text("Device binding") }) }) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Field("Session", session?.let { "${it.id} (${it.status})" } ?: "signed out")
      Field("Device token", deviceToken?.take(TOKEN_PREVIEW_LENGTH)?.plus("…") ?: "none")
      Field("cnf.jkt", deviceToken?.let(::confirmationThumbprint) ?: "none (unbound)")
      KeystoreFaultRow(
        enabled = keystoreFault,
        onChange = { enabled ->
          if (enabled) {
            StorageHelper.saveValue(StorageKey.KEYSTORE_FAULT, FAULT_ON)
          } else {
            StorageHelper.deleteValue(StorageKey.KEYSTORE_FAULT)
          }
          ProcessPhoenix.triggerRebirth(context)
        },
      )
      HorizontalDivider()
      ActionButton("Export device token") {
        deviceToken = Clerk.getDeviceToken()
        context.filesDir.resolve(EXPORTED_TOKEN_FILE).writeText(deviceToken.orEmpty())
        status = "Exported the device token to files/$EXPORTED_TOKEN_FILE"
      }
      ActionButton("Delete device key") {
        scope.launch {
          status =
            if (Clerk.deleteDeviceKey()) "Deleted the device key"
            else "Could not delete the device key"
        }
      }
      ActionButton("Time $REFRESH_COUNT token refreshes", enabled = session != null) {
        status = "Timing $REFRESH_COUNT refreshes…"
        scope.launch { status = timeRefreshes() }
      }
      ActionButton("Reload") { deviceToken = Clerk.getDeviceToken() }
      if (status.isNotEmpty()) {
        Text(status, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

@Composable
private fun KeystoreFaultRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Keystore fault at launch", modifier = Modifier.weight(1f))
    Switch(checked = enabled, onCheckedChange = onChange)
  }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
  Button(modifier = Modifier.fillMaxWidth(), enabled = enabled, onClick = onClick) { Text(text) }
}

@Composable
private fun Field(label: String, value: String) {
  Column {
    Text(label, style = MaterialTheme.typography.labelMedium)
    Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
  }
}

private suspend fun timeRefreshes(): String =
  withContext(Dispatchers.IO) {
    val session = Clerk.activeSession ?: return@withContext "No active session"
    val millis =
      List(REFRESH_COUNT) {
          val start = System.nanoTime()
          val result = session.fetchToken(GetTokenOptions(skipCache = true))
          if (result is ClerkResult.Failure) return@withContext "Refresh failed: $result"
          (System.nanoTime() - start) / NANOS_PER_MILLI
        }
        .sorted()
    val summary =
      "refresh timings ms n=$REFRESH_COUNT p50=${millis[P50_INDEX]} p99=${millis[P99_INDEX]}"
    Log.i(TIMING_LOG_TAG, summary)
    summary
  }

private fun confirmationThumbprint(deviceToken: String): String? = runCatching {
  val payload = deviceToken.split(".")[1]
  val claims = JSONObject(String(Base64.decode(payload, Base64.URL_SAFE)))
  claims.optJSONObject("cnf")?.optString("jkt")
}
  .getOrNull()

private const val FAULT_ON = "on"
private const val EXPORTED_TOKEN_FILE = "device-token.txt"
private const val TOKEN_PREVIEW_LENGTH = 24
private const val REFRESH_COUNT = 100
private const val P50_INDEX = 49
private const val P99_INDEX = 98
private const val NANOS_PER_MILLI = 1_000_000.0
private const val TIMING_LOG_TAG = "WorkbenchTiming"
