package com.clerk.api.protect

import android.view.ViewGroup
import androidx.annotation.RestrictTo
import com.clerk.api.network.model.environment.Environment
import com.clerk.protect.Protect
import com.clerk.protect.ProtectCheck
import com.clerk.protect.ProtectException
import com.clerk.protect.asProtectHost
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** Internal bridge between Clerk Android resources and the Protect dispatcher. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ClerkProtect {

  /** Whether the latest Clerk environment enabled Protect. */
  val isEnabled: Boolean
    get() = Protect.isEnabled

  /** Executes one Protect challenge inside [container] and returns its proof token. */
  suspend fun executeProtectCheck(check: ProtectCheckResource, container: ViewGroup): String =
    Protect.executeProtectCheck(check.toProtectCheck(), container.asProtectHost())

  /** Returns Protect's stable error code for [throwable], if it originated in Protect. */
  fun errorCode(throwable: Throwable): String? = (throwable as? ProtectException)?.code

  internal fun initialize(environment: Environment, fapiOrigin: String?) {
    val protectEnvironment =
      buildMap<String, Any?> {
        runCatching { environment.protectConfig }
          .getOrNull()
          ?.let { put("protect_config", it.toAnyMap()) }
        runCatching { environment.displayConfig.frontendApiUrl }
          .getOrNull()
          ?.let { put("display_config", mapOf("frontend_api_url" to it)) }
        runCatching { environment.frontendApi }.getOrNull()?.let { put("frontend_api", it) }
      }
    Protect.initialize(protectEnvironment, fapiOrigin)
  }

  internal fun reset() {
    Protect.setAssertion(null)
    Protect.initialize(emptyMap())
  }

  internal fun setAssertion(token: String?) {
    Protect.setAssertion(token)
  }
}

internal fun ProtectCheckResource.toProtectCheck(): ProtectCheck =
  ProtectCheck.fromJson(raw.toAnyMap())

private fun JsonObject.toAnyMap(): Map<String, Any?> = mapValues { (_, value) ->
  value.toAnyValue()
}

private fun JsonElement.toAnyValue(): Any? =
  when (this) {
    is JsonObject -> toAnyMap()
    is JsonArray -> map(JsonElement::toAnyValue)
    is JsonNull -> null
    is JsonPrimitive ->
      if (isString) {
        content
      } else {
        booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
      }
  }
