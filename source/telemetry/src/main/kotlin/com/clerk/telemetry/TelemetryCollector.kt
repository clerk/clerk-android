package com.clerk.telemetry

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val DEFAULT_ENDPOINT_BASE_URL = "https://clerk-telemetry.com"

public class TelemetryCollector
private constructor(
  options: TelemetryCollectorOptions,
  private val transport: TelemetryTransport,
  private val environment: TelemetryEnvironment,
  private val throttler: TelemetryEventThrottler,
  private val json: Json,
  private val endpointBaseUrl: String,
) {

  /**
   * Creates a collector that posts events with [client]. When [client] is null, the collector
   * builds its own OkHttpClient off the main thread on the first flush.
   */
  public constructor(
    options: TelemetryCollectorOptions = TelemetryCollectorOptions(),
    environment: TelemetryEnvironment,
    throttler: TelemetryEventThrottler,
    json: Json = defaultJson(),
    endpointBaseUrl: String = DEFAULT_ENDPOINT_BASE_URL,
    client: OkHttpClient? = null,
  ) : this(
    options,
    OkHttpTelemetryTransport(client?.let { lazyOf(it) } ?: lazy { OkHttpClient() }),
    environment,
    throttler,
    json,
    endpointBaseUrl,
  )

  /**
   * Creates a collector that posts events with a Ktor [client].
   *
   * The SDK no longer depends on Ktor at runtime, so callers of this constructor must declare a
   * Ktor client dependency themselves.
   */
  @Deprecated(
    "The SDK posts telemetry with OkHttp and no longer ships Ktor. Pass an OkHttpClient instead.",
    ReplaceWith(
      "TelemetryCollector(options, environment, throttler, json, endpointBaseUrl, OkHttpClient())",
      "okhttp3.OkHttpClient",
    ),
  )
  public constructor(
    options: TelemetryCollectorOptions = TelemetryCollectorOptions(),
    client: HttpClient,
    environment: TelemetryEnvironment,
    throttler: TelemetryEventThrottler,
    json: Json = defaultJson(),
    endpointBaseUrl: String = DEFAULT_ENDPOINT_BASE_URL,
  ) : this(options, KtorTelemetryTransport(client), environment, throttler, json, endpointBaseUrl)

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

  public suspend fun record(raw: TelemetryEventRaw) {
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
      prepared.instanceType != DEVELOPMENT_INSTANCE_TYPE ->
        RecordResult(false, "non-development instance")
      environment.isDebugModeEnabled() -> RecordResult(false, "debug mode")
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
      scope.launch { flush() }
    }
  }

  public suspend fun flush() {
    val events = mutex.withLock {
      if (buffer.isEmpty()) return
      val copy = buffer.toList()
      buffer.clear()
      copy
    }

    val body = json.encodeToString(TelemetryEnvelope.serializer(), TelemetryEnvelope(events))
    try {
      transport.post("$endpointBaseUrl/v1/event", body)
    } catch (_: Exception) {}
  }

  private companion object {
    const val PERCENTAGE_MULTIPLIER = 100
    const val DEVELOPMENT_INSTANCE_TYPE = "development"

    fun defaultJson() = Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
    }
  }
}

internal fun interface TelemetryTransport {
  suspend fun post(url: String, jsonBody: String)
}

/**
 * Building an OkHttpClient initializes the TLS platform, so the default client is created lazily on
 * the IO dispatcher rather than when the UI composes its first telemetry provider.
 */
private class OkHttpTelemetryTransport(
  private val client: Lazy<OkHttpClient>,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TelemetryTransport {
  override suspend fun post(url: String, jsonBody: String) {
    val request = Request.Builder().url(url).post(jsonBody.toRequestBody(JSON_MEDIA_TYPE)).build()
    withContext(ioDispatcher) { client.value.newCall(request).execute().close() }
  }

  private companion object {
    val JSON_MEDIA_TYPE = "application/json".toMediaType()
  }
}

/**
 * Backs the deprecated Ktor constructor. Ktor is a compileOnly dependency, so this class is loaded
 * only when a caller that ships Ktor itself uses that constructor.
 */
private class KtorTelemetryTransport(private val client: HttpClient) : TelemetryTransport {
  override suspend fun post(url: String, jsonBody: String) {
    client.post(url) { setBody(TextContent(jsonBody, ContentType.Application.Json)) }
  }
}
