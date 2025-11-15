package com.clerk.telemetry

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TelemetryEventsTest {

  @Test
  fun `methodInvoked merges payload and uses default sampling`() {
    val payload = telemetryPayload("extra" to "value")

    val event = TelemetryEvents.methodInvoked(method = "create", payload = payload)

    val payloadJson = JsonObject(event.payload)

    assertEquals("METHOD_INVOKED", event.event)
    assertEquals(0.1, event.eventSamplingRate)
    assertEquals("create", payloadJson.getValue("method").jsonPrimitive.content)
    assertEquals("value", payloadJson.getValue("extra").jsonPrimitive.content)
  }

  @Test
  fun `viewDidAppear allows overriding sampling rate`() {
    val event = TelemetryEvents.viewDidAppear(viewName = "Home", samplingRate = 0.42)

    val payloadJson = JsonObject(event.payload)

    assertEquals("VIEW_DID_APPEAR", event.event)
    assertEquals(0.42, event.eventSamplingRate)
    assertEquals("Home", payloadJson.getValue("view").jsonPrimitive.content)
  }

  @Test
  fun `telemetryPayload encodes supported primitive values`() {
    val payload =
      telemetryPayload(
        "string" to "text",
        "boolean" to true,
        "int" to 7,
        "long" to 9L,
        "double" to 3.14,
        "null" to null,
      )

    val jsonObject = JsonObject(payload)

    assertEquals("text", jsonObject.getValue("string").jsonPrimitive.content)
    assertTrue(jsonObject.getValue("boolean").jsonPrimitive.boolean)
    assertEquals(7, jsonObject.getValue("int").jsonPrimitive.int)
    assertEquals(9, jsonObject.getValue("long").jsonPrimitive.int)
    assertEquals(3.14, jsonObject.getValue("double").jsonPrimitive.double)
    assertNull(jsonObject.getValue("null").jsonPrimitive.contentOrNull)
  }
}
