package com.clerk.api.sso

import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import kotlinx.serialization.Serializable

/**
 * The result of an SSO operation.
 *
 * @property signIn The sign-in object if the SSO operation resulted in a sign-in.
 * @property signUp The sign-up object if the SSO operation resulted in a sign-up.
 *
 * This is used since SSO can result in either a sign-in or a sign-up, depending on the user's
 * state. We handle the transfer automatically in the SDK, so you don't have to worry about it, but
 * this is the object that will be returned to you. Prefer [outcome], which makes the possible cases
 * explicit:
 * ```kotlin
 * when (val outcome = result.outcome) {
 *   is OAuthResult.Outcome.SignIn -> handle(outcome.signIn)
 *   is OAuthResult.Outcome.SignUp -> handle(outcome.signUp)
 *   OAuthResult.Outcome.Empty -> showError()
 * }
 * ```
 */
@Serializable
public data class OAuthResult(val signIn: SignIn? = null, val signUp: SignUp? = null) {

  /**
   * Convenience property to determine the type of result.
   *
   * Returns [ResultType.SIGN_UP] when neither [signIn] nor [signUp] is set; use [outcome], which
   * reports that case as [Outcome.Empty].
   *
   * @return The type of result, either [ResultType.SIGN_IN] or [ResultType.SIGN_UP].
   */
  @Deprecated(
    "Use outcome instead and switch on its cases: Outcome.SignIn (replaces ResultType.SIGN_IN, " +
      "carries a non-null signIn), Outcome.SignUp (replaces ResultType.SIGN_UP, carries a " +
      "non-null signUp) and Outcome.Empty (neither was returned; resultType reports this as " +
      "SIGN_UP)."
  )
  val resultType: ResultType
    get() = if (signIn != null) ResultType.SIGN_IN else ResultType.SIGN_UP

  /**
   * The result as a closed set of cases, each carrying its non-null resource. A [signIn] takes
   * precedence over a [signUp], matching [resultType].
   */
  val outcome: Outcome
    get() =
      when {
        signIn != null -> Outcome.SignIn(signIn)
        signUp != null -> Outcome.SignUp(signUp)
        else -> Outcome.Empty
      }

  /** What an SSO operation produced. */
  public sealed interface Outcome {
    /** The operation completed or continued a sign-in. */
    public data class SignIn(val signIn: com.clerk.api.signin.SignIn) : Outcome

    /** The operation created or continued a sign-up (for example, a new user via OAuth). */
    public data class SignUp(val signUp: com.clerk.api.signup.SignUp) : Outcome

    /** The operation returned neither a sign-in nor a sign-up. */
    public data object Empty : Outcome
  }
}

@kotlinx.serialization.Serializable
public enum class ResultType {
  SIGN_IN,
  SIGN_UP,
  UNKNOWN,
}
