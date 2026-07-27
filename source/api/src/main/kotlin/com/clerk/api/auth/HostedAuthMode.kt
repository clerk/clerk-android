package com.clerk.api.auth

/** The Account Portal screen shown when hosted authentication starts. */
enum class HostedAuthMode(internal val value: String) {
  /** Opens Account Portal on sign-in. */
  SIGN_IN("sign-in"),

  /** Opens Account Portal on sign-up. */
  SIGN_UP("sign-up"),
}
