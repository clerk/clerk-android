package com.clerk.telemetry

import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonObject

class DefaultTelemetryCollector(private val options: TelemetryCollectorOptions) :
  TelemetryCollector {

  companion object {
    private const val KEY_SAMPLE_LIMIT = 16
    private const val DEFAULT_ENDPOINT = "https://clerk-telemetry.com"
    private const val DEFAULT_SAMPLING_RATE = 1.0
    private const val DEFAULT_MAX_BUFFER_SIZE = 5
  }

  private data class Config(
    val samplingRate: Double,
    val disabled: Boolean,
    val debug: Boolean,
    val maxBufferSize: Int,
    val endpoint: String,
  )

  private data class Metadata(
    var clerkVersion: String = "",
    var sdk: String = "",
    var sdkVersion: String = "",
    var publishableKey: String = "",
    var secretKey: String? = null,
    var instanceType: InstanceType = InstanceType.Development,
  )

  private val config =
    Config(
      samplingRate = options.samplingRate ?: DEFAULT_SAMPLING_RATE,
      disabled = options.disabled,
      debug = options.debug,
      maxBufferSize = options.maxBufferSize ?: DEFAULT_MAX_BUFFER_SIZE,
      endpoint = options.endpoint ?: DEFAULT_ENDPOINT,
    )

  private val metadata = Metadata()
  private val throttler = TelemetryEventThrottler()
  private val buffer = mutableListOf<TelemetryEvent>()
  private var pendingFlushJob: Job? = null

  private val scope: CoroutineScope =
    options.scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

  init {
    metadata.clerkVersion = options.clerkVersion ?: ""
    metadata.sdk = options.sdk
    metadata.sdkVersion = options.sdkVersion
    metadata.publishableKey = options.publishableKey ?: ""

    parsePublishableKey(options.publishableKey)?.let { metadata.instanceType = it.instanceType }

    options.secretKey?.let { metadata.secretKey = it.take(KEY_SAMPLE_LIMIT) }
  }

  override val isEnabled: Boolean
    get() {
      if (metadata.instanceType != InstanceType.Development) return false
      if (config.disabled || isTruthyEnv(options.env, "CLERK_TELEMETRY_DISABLED")) return false
      return true
    }

  override val isDebug: Boolean
    get() = config.debug || isTruthyEnv(options.env, "CLERK_TELEMETRY_DEBUG")

  override fun record(event: TelemetryEventRaw) {
    val prepared = preparePayload(event.event, event.payload)

    if (isDebug) {
      options.logger?.logDebug("[clerk/telemetry] ${prepared.event} -> $prepared")
    }

    if (!shouldRecord(prepared, event.eventSamplingRate)) return

    buffer += prepared
    scheduleFlush()
  }

  private fun shouldRecord(preparedPayload: TelemetryEvent, eventSamplingRate: Double?): Boolean {
    return isEnabled && !isDebug && shouldBeSampled(preparedPayload, eventSamplingRate)
  }

  /**
   * Determines whether a telemetry event should be sampled based on several criteria.
   *
   * An event is sampled if it passes all of the following checks:
   * 1. It is not throttled (i.e., the same event hasn't occurred too frequently).
   * 2. It passes the base sampling rate check defined in the collector's configuration.
   * 3. It passes the event-specific sampling rate check, if one is provided.
   *
   * Both sampling checks are performed against the same random seed to ensure consistency.
   *
   * @param preparedPayload The fully constructed telemetry event to evaluate.
   * @param eventSamplingRate An optional, per-event sampling rate (between 0.0 and 1.0).
   * @return `true` if the event should be sampled and recorded, `false` otherwise.
   */
  private fun shouldBeSampled(
    preparedPayload: TelemetryEvent,
    eventSamplingRate: Double?,
  ): Boolean {
    val seed = Random.nextDouble()

    if (throttler.isEventThrottled(preparedPayload)) return false

    val basePass = seed <= config.samplingRate
    val eventPass = eventSamplingRate?.let { seed <= it } ?: true

    return basePass && eventPass
  }

  /**
   * Schedules a flush of the telemetry event buffer.
   *
   * A flush is triggered in one of two ways:
   * 1. **Immediately:** If the buffer size reaches or exceeds the `maxBufferSize`, any pending
   *    scheduled flush is cancelled and a new one is executed right away.
   * 2. **Debounced:** If the buffer is not full and no flush is currently scheduled, a new flush is
   *    scheduled to run in the near future (after the current coroutine suspension point). This
   *    helps batch events that arrive in quick succession without triggering a flush for each one.
   *
   * This prevents multiple flush operations from being scheduled simultaneously.
   */
  private fun scheduleFlush() {
    if (buffer.size >= config.maxBufferSize) {
      pendingFlushJob?.cancel()
      pendingFlushJob = null
      flush()
      return
    }

    if (pendingFlushJob != null) return

    pendingFlushJob =
      scope.launch {
        try {
          yield()
          flush()
        } finally {
          pendingFlushJob = null
        }
      }
  }

  /**
   * Sends all buffered telemetry events to the telemetry endpoint.
   *
   * This function clears the internal buffer after sending the events. It operates asynchronously
   * in a new coroutine. If the HTTP request fails, the failure is logged if debug mode is enabled,
   * but the events are not re-buffered.
   */
  private fun flush() {
    if (buffer.isEmpty()) return

    val eventsToSend = buffer.toList()
    buffer.clear()

    scope.launch {
      runCatching {
          val batch = TelemetryEventBatch(events = eventsToSend)
          options.httpClient.postEvents(
            url = "${config.endpoint.trimEnd('/')}/v1/event",
            batch = batch,
            headers = mapOf("Content-Type" to "application/json"),
          )
        }
        .onFailure {
          if (isDebug) {
            options.logger?.logDebug("[clerk/telemetry] Flush failed: $it")
          }
        }
    }
  }

  /**
   * Constructs a [TelemetryEvent] by combining the provided event name and payload with the
   * collector's metadata. This prepares the final event object that will be buffered and eventually
   * sent to the telemetry service.
   *
   * @param event The name of the telemetry event (e.g., "request-start").
   * @param payload A [JsonObject] containing the specific data for this event.
   * @return A fully constructed [TelemetryEvent] instance ready for processing.
   */
  private fun preparePayload(event: String, payload: JsonObject): TelemetryEvent {
    val instanceTypeString = metadata.instanceType.name.lowercase()

    return TelemetryEvent(
      event = event,
      cv = metadata.clerkVersion,
      it = instanceTypeString,
      sdk = metadata.sdk,
      sdkv = metadata.sdkVersion,
      pk = metadata.publishableKey.ifEmpty { null },
      sk = metadata.secretKey,
      payload = payload,
    )
  }
}
