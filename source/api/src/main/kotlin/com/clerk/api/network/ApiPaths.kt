package com.clerk.api.network

/**
 * Internal object containing all API endpoint path constants for the Clerk REST API.
 *
 * All paths are relative to the API base URL and version prefix. The paths are used by Retrofit
 * interfaces to define REST API endpoints.
 *
 * This is an internal utility and should not be used outside the Clerk SDK.
 */
internal object ApiPaths {

  /** Client-related API endpoints */
  internal object Client {
    internal const val BASE = "client"

    /** Device attestation endpoints */
    internal object DeviceAttestation {
      internal const val BASE = "${Client.BASE}/device_attestation"
      internal const val CHALLENGES = "${BASE}/challenges"
      internal const val VERIFY = "${Client.BASE}/verify"
    }

    /** Session management endpoints */
    internal object Sessions {
      internal const val BASE = "${Client.BASE}/sessions"
      internal const val WITH_ID = "${BASE}/{id}"
      internal const val REMOVE = "${WITH_ID}/remove"
      internal const val TOKENS = "${WITH_ID}/tokens"
      internal const val TOKEN_TEMPLATE = "${TOKENS}/{template}"
      internal const val SET_ACTIVE = "${WITH_ID}/touch"
    }

    /** Sign-in endpoints */
    internal object SignIn {
      internal const val BASE = "${Client.BASE}/sign_ins"
      internal const val WITH_ID = "${BASE}/{id}"
      internal const val ATTEMPT_FIRST_FACTOR = "${WITH_ID}/attempt_first_factor"
      internal const val ATTEMPT_SECOND_FACTOR = "${WITH_ID}/attempt_second_factor"
      internal const val PREPARE_FIRST_FACTOR = "${WITH_ID}/prepare_first_factor"
      internal const val PREPARE_SECOND_FACTOR = "${WITH_ID}/prepare_second_factor"
      internal const val RESET_PASSWORD = "${WITH_ID}/reset_password"
    }

    /** Sign-up endpoints */
    internal object SignUp {
      internal const val BASE = "${Client.BASE}/sign_ups"
      internal const val WITH_ID = "${BASE}/{id}"
      internal const val PREPARE_VERIFICATION = "${WITH_ID}/prepare_verification"
      internal const val ATTEMPT_VERIFICATION = "${WITH_ID}/attempt_verification"
    }
  }

  /** Environment configuration endpoint */
  internal const val ENVIRONMENT = "environment"

  /** User-related endpoints */
  internal object User {
    internal const val BASE = "me"
    internal const val PROFILE_IMAGE = "${BASE}/profile_image"
    internal const val ACCEPT_ORG_INVITATION =
      "${BASE}/organization_invitations/{invitation_id}/accept"
    internal const val ACCEPT_ORG_SUGGESTION =
      "${BASE}/organization_suggestions/{suggestion_id}/accept"
    internal const val BACKUP_CODES = "${BASE}/backup_codes"

    /** Password management */
    internal object Password {
      internal const val UPDATE = "${User.BASE}/change_password"
      internal const val DELETE = "${User.BASE}/remove_password"
    }

    /** Session management */
    internal object Sessions {
      internal const val BASE = "${User.BASE}/sessions"
      internal const val REVOKE = "${BASE}/{session_id}/revoke"
      internal const val ACTIVE = "${BASE}/active"
    }

    /** Email address management */
    internal object EmailAddress {
      internal const val BASE = "${User.BASE}/email_addresses"
      internal const val WITH_ID = "${BASE}/{email_id}"
      internal const val ATTEMPT_VERIFICATION = "${WITH_ID}/attempt_verification"
      internal const val PREPARE_VERIFICATION = "${WITH_ID}/prepare_verification"
    }

    /** Phone number management */
    internal object PhoneNumber {
      internal const val BASE = "${User.BASE}/phone_numbers"
      internal const val WITH_ID = "${BASE}/{phone_number_id}"
      internal const val ATTEMPT_VERIFICATION = "${WITH_ID}/attempt_verification"
      internal const val PREPARE_VERIFICATION = "${WITH_ID}/prepare_verification"
    }

    /** Passkey management */
    internal object Passkey {
      internal const val BASE = "${User.BASE}/passkeys"
      internal const val WITH_ID = "${BASE}/{passkey_id}"
      internal const val ATTEMPT_VERIFICATION = "${WITH_ID}/attempt_verification"
    }

    /** External account management */
    internal object ExternalAccount {
      internal const val BASE = "${User.BASE}/external_accounts"
      internal const val WITH_ID = "${BASE}/{external_account_id}"
      internal const val REAUTHORIZE = "${WITH_ID}/reauthorize"
      internal const val REVOKE_TOKENS = "${WITH_ID}/tokens"
    }

    /** TOTP management */
    internal object TOTP {
      internal const val BASE = "${User.BASE}/totp"
      internal const val ATTEMPT_VERIFICATION = "${BASE}/attempt_verification"
    }
  }

  /** Organization endpoints */
  internal object Organization {
    internal const val BASE = "organizations"
    internal const val WITH_ID = "${BASE}/{organization_id}"
    internal const val LOGO = "${WITH_ID}/logo"
    internal const val ROLES = "${WITH_ID}/roles"

    /** Domain management */
    internal object Domain {
      internal const val BASE = "${Organization.WITH_ID}/domains"
      internal const val WITH_ID = "${BASE}/{domain_id}"
      internal const val UPDATE_ENROLLMENT_MODE = "${WITH_ID}/update_enrollment_mode"
      internal const val PREPARE_AFFILIATION = "${WITH_ID}/prepare_affiliation_verification"
      internal const val ATTEMPT_AFFILIATION = "${WITH_ID}/attempt_affiliation_verification"
    }
  }
}
