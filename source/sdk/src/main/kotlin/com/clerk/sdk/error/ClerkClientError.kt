package com.clerk.sdk.error

/** Represents an error that occurs when the Clerk client encounters an issue. */
data class ClerkClientError(override val message: String? = null) : Throwable(message)
