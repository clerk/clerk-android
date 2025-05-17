package com.clerk.sdk.network.paths

internal object Paths {
  internal object SignUpPath {
    const val SIGN_UP = "client/sign_ups"

    internal object WithId {
      private const val SIGN_UP_WITH_ID = "client/sign_ups{id}"

      const val PREPARE_VERIFICATION = "${SIGN_UP_WITH_ID}/prepare_verification"

      const val ATTEMPT_VERIFICATION = "${SIGN_UP_WITH_ID}/attempt_verification"
    }
  }
}
