package com.cerk.clerkserializer

import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Represents a status code in a 4xx or 5xx response. Retrieve it from Retrofit annotations via
 * [statusCode].
 *
 * This API should be considered read-only.
 */
@Retention(RUNTIME) public annotation class StatusCode(public val value: Int)
