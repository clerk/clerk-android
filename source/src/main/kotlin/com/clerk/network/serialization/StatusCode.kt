package com.clerk.network.serialization

import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Represents a status code in a 4xx or 5xx response. Retrieve it from Retrofit annotations via
 * [statusCode].
 *
 * This API should be considered read-only.
 */
@Retention(RUNTIME) internal annotation class StatusCode(public val value: Int)
