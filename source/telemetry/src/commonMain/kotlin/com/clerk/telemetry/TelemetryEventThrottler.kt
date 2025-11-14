import com.clerk.telemetry.TelemetryEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class TelemetryEventThrottler(
  private val ttlMs: Long = 86_400_000L // 24 hours
) {
  private val timeSource = TimeSource.Monotonic

  // key -> time mark when event was first seen
  private val cache: MutableMap<String, TimeSource.Monotonic.ValueTimeMark> = mutableMapOf()

  fun isEventThrottled(event: TelemetryEvent): Boolean {
    val key = generateKey(event)
    val existingMark = cache[key]

    if (existingMark == null) {
      // First time ever seen
      cache[key] = timeSource.markNow()
      return false
    }

    val elapsed = existingMark.elapsedNow()

    return if (elapsed < ttlMs.milliseconds) {
      // Seen recently → throttle
      true
    } else {
      // TTL expired → update timestamp and allow
      cache[key] = timeSource.markNow()
      false
    }
  }

  private fun generateKey(event: TelemetryEvent): String {
    val baseMap =
      mutableMapOf<String, Any?>(
        "event" to event.event,
        "cv" to event.cv,
        "it" to event.it,
        "sdk" to event.sdk,
        "sdkv" to event.sdkv,
      )

    event.payload.forEach { (k, v) -> baseMap[k] = v }

    return baseMap.toSortedMap().values.joinToString(prefix = "[", postfix = "]", separator = ",") {
      it.toString()
    }
  }
}
