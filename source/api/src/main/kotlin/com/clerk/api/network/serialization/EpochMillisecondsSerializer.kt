package com.clerk.api.network.serialization

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal object EpochMillisecondsSerializer : KSerializer<Long> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("EpochMilliseconds", PrimitiveKind.LONG)

  override fun deserialize(decoder: Decoder): Long {
    val element = (decoder as? JsonDecoder)?.decodeJsonElement()
    val primitive = element as? JsonPrimitive
    val content = primitive?.contentOrNull.orEmpty()

    return content.toLongOrNull()?.let(::normalizeEpochTimestamp)
      ?: parseIsoTimestampMilliseconds(content)
  }

  override fun serialize(encoder: Encoder, value: Long) {
    encoder.encodeLong(value)
  }

  private fun normalizeEpochTimestamp(timestamp: Long): Long {
    return if (timestamp > SECONDS_TO_MILLIS_BOUNDARY) {
      timestamp
    } else {
      timestamp * MILLIS_PER_SECOND
    }
  }

  private fun parseIsoTimestampMilliseconds(content: String): Long {
    val normalizedContent = normalizeFractionalSeconds(content)

    return ISO_8601_PATTERNS.firstNotNullOfOrNull { pattern ->
      parseWithPattern(normalizedContent, pattern)
    } ?: throw SerializationException("Unable to parse timestamp: $content")
  }

  private fun parseWithPattern(content: String, pattern: String): Long? {
    val position = ParsePosition(0)
    val date =
      SimpleDateFormat(pattern, Locale.US)
        .apply {
          isLenient = false
          timeZone = UTC
        }
        .parse(content, position)

    return date?.time.takeIf { position.index == content.length }
  }

  private fun normalizeFractionalSeconds(content: String): String {
    var normalizedContent = content
    val decimalIndex = content.indexOf(DECIMAL_SEPARATOR)
    if (decimalIndex != INDEX_NOT_FOUND) {
      val fractionStart = decimalIndex + 1
      var fractionEnd = fractionStart
      while (fractionEnd < content.length && content[fractionEnd].isDigit()) {
        fractionEnd += 1
      }

      val fraction = content.substring(fractionStart, fractionEnd)
      if (fraction.isNotEmpty()) {
        val milliseconds = fraction.take(MILLISECONDS_DIGITS).padEnd(MILLISECONDS_DIGITS, '0')
        normalizedContent = buildString {
          append(content.substring(0, fractionStart))
          append(milliseconds)
          append(content.substring(fractionEnd))
        }
      }
    }

    return normalizedContent
  }

  private val UTC: TimeZone = TimeZone.getTimeZone("UTC")
  private val ISO_8601_PATTERNS =
    listOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSSX",
      "yyyy-MM-dd'T'HH:mm:ssX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd'T'HH:mm:ssXXX",
    )

  private const val DECIMAL_SEPARATOR = '.'
  private const val INDEX_NOT_FOUND = -1
  private const val MILLISECONDS_DIGITS = 3
  private const val MILLIS_PER_SECOND = 1_000L
  private const val SECONDS_TO_MILLIS_BOUNDARY = 9_999_999_999L
}
