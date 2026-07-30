package com.clerk.api.network.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Internal data class representing authentication configuration from the Clerk environment.
 *
 * This class contains authentication-related settings that control how the SDK handles user
 * sessions and authentication behavior.
 *
 * @property singleSessionMode Whether the application is configured for single session mode. When
 *   true, only one active session is allowed per user at a time.
 * @property sessionMinter Whether the instance is served by the session minter. When true, session
 *   token requests carry the previous session token so the edge can mint the next one.
 */
@Serializable
internal data class AuthConfig(
  /**
   * Whether the application is configured for single session mode. When true, only one active
   * session is allowed per user at a time.
   */
  @SerialName("single_session_mode") val singleSessionMode: Boolean,

  /**
   * Whether the instance is served by the session minter. When true, session token requests carry
   * the previous session token so the edge can mint the next one.
   */
  @SerialName("session_minter") val sessionMinter: Boolean = false,
)
