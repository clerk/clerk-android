package com.clerk.api.network.serialization

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Computes a JSON Merge Patch (RFC 7396) that, when deep-merged into [current], produces
 * [desired]. Keys present in [current] but absent from [desired] receive `null` in the patch
 * (RFC 7396 null-delete semantics).
 *
 * Used to express *replace* semantics through a merge endpoint: the SDK holds the current
 * resource state locally, the caller passes the desired state, and we send the diff that
 * makes the server side end up at the desired state.
 *
 * Behaviour:
 * - both plain objects: recurse; emit only keys whose value changes
 * - `desired === JsonNull`: returned verbatim (caller decides what null means)
 * - any other type mismatch: [desired] is returned (full replace at that node)
 */
@Suppress("ReturnCount") // Two guard clauses plus a final return is the natural shape here.
internal fun computeMergePatch(current: JsonElement, desired: JsonElement): JsonElement {
  if (desired is JsonNull) {
    return JsonNull
  }
  if (current !is JsonObject || desired !is JsonObject) {
    return desired
  }

  val patch = mutableMapOf<String, JsonElement>()

  for ((key, des) in desired) {
    val cur = current[key]
    if (cur == null) {
      patch[key] = des
      continue
    }
    if (cur is JsonObject && des is JsonObject) {
      val sub = computeMergePatch(cur, des)
      if (sub is JsonObject && sub.isEmpty()) {
        continue
      }
      patch[key] = sub
    } else if (cur != des) {
      patch[key] = des
    }
  }

  for (key in current.keys) {
    if (key !in desired) {
      patch[key] = JsonNull
    }
  }

  return JsonObject(patch)
}
