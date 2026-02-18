package com.clerk.api.auth

import com.clerk.api.auth.builders.EnterpriseSsoBuilder
import com.clerk.api.auth.builders.SignUpBuilder
import com.clerk.api.auth.builders.SignUpWithIdTokenBuilder
import com.clerk.api.auth.types.IdTokenProvider
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import kotlin.jvm.JvmName

@Deprecated("Use Auth.signUp", level = DeprecationLevel.HIDDEN)
@JvmName("signUp")
suspend fun signUpCompat(
  auth: Auth,
  block: SignUpBuilder.() -> Unit,
): ClerkResult<SignUp, ClerkErrorResponse> = auth.signUp(block)

@Deprecated("Use Auth.signUpWithOAuth", level = DeprecationLevel.HIDDEN)
@JvmName("signUpWithOAuth")
suspend fun signUpWithOAuthCompat(
  auth: Auth,
  provider: OAuthProvider,
): ClerkResult<OAuthResult, ClerkErrorResponse> = auth.signUpWithOAuth(provider)

@Deprecated("Use Auth.signUpWithIdToken", level = DeprecationLevel.HIDDEN)
@JvmName("signUpWithIdToken")
suspend fun signUpWithIdTokenCompat(
  auth: Auth,
  token: String,
  provider: IdTokenProvider,
  block: SignUpWithIdTokenBuilder.() -> Unit = {},
): ClerkResult<SignUp, ClerkErrorResponse> = auth.signUpWithIdToken(token, provider, block)

@Deprecated("Use Auth.signUpWithAccountPortal", level = DeprecationLevel.HIDDEN)
@JvmName("signUpWithAccountPortal")
suspend fun signUpWithAccountPortalCompat(
  auth: Auth
): ClerkResult<OAuthResult, ClerkErrorResponse> = auth.signUpWithAccountPortal()

@Deprecated("Use Auth.signUpWithEnterpriseSso", level = DeprecationLevel.HIDDEN)
@JvmName("signUpWithEnterpriseSso")
suspend fun signUpWithEnterpriseSsoCompat(
  auth: Auth,
  block: EnterpriseSsoBuilder.() -> Unit,
): ClerkResult<OAuthResult, ClerkErrorResponse> = auth.signUpWithEnterpriseSso(block)

@Deprecated("Use Auth.signUpWithTicket", level = DeprecationLevel.HIDDEN)
@JvmName("signUpWithTicket")
suspend fun signUpWithTicketCompat(
  auth: Auth,
  ticket: String,
): ClerkResult<SignUp, ClerkErrorResponse> = auth.signUpWithTicket(ticket)
