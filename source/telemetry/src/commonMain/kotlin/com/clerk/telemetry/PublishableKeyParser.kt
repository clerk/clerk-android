package com.clerk.telemetry

import kotlin.io.encoding.Base64

private const val PUBLISHABLE_KEY_LIVE_PREFIX = "pk_live"
private const val PUBLISHABLE_KEY_TEST_PREFIX = "pk_test"
private const val PUBLISHABLE_KEY_PARTS_COUNT = 3
private const val PUBLISHABLE_KEY_ENCODED_PART_INDEX = 2

data class ParsePublishableKeyOptions(
  val fatal: Boolean = false,
  val domain: String? = null,
  val proxyUrl: String? = null,
  val isSatellite: Boolean = false,
)

fun parsePublishableKey(
  key: String?,
  options: ParsePublishableKeyOptions = ParsePublishableKeyOptions(),
): PublishableKey? {
  val k = key.orEmpty()

  var result: PublishableKey? = null

  if (isPublishableKey(k)) {
    val instanceType = instanceTypeFor(k)
    val encodedPart = k.split("_").getOrNull(2).orEmpty()
    val decoded = decodeOrFail(encodedPart, options)

    if (decoded != null) {
      if (isValidDecodedPublishableKey(decoded)) {
        val baseFrontendApi = decoded.dropLast(1)
        val frontendApi = overrideFrontendApi(baseFrontendApi, instanceType, options)
        result = PublishableKey(instanceType = instanceType, frontendApi = frontendApi)
      } else {
        handleInvalidDecoded(options)
      }
    } // else: decode failed and either errored (fatal) or we return null
  } else {
    handleInvalidKey(options, k)
  }

  return result
}

fun isPublishableKey(key: String = ""): Boolean {
  var isValid =
    key.startsWith(PUBLISHABLE_KEY_LIVE_PREFIX) || key.startsWith(PUBLISHABLE_KEY_TEST_PREFIX)

  if (isValid) {
    val parts = key.split("_")
    if (parts.size == PUBLISHABLE_KEY_PARTS_COUNT) {
      val encodedPart = parts[PUBLISHABLE_KEY_ENCODED_PART_INDEX]
      val decoded = base64Decode(encodedPart)
      isValid = decoded?.let { isValidDecodedPublishableKey(it) } ?: false
    } else {
      isValid = false
    }
  }

  return isValid
}

private fun isValidDecodedPublishableKey(decoded: String): Boolean {
  return decoded.endsWith("_") && decoded.length >= 2
}

fun base64Decode(input: String): String? {
  return runCatching { Base64.decode(input).decodeToString() }.getOrNull()
}

private fun handleInvalidKey(options: ParsePublishableKeyOptions, k: String): PublishableKey? {
  if (!options.fatal) return null

  if (k.isEmpty()) {
    error(
      """
      Publishable key is missing. Ensure that your publishable key is correctly configured.
      Double-check your environment configuration or retrieve keys here: 
      https://dashboard.clerk.com/last-active?path=api-keys
      """
        .trimIndent()
    )
  } else {
    error("Publishable key not valid.")
  }
}

private fun decodeOrFail(encodedPart: String, options: ParsePublishableKeyOptions): String? {
  return base64Decode(encodedPart)
    ?: if (options.fatal) error("Publishable key not valid: Failed to decode key.") else null
}

private fun handleInvalidDecoded(options: ParsePublishableKeyOptions): PublishableKey? {
  if (options.fatal) error("Publishable key not valid: Decoded key has invalid format.")
  return null
}

private fun instanceTypeFor(k: String): InstanceType =
  if (k.startsWith(PUBLISHABLE_KEY_LIVE_PREFIX)) InstanceType.Production
  else InstanceType.Development

private fun overrideFrontendApi(
  current: String,
  instanceType: InstanceType,
  options: ParsePublishableKeyOptions,
): String {
  return when {
    options.proxyUrl != null -> options.proxyUrl
    instanceType == InstanceType.Production && options.domain != null && options.isSatellite ->
      "clerk.${options.domain}"

    else -> current
  }
}
