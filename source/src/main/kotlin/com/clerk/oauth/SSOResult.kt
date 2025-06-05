package com.clerk.oauth

import com.clerk.signin.SignIn
import com.clerk.signup.SignUp
import kotlinx.serialization.Serializable

/**
 * The result of an SSO operation.
 *
 * @property signIn The sign-in object if the SSO operation resulted in a sign-in.
 * @property signUp The sign-up object if the SSO operation resulted in a sign-up.
 *
 * This is used since SSO can result in either a sign-in or a sign-up, depending on the user's
 * state. We handle the transfer automatically in the SDK, so you don't have to worry about it.
 */
@Serializable
data class SSOResult(val signIn: SignIn? = null, val signUp: SignUp? = null) {

  /**
   * Convenience property to determine the type of result.
   *
   * @return The type of result, either [ResultType.SIGN_IN] or [ResultType.SIGN_UP].
   */
  val resultType
    get() = if (signIn != null) ResultType.SIGN_IN else ResultType.SIGN_UP
}

enum class ResultType {
  SIGN_IN,
  SIGN_UP,
}
