package com.clerk.api

/**
 * Enables Clerk authentication-state synchronization between sibling Android apps.
 *
 * Participating apps must use the same Clerk publishable key, include the Clerk API artifact, and
 * be signed with the same signing certificate. Clerk discovers matching sibling apps and shares
 * only its client, environment, and device-token state through a signature-checked content
 * provider.
 *
 * Example:
 * ```kotlin
 * Clerk.initialize(
 *   context = applicationContext,
 *   publishableKey = "pk_...",
 *   options = ClerkConfigurationOptions(sharedSessionSync = SharedSessionSyncConfig.enabled),
 * )
 * ```
 */
class SharedSessionSyncConfig private constructor() {
  companion object {
    /** Enables shared-session synchronization for this Clerk instance. */
    @JvmField val enabled = SharedSessionSyncConfig()
  }
}
