package com.clerk.api.dpop

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

internal class ServerClock(private val localClock: Clock = Clock.System) : Clock {
  @Volatile private var offset: Duration = Duration.ZERO

  fun record(dateHeader: String?) {
    val serverTime = dateHeader?.let(::parseHttpDate) ?: return
    offset = serverTime - localClock.now()
  }

  override fun now(): Instant = localClock.now() + offset
}

private fun parseHttpDate(value: String): Instant? {
  val format =
    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("GMT")
      isLenient = false
    }
  val position = ParsePosition(0)
  val date = format.parse(value, position) ?: return null
  return if (position.index == value.length) Instant.fromEpochMilliseconds(date.time) else null
}
