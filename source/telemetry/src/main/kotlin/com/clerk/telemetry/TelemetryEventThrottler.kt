package com.clerk.telemetry

/**
 * An interface for throttling telemetry events. Implementations of this interface can decide
 * whether a specific event should be sent or discarded based on custom logic, such as rate limiting
 * or event type filtering.
 */
public interface TelemetryEventThrottler {
  public suspend fun isEventThrottled(event: TelemetryEvent): Boolean
}
