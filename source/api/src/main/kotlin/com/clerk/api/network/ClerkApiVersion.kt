package com.clerk.api.network

/**
 * Internal object containing API version constants for the Clerk REST API.
 *
 * This object centralizes the API version information used throughout the SDK to ensure consistency
 * in API endpoint URLs and version headers.
 *
 * This is an internal utility and should not be used outside the Clerk SDK.
 */
internal object ClerkApiVersion {
  /**
   * The current API version used by the Clerk SDK.
   *
   * This version string is appended to the base API URL to form complete endpoint URLs. For
   * example: `https://api.clerk.com/v1/client`
   */
  const val VERSION = "v1"
}
