package com.clerk.sdk.network.paths

@Suppress("MemberNameEqualsClassName")
internal object Paths {
  internal object ClientPath {
    internal const val CLIENT = "client"

    internal object DeviceAttestation {
      const val DEVICE_ATTESTATION = "${CLIENT}/device_attestation"
      const val CHALLENGES = "${DEVICE_ATTESTATION}/challenges"
      const val VERIFY = "${DEVICE_ATTESTATION}/verify"
    }

    internal object Sessions {
      internal const val SESSIONS = "${CLIENT}/sessions"

      internal object WithId {
        private const val SESSIONS_WITH_ID = "${SESSIONS}{id}"

        internal const val REMOVE = "${SESSIONS_WITH_ID}/remove"
        internal const val TOKENS = "${SESSIONS_WITH_ID}/tokens"
        internal const val TEMPLATE = "${TOKENS}/{template}"
      }
    }

    internal object SignIns {
      internal const val SIGN_INS = "${CLIENT}/sign_ins"

      internal object WithId {
        internal const val SIGN_INS_WITH_ID = "${SIGN_INS}{id}"

        internal const val ATTEMPT_FIRST_FACTOR = "${SIGN_INS_WITH_ID}/attempt_first_factor"
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

      const val PREPARE_VERIFICATION = "${SIGN_UP_WITH_ID}/prepare_verification"
      const val ATTEMPT_VERIFICATION = "${SIGN_UP_WITH_ID}/attempt_verification"
    }
  }

  internal const val ENVIRONMENT = "environment"
}
