package com.clerk.api.network.model.environment

import com.clerk.api.log.ClerkLogger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder

@Serializable
internal data class NativeAppSettings(
  @SerialName("minimum_supported_version")
  val minimumSupportedVersion: MinimumSupportedVersion = MinimumSupportedVersion()
) {
  @Serializable
  internal data class MinimumSupportedVersion(
    @Serializable(with = AndroidPolicyListSerializer::class)
    val android: List<AndroidPolicy> = emptyList()
  )

  @Serializable
  internal data class AndroidPolicy(
    @SerialName("package_name") val packageName: String,
    @SerialName("minimum_version") val minimumVersion: String,
    @SerialName("update_url") val updateUrl: String? = null,
  )
}

internal object NativeAppSettingsOrDefaultSerializer : KSerializer<NativeAppSettings> {
  private val generated = NativeAppSettings.serializer()

  override val descriptor: SerialDescriptor = generated.descriptor

  override fun serialize(encoder: Encoder, value: NativeAppSettings) {
    generated.serialize(encoder, value)
  }

  override fun deserialize(decoder: Decoder): NativeAppSettings {
    val jsonDecoder = decoder as? JsonDecoder ?: return generated.deserialize(decoder)
    val element = jsonDecoder.decodeJsonElement()
    return try {
      jsonDecoder.json.decodeFromJsonElement(generated, element)
    } catch (e: SerializationException) {
      ClerkLogger.w("Ignoring unreadable native app settings: ${e.message}")
      NativeAppSettings()
    }
  }
}

internal object AndroidPolicyListSerializer : KSerializer<List<NativeAppSettings.AndroidPolicy>> {
  private val generated = ListSerializer(NativeAppSettings.AndroidPolicy.serializer())

  override val descriptor: SerialDescriptor = generated.descriptor

  override fun serialize(encoder: Encoder, value: List<NativeAppSettings.AndroidPolicy>) {
    generated.serialize(encoder, value)
  }

  override fun deserialize(decoder: Decoder): List<NativeAppSettings.AndroidPolicy> {
    val jsonDecoder = decoder as? JsonDecoder ?: return generated.deserialize(decoder)
    val elements = (jsonDecoder.decodeJsonElement() as? JsonArray).orEmpty()
    return elements.mapNotNull { element ->
      try {
        jsonDecoder.json.decodeFromJsonElement(
          NativeAppSettings.AndroidPolicy.serializer(),
          element,
        )
      } catch (e: SerializationException) {
        ClerkLogger.w("Ignoring unreadable Android minimum version: ${e.message}")
        null
      }
    }
  }
}
