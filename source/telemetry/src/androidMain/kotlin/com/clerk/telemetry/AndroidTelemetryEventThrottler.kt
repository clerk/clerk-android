package com.clerk.telemetry

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class AndroidTelemetryEventThrottler(
  context: Context,
  private val json: Json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
  },
  private val cacheTtlMillis: Long = 24L * 60L * 60L * 1000L, // 24h
) : TelemetryEventThrottler {

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val mutex = Mutex()
  private var memoryCache: MutableMap<String, Long>? = null

  override suspend fun isEventThrottled(event: TelemetryEvent): Boolean =
    mutex.withLock {
      if (memoryCache == null) {
        memoryCache = loadCache().toMutableMap()
        cleanupExpiredEntries()
      }

      val now = System.currentTimeMillis()
      val key = generateKey(event)

      val lastSeen = memoryCache!![key]
      if (lastSeen == null || now - lastSeen > cacheTtlMillis) {
        memoryCache!![key] = now
        saveCache(memoryCache!!)
        return false
      }

      true
    }

  private fun generateKey(event: TelemetryEvent): String {
    val fields = mutableListOf<String>()
    fields += "event:${event.event}"
    fields += "it:${event.instanceType}"
    fields += "sdk:${event.sdkName}"
    fields += "sdkv:${event.sdkVersion}"
    event.publishableKey?.let { fields += "pk:$it" }

    val payloadKey = stableJsonString(event.payload)
    fields += "payload:$payloadKey"

    return fields.sorted().joinToString("|")
  }

  private fun stableJsonString(map: Map<String, JsonElement>): String {
    // Stable JSON string by sorting keys and re-encoding; fall back to {}
    return runCatching {
        json.encodeToString(
          MapSerializer(String.serializer(), JsonElement.serializer()),
          map.toSortedMap(),
        )
      }
      .getOrElse { "{}" }
  }

  private suspend fun loadCache(): Map<String, Long> =
    withContext(Dispatchers.IO) {
      val raw = prefs.getString(KEY_STORAGE, null) ?: return@withContext emptyMap()
      runCatching {
          json.decodeFromString(MapSerializer(String.serializer(), Long.serializer()), raw)
        }
        .getOrElse { emptyMap() }
    }

  private suspend fun saveCache(cache: Map<String, Long>) {
    withContext(Dispatchers.IO) {
      val encoded =
        json.encodeToString(MapSerializer(String.serializer(), Long.serializer()), cache)
      prefs.edit().putString(KEY_STORAGE, encoded).apply()
    }
  }

  private suspend fun cleanupExpiredEntries() {
    val now = System.currentTimeMillis()
    val cache = memoryCache ?: return
    val originalSize = cache.size
    val filtered = cache.filterValues { now - it <= cacheTtlMillis }.toMutableMap()
    if (filtered.size != originalSize) {
      memoryCache = filtered
      saveCache(filtered)
    }
  }

  companion object {
    private const val PREFS_NAME = "clerk_telemetry"
    private const val KEY_STORAGE = "clerk_telemetry_throttler"
  }
}
