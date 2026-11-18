package com.clerk.api.network.serialization

private const val JSON_INPUT_MARKER = "JSON input:"
private val decodeFailurePath = Regex("""\bat (?:path|element): (\$[\w.\[\]]*)""")

/**
 * kotlinx.serialization appends an excerpt of the JSON input, or the whole element on the tree
 * decoding path, to its decoding exceptions, and quotes a literal it could not parse before the
 * locator. The input here can be a client with session JWTs or personal data, so logs name only the
 * exception class and the locator kotlinx adds last, read after the input excerpt is dropped.
 */
internal fun Throwable.decodeFailureSummary(): String {
  val name = this::class.simpleName ?: "DecodeFailure"
  val path =
    message
      ?.substringBefore(JSON_INPUT_MARKER)
      ?.let { decodeFailurePath.findAll(it).lastOrNull()?.groupValues?.get(1) }
      ?.trimEnd('.', ',')
  return if (path == null) name else "$name at path: $path"
}
