package com.clerk.api.contract

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonNamingStrategy

internal enum class FindingKind(val label: String) {
  MISSING_IN_SDK("missing-in-sdk"),
  MISSING_IN_SPEC("missing-in-spec"),
  REQUIRED_ONLY_IN_SDK("required-only-in-sdk"),
  ENUM_VALUE_MISSING_IN_SDK("enum-value-missing-in-sdk"),
}

internal data class Finding(
  val model: String,
  val kind: FindingKind,
  val key: String,
  val value: String? = null,
) {
  val line: String
    get() = "$model|${kind.label}|$key" + value?.let { "=$it" }.orEmpty()
}

@OptIn(ExperimentalSerializationApi::class)
internal object ContractDiff {
  private const val OBJECT_KEY = "object"

  fun findings(descriptor: SerialDescriptor, spec: Map<String, SpecProperty>): List<Finding> {
    val model = modelName(descriptor)
    val elements =
      (0 until descriptor.elementsCount)
        .associateBy { jsonKey(descriptor, it) }
        .filterKeys { it != OBJECT_KEY }
    val missingInSdk =
      (spec.keys - elements.keys).map { Finding(model, FindingKind.MISSING_IN_SDK, it) }
    val missingInSpec =
      (elements.keys - spec.keys).map { Finding(model, FindingKind.MISSING_IN_SPEC, it) }
    val shared =
      elements
        .filterKeys { it in spec }
        .flatMap { (key, index) ->
          sharedFindings(model, descriptor, index, key, spec.getValue(key))
        }
    return (missingInSdk + missingInSpec + shared).sortedBy { it.line }
  }

  fun modelName(descriptor: SerialDescriptor): String =
    descriptor.serialName.removeSuffix("?").removePrefix("com.clerk.api.")

  fun jsonKey(descriptor: SerialDescriptor, index: Int): String =
    JsonNamingStrategy.SnakeCase.serialNameForJson(
      descriptor,
      index,
      descriptor.getElementName(index),
    )

  fun jvmClass(serialName: String): Class<*>? {
    val parts = serialName.removeSuffix("?").split(".")
    return (parts.size downTo 1).firstNotNullOfOrNull { split ->
      val outer = parts.take(split).joinToString(".")
      val nested = parts.drop(split).joinToString("") { "$$it" }
      runCatching { Class.forName(outer + nested) }.getOrNull()
    }
  }

  private fun sharedFindings(
    model: String,
    descriptor: SerialDescriptor,
    index: Int,
    key: String,
    property: SpecProperty,
  ): List<Finding> {
    val element = descriptor.getElementDescriptor(index)
    val optional = descriptor.isElementOptional(index)
    val decodeCanFail =
      !optional && (!property.required || (property.nullable && !element.isNullable))
    val required =
      if (decodeCanFail) listOf(Finding(model, FindingKind.REQUIRED_ONLY_IN_SDK, key))
      else emptyList()
    val known = enumValues(itemOf(element))
    val enumGaps =
      if (known == null) emptyList()
      else
        (property.enumValues - known).map {
          Finding(model, FindingKind.ENUM_VALUE_MISSING_IN_SDK, key, it)
        }
    return required + enumGaps
  }

  private fun itemOf(element: SerialDescriptor): SerialDescriptor =
    if (element.kind == StructureKind.LIST) element.getElementDescriptor(0) else element

  private fun enumValues(element: SerialDescriptor): Set<String>? =
    when (element.kind) {
      SerialKind.ENUM -> element.elementNames.toSet()
      PrimitiveKind.STRING -> jvmClass(element.serialName)?.let(::wireValuesOfFallbackEnum)
      else -> null
    }

  private fun wireValuesOfFallbackEnum(type: Class<*>): Set<String>? =
    type.enumConstants
      ?.map { constant ->
        val name = (constant as Enum<*>).name
        type.getField(name).getAnnotation(SerialName::class.java)?.value ?: name
      }
      ?.toSet()
}
