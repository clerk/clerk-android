package com.clerk.telemetry

import kotlin.time.DurationUnit
import kotlin.time.TimeSource

private typealias TtlInMilliseconds = Long

private const val DEFAULT_CACHE_TTL_MS: TtlInMilliseconds = 86_400_000L // 24 hours

/**
 * Manages throttling for telemetry events using an in-memory cache to mitigate event flooding in
 * frequently executed code paths.
 *
 * This roughly mirrors the JS implementation that uses localStorage, but does not persist across
 * app restarts.
 */
class TelemetryEventThrottler(private val cacheTtlMs: TtlInMilliseconds = DEFAULT_CACHE_TTL_MS) {

  // key -> first-seen timestamp
  private val cache: MutableMap<String, TtlInMilliseconds> = mutableMapOf()

  private fun nowMillis(): Long =
    TimeSource.Monotonic.markNow().elapsedNow().toLong(DurationUnit.MILLISECONDS)

  /**
   * Returns true if this event should be throttled (i.e., we've seen an equivalent event within the
   * TTL window), false otherwise.
   */
  fun isEventThrottled(event: TelemetryEvent): Boolean {
    val now = nowMillis()
    val key = generateKey(event)
    val firstSeen = cache[key]

    val throttled: Boolean =
      if (firstSeen == null) {
        // Not seen before – store it and allow
        cache[key] = now
        false
      } else {
        val isExpired = now - firstSeen > cacheTtlMs
        if (isExpired) {
          // TTL expired – treat as new event, update timestamp
          cache[key] = now
          false
        } else {
          // Seen and still within TTL – throttle
          true
        }
      }

    return throttled
  }

  /**
   * Generates a consistent unique key for telemetry events by combining payload + event metadata
   * (excluding pk/sk), sorting by key, and then serializing the ordered values.
   *
   * This mirrors the JS logic: const { sk, pk, payload, ...rest } = event; const sanitized = {
   * ...payload, ...rest }; JSON.stringify(Object.keys(sanitized).sort().map(k => sanitized[k]))
   */
  private fun generateKey(event: TelemetryEvent): String {
    // Exclude secret/public keys from the throttling signature
    val baseMap =
      mutableMapOf<String, Any?>(
        "event" to event.event,
        "cv" to event.cv,
        "it" to event.it,
        "sdk" to event.sdk,
        "sdkv" to event.sdkv,
      )

    // Merge payload entries
    event.payload.forEach { (k, v) -> baseMap[k] = v }

    // Sort keys, take ordered values, stringify them deterministically
    val sortedValues = baseMap.toSortedMap().values.map { value -> value?.toString() ?: "null" }

    // Close enough to JSON.stringify([...]) for our purposes
    return sortedValues.joinToString(prefix = "[", postfix = "]", separator = ",")
  }
}
