package com.clerk.api

/**
 * Marks APIs that exist for Clerk's own framework integrations (for example the Expo SDK).
 *
 * These APIs are supported for that purpose but are not part of the SDK's stable public surface:
 * they can change shape or disappear in minor releases as integration needs evolve. This is the
 * Android counterpart of the iOS SDK's `@_spi(FrameworkIntegration)` surface.
 */
@RequiresOptIn(
  level = RequiresOptIn.Level.ERROR,
  message =
    "This API exists for Clerk's own framework integrations (e.g. the Expo SDK) and may " +
      "change without notice in minor releases. Opt in only if you accept that contract.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION)
annotation class FrameworkIntegrationApi
