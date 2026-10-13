package com.clerk.e2e

import java.util.UUID

internal object E2EFixtures {
  const val SIGN_UP_PHONE_E164 = "+15555550160"

  val signUpEmail: String by lazy { uniqueTestEmail() }

  fun uniqueTestEmail(uuid: UUID = UUID.randomUUID()): String {
    val suffix = uuid.toString().replace("-", "")
    return "clerk_android_e2e+clerk_test_$suffix@example.com"
  }
}
