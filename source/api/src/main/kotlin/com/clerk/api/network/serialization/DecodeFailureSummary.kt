package com.clerk.api.network.serialization

private val decodeFailurePath = Regex("""\bat (?:path|element): (\S+)""")

/**
 * kotlinx.serialization appends an excerpt of the JSON input, or the whole element on the tree
 * decoding path, to its decoding exceptions. The input here can be a client with session JWTs or
 * personal data, so logs name only the exception class and the path that failed.
 */
internal fun Throwable.decodeFailureSummary(): String {
  val name = this::class.simpleName ?: "DecodeFailure"
  val path = message?.let { decodeFailurePath.find(it)?.groupValues?.get(1) }?.trimEnd('.', ',')
  return if (path == null) name else "$name at path: $path"
}
