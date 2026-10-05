package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.Constants.Strategy.TRANSFER
import com.clerk.api.biometriccredential.BiometricCredentials
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.PasskeyService
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.toMap
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.toMap
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOService

/*
 * Canonical implementations of the operations that have an entry point on both `Clerk.auth` and
 * `SignIn` / `SignUp`. Each `Clerk.auth` method builds the same typed params the older entry
 * point accepts and calls one of these, so the two layers cannot drift apart again.
 */

/** Creates a sign-in. Backs `Clerk.auth.signIn*` and `SignIn.create(CreateParams.Strategy)`. */
internal suspend fun Auth.createSignIn(
  params: SignIn.CreateParams.Strategy
): ClerkResult<SignIn, ClerkErrorResponse> = reportingFailures {
  when (params) {
    is SignIn.CreateParams.Strategy.Passkey -> PasskeyService.signInWithPasskey()
    is SignIn.CreateParams.Strategy.BiometricCredential ->
      BiometricCredentials.signIn(
        id = params.id,
        identifierHint = params.identifierHint,
        promptTitle = params.promptTitle,
        promptSubtitle = params.promptSubtitle,
      )
    is SignIn.CreateParams.Strategy.Transfer -> postSignIn(mapOf(TRANSFER to "true"))
    else -> postSignIn(params.toMap())
  }
}

private suspend fun postSignIn(params: Map<String, String>) =
  ClerkApi.signIn.createSignIn(params + ("locale" to Clerk.locale.value.orEmpty()))

/**
 * Starts an OAuth or Enterprise SSO sign-in redirect. Backs `Clerk.auth.signInWithOAuth`,
 * `Clerk.auth.signInWithEnterpriseSso` and `SignIn.authenticateWithRedirect`.
 */
internal suspend fun Auth.authenticateSignInWithRedirect(
  params: SignIn.AuthenticateWithRedirectParams,
  transferable: Boolean,
): ClerkResult<OAuthResult, ClerkErrorResponse> = reportingFailures {
  SSOService.authenticateWithRedirect(
    strategy =
      when (params) {
        is SignIn.AuthenticateWithRedirectParams.EnterpriseSSO -> params.strategy
        is SignIn.AuthenticateWithRedirectParams.OAuth -> params.provider.strategy
      },
    redirectUrl = params.redirectUrl,
    identifier = params.identifier,
    emailAddress = params.emailAddress,
    legalAccepted = params.legalAccepted,
    transferable = transferable,
  )
}

/** Creates a sign-up. Backs `Clerk.auth.signUp*` and `SignUp.create(CreateParams)`. */
internal suspend fun Auth.createSignUp(
  params: SignUp.CreateParams
): ClerkResult<SignUp, ClerkErrorResponse> {
  val fields =
    when (params) {
      is SignUp.CreateParams.None -> emptyMap()
      is SignUp.CreateParams.Transfer -> mapOf(TRANSFER to "true")
      is SignUp.CreateParams.Ticket ->
        mapOf("strategy" to SignUp.CreateParams.Ticket.STRATEGY, "ticket" to params.ticket)
      is SignUp.CreateParams.GoogleOneTap ->
        mapOf("strategy" to SignUp.CreateParams.GoogleOneTap.STRATEGY, "token" to params.token)
      else -> params.toMap()
    }
  return postSignUp(fields)
}

/**
 * Creates a sign-up from raw fields, adding the current locale. Unlike the public
 * `SignUp.create(Map)`, which sends the map as given, this always sends `locale`.
 */
internal suspend fun Auth.postSignUp(
  fields: Map<String, String>
): ClerkResult<SignUp, ClerkErrorResponse> = reportingFailures {
  ClerkApi.signUp.createSignUp(fields + ("locale" to Clerk.locale.value.orEmpty()))
}

/**
 * Starts an OAuth or Enterprise SSO sign-up redirect. Backs `Clerk.auth.signUpWithOAuth`,
 * `Clerk.auth.signUpWithEnterpriseSso` and `SignUp.authenticateWithRedirect`.
 */
internal suspend fun Auth.authenticateSignUpWithRedirect(
  params: SignUp.AuthenticateWithRedirectParams
): ClerkResult<OAuthResult, ClerkErrorResponse> = reportingFailures {
  SSOService.authenticateSignUpWithRedirect(
    strategy =
      when (params) {
        is SignUp.AuthenticateWithRedirectParams.EnterpriseSSO -> params.strategy
        is SignUp.AuthenticateWithRedirectParams.OAuth -> params.provider.strategy
      },
    redirectUrl = params.redirectUrl,
    identifier = params.identifier,
    emailAddress = params.emailAddress,
    legalAccepted = params.legalAccepted,
    unsafeMetadata = params.unsafeMetadata,
  )
}
