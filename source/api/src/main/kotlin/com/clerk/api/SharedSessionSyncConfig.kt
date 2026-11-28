package com.clerk.api

/**
 * Enables Clerk authentication-state synchronization between sibling Android apps.
 *
 * Participating apps must use the same Clerk publishable key, include the Clerk API artifact, and
 * be signed with the same signing certificate. Clerk discovers matching sibling apps and shares
 * only its client, environment, and device-token state through a signature-checked content
 * provider.
 *
 * An app that enables shared session sync does not bind its client to a device key, because a
 * sibling app cannot sign with a key it does not hold. If an earlier release of the app ran without
 * shared session sync and bound the client, the first launch with it enabled drops that token, and
 * the user signs in again once.
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
public class SharedSessionSyncConfig private constructor() {
  public companion object {
    /** Enables shared-session synchronization for this Clerk instance. */
    public val enabled: SharedSessionSyncConfig = SharedSessionSyncConfig()
  }
}
