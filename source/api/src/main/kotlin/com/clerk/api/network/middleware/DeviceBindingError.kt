package com.clerk.api.network.middleware

import com.clerk.api.network.model.error.ClerkErrorResponse
import java.io.IOException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import kotlinx.serialization.json.Json
import okhttp3.Response

internal enum class DeviceBindingError(val code: String, val recovery: Recovery) {
  PROOF_INVALID("device_binding_proof_invalid", Recovery.RETRY_WITH_SERVER_CLOCK),
  PROOF_REPLAYED("device_binding_proof_replayed", Recovery.NONE),
  PROOF_MISSING("device_binding_proof_missing", Recovery.DROP_TOKEN),
  KEY_MISMATCH("device_binding_key_mismatch", Recovery.DROP_TOKEN);

  enum class Recovery {
    RETRY_WITH_SERVER_CLOCK,
    DROP_TOKEN,
    NONE,
  }

  companion object {
    private const val MAX_ERROR_BODY_BYTES = 64L * 1024

    fun of(response: Response, json: Json): DeviceBindingError? =
      if (response.code == HTTP_UNAUTHORIZED) {
        val codes = errorCodes(response, json)
        entries.firstOrNull { it.code in codes }
      } else {
        null
      }

    private fun errorCodes(response: Response, json: Json): List<String> =
      try {
        json
          .decodeFromString<ClerkErrorResponse>(response.peekBody(MAX_ERROR_BODY_BYTES).string())
          .errors
          .mapNotNull { it.code }
      } catch (_: IllegalArgumentException) {
        emptyList()
      } catch (_: IOException) {
        emptyList()
      }
  }
}
