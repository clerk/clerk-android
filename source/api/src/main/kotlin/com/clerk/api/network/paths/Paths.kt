package com.clerk.api.network.paths

import com.clerk.api.Constants.Paths.SESSIONS_WITH_ID
import com.clerk.api.Constants.Paths.SIGN_UP_WITH_ID

/**
 * Internal object containing API endpoint path constants for the Clerk REST API.
 *
 * This object organizes API paths into logical groups based on the resource they operate on. All
 * paths are relative to the API base URL and version prefix. The paths are used by Retrofit
 * interfaces to define REST API endpoints.
 *
 * This is an internal utility and should not be used outside the Clerk SDK.
 */
@Suppress("MemberNameEqualsClassName")
internal object Paths {
  /**
   * Client-related API endpoints.
   *
   * These paths handle operations on the client object, which represents the current
   * device/application instance and contains session and authentication state.
   */
  internal object ClientPath {
    /** Base path for client operations */
    internal const val CLIENT = "client"

    /**
     * Device attestation related endpoints.
     *
     * These paths handle device attestation operations for enhanced security verification.
     */
    internal object DeviceAttestation {
      const val DEVICE_ATTESTATION = "${CLIENT}/device_attestation"
      const val CHALLENGES = "${DEVICE_ATTESTATION}/challenges"
      const val VERIFY = "$CLIENT/verify"
    }

    /**
     * Session management endpoints within the client context.
     *
     * These paths handle operations on sessions associated with the current client.
     */
    internal object Sessions {
      internal const val SESSIONS = "${CLIENT}/sessions"

      /** Session operations that require a specific session ID. */
      internal object WithId {
        internal const val REMOVE = "${SESSIONS_WITH_ID}/remove"
        internal const val TOKENS = "${SESSIONS_WITH_ID}/tokens"
        internal const val TEMPLATE = "${TOKENS}/{template}"
      }
    }

    /**
     * Sign-in related endpoints within the client context.
     *
     * These paths handle the sign-in flow, including factor verification and password reset.
     */
    internal object SignInPath {
      internal const val SIGN_INS = "${CLIENT}/sign_ins"

      /** Sign-in operations that require a specific sign-in ID. */
      internal object WithId {
        internal const val SIGN_INS_WITH_ID = "${SIGN_INS}/{id}"

        internal const val ATTEMPT_FIRST_FACTOR = "${SIGN_INS_WITH_ID}/attempt_first_factor"
        internal const val ATTEMPT_SECOND_FACTOR = "${SIGN_INS_WITH_ID}/attempt_second_factor"
        internal const val PREPARE_FIRST_FACTOR = "${SIGN_INS_WITH_ID}/prepare_first_factor"
        internal const val PREPARE_SECOND_FACTOR = "${SIGN_INS_WITH_ID}/prepare_second_factor"
        internal const val RESET_PASSWORD = "${SIGN_INS_WITH_ID}/reset_password"
      }
    }
  }

  /**
   * Sign-up related endpoints.
   *
   * These paths handle user registration and sign-up verification flows.
   */
  internal object SignUpPath {
    const val SIGN_UP = "client/sign_ups"

    /** Sign-up operations that require a specific sign-up ID. */
    internal object WithId {
      const val UPDATE = SIGN_UP_WITH_ID
      const val PREPARE_VERIFICATION = "${SIGN_UP_WITH_ID}/prepare_verification"
      const val ATTEMPT_VERIFICATION = "${SIGN_UP_WITH_ID}/attempt_verification"
    }
  }

  /** Environment configuration endpoint */
  internal const val ENVIRONMENT = "environment"

  /**
   * User-related endpoints.
   *
   * These paths handle operations on the authenticated user's profile, settings, and associated
   * resources like email addresses, phone numbers, and sessions.
   */
  internal object UserPath {
    const val ME = "me"
    const val PROFILE_IMAGE = "${ME}/profile_image"

    /** Password management endpoints. */
    internal object Password {
      const val UPDATE = "${ME}/change_password"
      const val DELETE = "${ME}/remove_password"
    }

    /** Session management endpoints for the user. */
    internal object Sessions {
      const val SESSIONS = "${ME}/sessions"
      const val REVOKE = "${SESSIONS}/{session_id}/revoke"
      const val ACTIVE = "${SESSIONS}/active"
    }

    /** Email address management endpoints. */
    internal object EmailAddress {
      const val EMAIL_ADDRESSES = "${ME}/email_addresses"

      /** Email address operations that require a specific email address ID. */
      internal object WithId {
        const val EMAIL_ADDRESSES_WITH_ID = "${EMAIL_ADDRESSES}/{email_id}"
        const val ATTEMPT_VERIFICATION = "${EMAIL_ADDRESSES_WITH_ID}/attempt_verification"
        const val PREPARE_VERIFICATION = "${EMAIL_ADDRESSES_WITH_ID}/prepare_verification"
      }
    }

    /** Phone number management endpoints. */
    internal object PhoneNumbers {
      const val PHONE_NUMBERS = "${ME}/phone_numbers"

      /** Phone number operations that require a specific phone number ID. */
      internal object WithId {
        const val PHONE_NUMBERS_WITH_ID = "${PHONE_NUMBERS}/{phone_number_id}"
        const val ATTEMPT_VERIFICATION = "${PHONE_NUMBERS_WITH_ID}/attempt_verification"
        const val PREPARE_VERIFICATION = "${PHONE_NUMBERS_WITH_ID}/prepare_verification"
      }
    }

    /** Passkey management endpoints. */
    internal object Passkeys {
      const val PASSKEYS = "${ME}/passkeys"

      /** Passkey operations that require a specific passkey ID. */
      internal object WithId {
        const val PASSKEYS_WITH_ID = "${PASSKEYS}/{passkey_id}"
        const val ATTEMPT_VERIFICATION = "${PASSKEYS_WITH_ID}/attempt_verification"
      }
    }

    /** External account management endpoints. */
    internal object ExternalAccounts {
      const val EXTERNAL_ACCOUNTS = "${ME}/external_accounts"

      /** External account operations that require a specific external account ID. */
      internal object WithId {
        const val EXTERNAL_ACCOUNTS_WITH_ID = "${EXTERNAL_ACCOUNTS}/{external_account_id}"
        const val REAUTHORIZE = "${EXTERNAL_ACCOUNTS_WITH_ID}/reauthorize"
        const val REVOKE_TOKENS = "${EXTERNAL_ACCOUNTS_WITH_ID}/tokens"
      }
    }

    /** TOTP (Time-based One-Time Password) management endpoints. */
    internal object TOTP {
      const val TOTP = "${ME}/totp"
      const val ATTEMPT_VERIFICATION = "${TOTP}/attempt_verification"
    }

    /** Backup codes endpoint */
    const val BACKUP_CODES = "${ME}/backup_codes"
  }
}

/**
 * Common parameter names used in API paths and requests.
 *
 * This object contains constants for parameter names that are frequently used across different API
 * endpoints. Using these constants ensures consistency and reduces the risk of typos in parameter
 * names.
 */
internal object CommonParams {
  /** Parameter name for phone number ID */
  const val PHONE_NUMBER_ID = "phone_number_id"

  /** Parameter name for email address ID */
  const val EMAIL_ID = "email_id"

  /** Parameter name for session ID */
  const val SESSION_ID = "session_id"

  /** Parameter name for external account ID */
  const val EXTERNAL_ACCOUNT_ID = "external_account_id"

  /** Parameter name for passkey ID */
  const val PASSKEY_ID = "passkey_id"

  /** Parameter name for TOTP ID */
  const val TOTP_ID = "totp_id"

  /** Parameter name for authentication strategy */
  const val STRATEGY = "strategy"

  /** Parameter name for verification code */
  const val CODE = "code"

  /** Generic parameter name for ID */
  const val ID = "id"

  /** Parameter name for Clerk session ID in query parameters */
  const val CLERK_SESSION_ID = "_clerk_session_id"
}
