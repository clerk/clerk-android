package com.clerk.api.network.serialization

/**
 * kotlinx.serialization puts the input into its decoding exceptions: an excerpt of the JSON, the
 * whole element on the tree decoding path, and literals it could not parse, any of which can hold
 * session JWTs or personal data and can imitate the path locator kotlinx appends. Logs therefore
 * name only the exception class.
 */
internal fun Throwable.decodeFailureSummary(): String = this::class.simpleName ?: "DecodeFailure"
