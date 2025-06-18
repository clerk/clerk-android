package com.clerk.network.paths

@Suppress("MemberNameEqualsClassName")
internal object Paths {
  internal object ClientPath {
    internal const val CLIENT = "client"

    internal object DeviceAttestation {
      const val DEVICE_ATTESTATION = "${CLIENT}/device_attestation"
      const val CHALLENGES = "${DEVICE_ATTESTATION}/challenges"
      const val VERIFY = "$CLIENT/verify"
    }

    internal object Sessions {
      internal const val SESSIONS = "${CLIENT}/sessions"

      internal object WithId {
        private const val SESSIONS_WITH_ID = "${SESSIONS}/{id}"

        internal const val REMOVE = "${SESSIONS_WITH_ID}/remove"
        internal const val TOKENS = "${SESSIONS_WITH_ID}/tokens"
        internal const val TEMPLATE = "${TOKENS}/{template}"
      }
    }

    internal object SignInPath {
      internal const val SIGN_INS = "${CLIENT}/sign_ins"

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

  internal object SignUpPath {
    const val SIGN_UP = "client/sign_ups"

    internal object WithId {
      private const val SIGN_UP_WITH_ID = "client/sign_ups/{id}"

      const val UPDATE = SIGN_UP_WITH_ID
      const val PREPARE_VERIFICATION = "${SIGN_UP_WITH_ID}/prepare_verification"
      const val ATTEMPT_VERIFICATION = "${SIGN_UP_WITH_ID}/attempt_verification"
    }
  }

  internal const val ENVIRONMENT = "environment"

  internal object UserPath {
    const val ME = "me"
    const val PROFILE_IMAGE = "${ME}/profile_image"

    internal object Password {
      const val UPDATE = "${ME}/change_password"
      const val DELETE = "${ME}/remove_password"
    }

    internal object Sessions {
      const val SESSIONS = "${ME}/sessions"
      const val REVOKE = "${SESSIONS}/{session_id}/revoke"
      const val ACTIVE = "${SESSIONS}/active"
    }

    internal object EmailAddress {
      const val EMAIL_ADDRESSES = "${ME}/email_addresses"

      internal object WithId {
        const val EMAIL_ADDRESSES_WITH_ID = "${EMAIL_ADDRESSES}/{email_id}"
        const val ATTEMPT_VERIFICATION = "${EMAIL_ADDRESSES_WITH_ID}/attempt_verification"
        const val PREPARE_VERIFICATION = "${EMAIL_ADDRESSES_WITH_ID}/prepare_verification"
      }
    }

    internal object PhoneNumbers {
      const val PHONE_NUMBERS = "${ME}/phone_numbers"

      internal object WithId {
        const val PHONE_NUMBERS_WITH_ID = "${PHONE_NUMBERS}/{phone_number_id}"
        const val ATTEMPT_VERIFICATION = "${PHONE_NUMBERS_WITH_ID}/attempt_verification"
        const val PREPARE_VERIFICATION = "${PHONE_NUMBERS_WITH_ID}/prepare_verification"
      }
    }

    internal object Passkeys {
      const val PASSKEYS = "${ME}/passkeys"

      internal object WithId {
        const val PASSKEYS_WITH_ID = "${PASSKEYS}/{passkey_id}"
        const val ATTEMPT_VERIFICATION = "${PASSKEYS_WITH_ID}/attempt_verification"
      }
    }

    internal object ExternalAccounts {
      const val EXTERNAL_ACCOUNTS = "${ME}/external_accounts"

      internal object WithId {
        const val EXTERNAL_ACCOUNTS_WITH_ID = "${EXTERNAL_ACCOUNTS}/{external_account_id}"
        const val REAUTHORIZE = "${EXTERNAL_ACCOUNTS_WITH_ID}/reauthorize"
        const val REVOKE_TOKENS = "${EXTERNAL_ACCOUNTS_WITH_ID}/tokens"
      }
    }

    internal object TOTP {
      const val TOTP = "${ME}/totp"
      const val ATTEMPT_VERIFICATION = "${TOTP}/attempt_verification"
    }

    const val BACKUP_CODES = "${ME}/backup_codes"
  }
}

internal object CommonParams {
  const val PHONE_NUMBER_ID = "phone_number_id"
  const val EMAIL_ID = "email_id"
  const val SESSION_ID = "session_id"
  const val EXTERNAL_ACCOUNT_ID = "external_account_id"
  const val PASSKEY_ID = "passkey_id"
  const val TOTP_ID = "totp_id"
  const val STRATEGY = "strategy"
  const val CODE = "code"
  const val ID = "id"
  const val CLERK_SESSION_ID = "_clerk_session_id"
}
