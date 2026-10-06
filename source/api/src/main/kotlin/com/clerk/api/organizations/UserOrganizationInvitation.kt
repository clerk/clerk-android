package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.EpochMillisecondsSerializer
import com.clerk.api.user.currentSessionId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
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
