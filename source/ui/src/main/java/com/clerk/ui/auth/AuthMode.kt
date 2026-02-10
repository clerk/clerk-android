package com.clerk.ui.auth

enum class AuthMode {
  SignIn,
  SignUp,
  SignInOrUp,
}

/**
 * Whether this auth mode allows sign-in attempts to transfer into sign-up flows when the user
 * doesn't have an account.
 */
val AuthMode.transferable: Boolean
  get() = this != AuthMode.SignIn
