package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.auth.types.Strategy as AuthStrategy
import com.clerk.api.biometriccredential.BiometricCredentials
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.toFormFields
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOService

internal suspend fun Auth.createSignIn(
  params: SignIn.CreateParams.Strategy
): ClerkResult<SignIn, ClerkErrorResponse> = reportingFailures {
  when (params) {
    is SignIn.CreateParams.Strategy.Passkey -> signInWithPasskey()
    is SignIn.CreateParams.Strategy.BiometricCredential ->
      BiometricCredentials.signIn(
        id = params.id,
        identifierHint = params.identifierHint,
        promptTitle = params.promptTitle,
        promptSubtitle = params.promptSubtitle,
      )
    is SignIn.CreateParams.Strategy.Transfer ->
      postSignIn(mapOf(AuthStrategy.Transfer.value to "true"))
    is SignIn.CreateParams.Strategy.EmailCode,
    is SignIn.CreateParams.Strategy.PhoneCode,
    is SignIn.CreateParams.Strategy.Password,
    is SignIn.CreateParams.Strategy.ResetPasswordEmailCode,
    is SignIn.CreateParams.Strategy.ResetPasswordPhoneCode,
    is SignIn.CreateParams.Strategy.Ticket,
    is SignIn.CreateParams.Strategy.Identifier -> postSignIn(params.toFormFields())
  }
}

private suspend fun postSignIn(params: Map<String, String>) =
  ClerkApi.signIn.createSignIn(params + ("locale" to Clerk.locale.value.orEmpty()))

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

internal suspend fun Auth.createSignUp(
  params: SignUp.CreateParams
): ClerkResult<SignUp, ClerkErrorResponse> {
  val fields =
    when (params) {
      is SignUp.CreateParams.None -> emptyMap()
      is SignUp.CreateParams.Transfer -> mapOf(AuthStrategy.Transfer.value to "true")
      is SignUp.CreateParams.Ticket ->
        mapOf("strategy" to AuthStrategy.Ticket.value, "ticket" to params.ticket)
      is SignUp.CreateParams.GoogleOneTap ->
        mapOf("strategy" to AuthStrategy.GoogleOneTap.value, "token" to params.token)
      is SignUp.CreateParams.Standard -> params.toFormFields()
    }
  return postSignUp(fields)
}

internal suspend fun Auth.postSignUp(
  fields: Map<String, String>
): ClerkResult<SignUp, ClerkErrorResponse> = reportingFailures {
  ClerkApi.signUp.createSignUp(fields + ("locale" to Clerk.locale.value.orEmpty()))
}

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
