package com.clerk.telemetry

import com.clerk.api.log.ClerkLog
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class TelemetryCollector(
  options: TelemetryCollectorOptions = TelemetryCollectorOptions(),
  private val client: HttpClient,
  private val environment: TelemetryEnvironment,
  private val throttler: TelemetryEventThrottler,
  private val json: Json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
  },
  private val endpointBaseUrl: String = "https://clerk-telemetry.com",
) {

  @Serializable private data class TelemetryEnvelope(val events: List<TelemetryEvent>)

  private data class RecordResult(val shouldRecord: Boolean, val reason: String)

  private data class Config(
    val samplingRate: Double,
    val maxBufferSize: Int,
    val flushIntervalMillis: Long,
    val disableThrottling: Boolean,
  )

  private data class Metadata(val sdk: String, val sdkVersion: String)

  private val config =
    Config(
      samplingRate = options.samplingRate,
      maxBufferSize = options.normalizedMaxBufferSize,
      flushIntervalMillis = options.normalizedFlushIntervalSeconds * 1000L,
      disableThrottling = options.disableThrottling,
    )

  private val metadata = Metadata(sdk = environment.sdkName, sdkVersion = environment.sdkVersion)

  private val scope = CoroutineScope(SupervisorJob())
  private val mutex = Mutex()
  private val buffer = mutableListOf<TelemetryEvent>()
  private var flushJobActive = false

  init {
    startPeriodicFlushing()
  }

  suspend fun record(raw: TelemetryEventRaw) {
    val prepared = preparePayload(raw.event, raw.payload)
    val recordResult = shouldRecord(prepared, raw.eventSamplingRate)

    // TODO hook into your logger if you want parity with ClerkLogger.debug
    if (!recordResult.shouldRecord) {
      // e.g. log "[telemetry][skipped - ${recordResult.reason}] ${prepared.event}"
      return
    }

    mutex.withLock { buffer += prepared }

    scheduleFlushIfNeeded()
  }

  private suspend fun preparePayload(
    event: String,
    payload: Map<String, kotlinx.serialization.json.JsonElement>,
  ): TelemetryEvent {
    val instanceType = environment.instanceTypeString()
    val pk = environment.publishableKey()
    return TelemetryEvent(
      event = event,
      instanceType = instanceType,
      sdkName = metadata.sdk,
      sdkVersion = metadata.sdkVersion,
      publishableKey = pk,
      payload = payload,
    )
  }

  private suspend fun shouldRecord(
    prepared: TelemetryEvent,
    eventSamplingRate: Double?,
  ): RecordResult {
    return when {
      !environment.isTelemetryEnabled() -> RecordResult(false, "telemetry disabled")
      environment.instanceTypeString() != "development" ->
        RecordResult(false, "production instance")

      else -> shouldBeSampled(prepared, eventSamplingRate)
    }
  }

  private suspend fun shouldBeSampled(
    prepared: TelemetryEvent,
    eventSamplingRate: Double?,
  ): RecordResult {
    if (config.disableThrottling) {
      return RecordResult(true, "throttling disabled")
    }

    val seed = Random.nextDouble(0.0, 1.0)
    val globalOk = seed <= config.samplingRate
    val eventOk = eventSamplingRate?.let { seed <= it } ?: true

    val globalSamplingPercent = (config.samplingRate * PERCENTAGE_MULTIPLIER).toInt()
    val eventSamplingPercent = eventSamplingRate?.let { (it * PERCENTAGE_MULTIPLIER).toInt() }

    return when {
      !globalOk -> RecordResult(false, "global sampling ($globalSamplingPercent%)")
      !eventOk && eventSamplingRate != null ->
        RecordResult(false, "event sampling ($eventSamplingPercent%)")

      throttler.isEventThrottled(prepared) -> RecordResult(false, "throttled")
      else -> RecordResult(true, "accepted")
    }
  }

  private fun startPeriodicFlushing() {
    if (flushJobActive) return
    flushJobActive = true

    scope.launch {
      while (isActive) {
        delay(config.flushIntervalMillis)
        if (hasBufferedEvents()) {
          flush()
        }
      }
    }
  }

  private suspend fun hasBufferedEvents(): Boolean = mutex.withLock { buffer.isNotEmpty() }

  private suspend fun scheduleFlushIfNeeded() {
    val shouldFlush = mutex.withLock { buffer.size >= config.maxBufferSize }
    if (shouldFlush) {
      // fire-and-forget
      scope.launch { flush() }
    }
  }

  suspend fun flush() {
    val events =
      mutex.withLock {
        if (buffer.isEmpty()) return
        val copy = buffer.toList()
        buffer.clear()
        copy
      }

    val envelope = TelemetryEnvelope(events)
    try {
      client
        .post("$endpointBaseUrl/v1/event") {
          contentType(ContentType.Application.Json)
          setBody(envelope)
        }
        .body<Unit>()
    } catch (e: Exception) {
      ClerkLog.e("${e.message}")
    }
  }

  private companion object {
    const val PERCENTAGE_MULTIPLIER = 100
  }
}
