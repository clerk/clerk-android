package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.currentSessionId
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class UserOrganizationInvitation(
  val id: String,
  val emailAddress: String,
  val publicOrganizationData: PublicOrganizationData,
  @Serializable(with = JsonElementStringSerializer::class) val publicMetadata: String,
  val role: String,
  val status: String, // "pending", "accepted", "revoked"
  @Serializable(with = EpochMillisecondsSerializer::class) val createdAt: Long,
  @Serializable(with = EpochMillisecondsSerializer::class) val updatedAt: Long,
) {

  @Serializable
  data class PublicOrganizationData(
    val hasImage: Boolean,
    val imageUrl: String?,
    val name: String,
    val id: String,
    val slug: String? = null,
  )
}

suspend fun UserOrganizationInvitation.accept():
  ClerkResult<UserOrganizationInvitation, ClerkErrorResponse> {
  return ClerkApi.user.acceptUserOrganizationInvitation(id, sessionId = currentSessionId())
}

private object EpochMillisecondsSerializer : KSerializer<Long> {
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
  private val ISO_8601_PATTERNS = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX")

  private const val DECIMAL_SEPARATOR = '.'
  private const val INDEX_NOT_FOUND = -1
  private const val MILLISECONDS_DIGITS = 3
  private const val MILLIS_PER_SECOND = 1_000L
  private const val SECONDS_TO_MILLIS_BOUNDARY = 9_999_999_999L
}

private object JsonElementStringSerializer : KSerializer<String> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("JsonElementString", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): String {
    val element: JsonElement? = (decoder as? JsonDecoder)?.decodeJsonElement()
    return (element as? JsonPrimitive)?.contentOrNull ?: element?.toString().orEmpty()
  }

  override fun serialize(encoder: Encoder, value: String) {
    encoder.encodeString(value)
  }
}
