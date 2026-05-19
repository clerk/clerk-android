package com.clerk.api.network.serialization

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergePatchTest {

  @Test
  fun `added keys appear verbatim`() {
    val current = buildJsonObject { put("a", 1) }
    val desired = buildJsonObject {
      put("a", 1)
      put("b", 2)
    }
    val expected = buildJsonObject { put("b", 2) }
    assertEquals(expected, computeMergePatch(current, desired))
  }

  @Test
  fun `keys absent from desired become null (RFC 7396)`() {
    val current = buildJsonObject {
      put("a", 1)
      put("b", 2)
    }
    val desired = buildJsonObject { put("a", 1) }
    val expected = buildJsonObject { put("b", JsonNull) }
    assertEquals(expected, computeMergePatch(current, desired))
  }

  @Test
  fun `changed primitive values are overwritten`() {
    val current = buildJsonObject { put("a", 1) }
    val desired = buildJsonObject { put("a", 2) }
    val expected = buildJsonObject { put("a", 2) }
    assertEquals(expected, computeMergePatch(current, desired))
  }

  @Test
  fun `unchanged values are skipped`() {
    val current = buildJsonObject {
      put("a", 1)
      put("b", 2)
    }
    val desired = buildJsonObject {
      put("a", 1)
      put("b", 2)
    }
    assertEquals(JsonObject(emptyMap()), computeMergePatch(current, desired))
  }

  @Test
  fun `nested objects recurse and emit only changed sub-keys`() {
    val current = buildJsonObject {
      put(
        "profile",
        buildJsonObject {
          put("theme", "dark")
          put("font", "sans")
        },
      )
    }
    val desired = buildJsonObject {
      put(
        "profile",
        buildJsonObject {
          put("theme", "light")
          put("font", "sans")
        },
      )
    }
    val expected = buildJsonObject {
      put("profile", buildJsonObject { put("theme", "light") })
    }
    assertEquals(expected, computeMergePatch(current, desired))
  }

  @Test
  fun `removed nested key is nulled, siblings untouched`() {
    val current = buildJsonObject {
      put(
        "profile",
        buildJsonObject {
          put("theme", "dark")
          put("font", "sans")
        },
      )
    }
    val desired = buildJsonObject {
      put("profile", buildJsonObject { put("font", "sans") })
    }
    val expected = buildJsonObject {
      put("profile", buildJsonObject { put("theme", JsonNull) })
    }
    assertEquals(expected, computeMergePatch(current, desired))
  }

  @Test
  fun `type mismatch returns desired verbatim`() {
    val current: JsonElement = buildJsonObject { put("a", 1) }
    val desired: JsonElement = JsonPrimitive("replaced")
    assertEquals(JsonPrimitive("replaced"), computeMergePatch(current, desired))
  }

  @Test
  fun `null desired is passed through verbatim`() {
    val current: JsonElement = buildJsonObject { put("a", 1) }
    assertEquals(JsonNull, computeMergePatch(current, JsonNull))
  }

  @Test
  fun `desired empty object clears every existing key`() {
    val current = buildJsonObject {
      put("a", 1)
      put("b", 2)
    }
    val desired = JsonObject(emptyMap())
    val expected = buildJsonObject {
      put("a", JsonNull)
      put("b", JsonNull)
    }
    assertEquals(expected, computeMergePatch(current, desired))
  }

  @Test
  fun `empty current returns desired verbatim`() {
    val current = JsonObject(emptyMap())
    val desired = buildJsonObject {
      put("a", 1)
      put("nested", buildJsonObject { put("c", 2) })
    }
    assertEquals(desired, computeMergePatch(current, desired))
  }

  @Test
  fun `arrays are treated as atomic values (replace, not merge)`() {
    // RFC 7396 explicitly treats arrays as opaque.
    val current = buildJsonObject {
      put(
        "tags",
        kotlinx.serialization.json.buildJsonArray {
          add(JsonPrimitive("a"))
          add(JsonPrimitive("b"))
        },
      )
    }
    val desired = buildJsonObject {
      put(
        "tags",
        kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive("a")) },
      )
    }
    val expected = buildJsonObject {
      put(
        "tags",
        kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive("a")) },
      )
    }
    assertEquals(expected, computeMergePatch(current, desired))
  }

  @Test
  fun `applying the patch reproduces desired (round-trip sanity check)`() {
    val current = buildJsonObject {
      put("a", 1)
      put(
        "nested",
        buildJsonObject {
          put("x", 1)
          put("y", 2)
        },
      )
      put("removed", true)
    }
    val desired = buildJsonObject {
      put("a", 2)
      put(
        "nested",
        buildJsonObject {
          put("x", 1)
          put("z", 3)
        },
      )
      put("added", "yes")
    }

    val patch = computeMergePatch(current, desired)
    val applied = applyMergePatch(current, patch)

    assertEquals(desired, applied)
    assertTrue("Patch must not be empty for a real change", (patch as JsonObject).isNotEmpty())
  }

  // RFC 7396 reference implementation of patch application. Used to validate that the
  // patch we compute, when applied with merge semantics, reproduces the desired state.
  private fun applyMergePatch(target: JsonElement, patch: JsonElement): JsonElement {
    if (patch !is JsonObject) {
      return patch
    }
    val out =
      mutableMapOf<String, JsonElement>().apply {
        if (target is JsonObject) putAll(target)
      }
    for ((key, value) in patch) {
      if (value is JsonNull) {
        out.remove(key)
      } else {
        out[key] = applyMergePatch(out[key] ?: JsonObject(emptyMap()), value)
      }
    }
    return JsonObject(out)
  }
}
