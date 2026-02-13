package com.clerk.api.auth

import com.clerk.api.auth.builders.EnterpriseSsoBuilder
import com.clerk.api.auth.builders.SignInIdentifierBuilder
import com.clerk.api.auth.builders.SignInWithIdTokenBuilder
import com.clerk.api.auth.builders.SignInWithOtpBuilder
import com.clerk.api.auth.builders.SignInWithPasswordBuilder
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import kotlin.jvm.JvmName

@Deprecated("Use Auth.signIn", level = DeprecationLevel.HIDDEN)
@JvmName("signIn")
suspend fun signInCompat(
  auth: Auth,
  block: SignInIdentifierBuilder.() -> Unit,
): ClerkResult<SignIn, ClerkErrorResponse> = auth.signIn(block)

@Deprecated("Use Auth.signInWithPassword", level = DeprecationLevel.HIDDEN)
@JvmName("signInWithPassword")
suspend fun signInWithPasswordCompat(
  auth: Auth,
  block: SignInWithPasswordBuilder.() -> Unit,
): ClerkResult<SignIn, ClerkErrorResponse> = auth.signInWithPassword(block)

@Deprecated("Use Auth.signInWithOtp", level = DeprecationLevel.HIDDEN)
@JvmName("signInWithOtp")
suspend fun signInWithOtpCompat(
  auth: Auth,
  block: SignInWithOtpBuilder.() -> Unit,
): ClerkResult<SignIn, ClerkErrorResponse> = auth.signInWithOtp(block)

@Deprecated("Use Auth.signInWithOAuth", level = DeprecationLevel.HIDDEN)
@JvmName("signInWithOAuth")
suspend fun signInWithOAuthCompat(
  auth: Auth,
  provider: OAuthProvider,
): ClerkResult<OAuthResult, ClerkErrorResponse> = auth.signInWithOAuth(provider)

@Deprecated("Use Auth.signInWithIdToken", level = DeprecationLevel.HIDDEN)
@JvmName("signInWithIdToken")
suspend fun signInWithIdTokenCompat(
  auth: Auth,
  block: SignInWithIdTokenBuilder.() -> Unit,
): ClerkResult<OAuthResult, ClerkErrorResponse> = auth.signInWithIdToken(block)

@Deprecated("Use Auth.signInWithPasskey", level = DeprecationLevel.HIDDEN)
@JvmName("signInWithPasskey")
suspend fun signInWithPasskeyCompat(auth: Auth): ClerkResult<SignIn, ClerkErrorResponse> =
  auth.signInWithPasskey()

@Deprecated("Use Auth.signInWithAccountPortal", level = DeprecationLevel.HIDDEN)
@JvmName("signInWithAccountPortal")
suspend fun signInWithAccountPortalCompat(
  auth: Auth
): ClerkResult<OAuthResult, ClerkErrorResponse> = auth.signInWithAccountPortal()

@Deprecated("Use Auth.signInWithEnterpriseSso", level = DeprecationLevel.HIDDEN)
@JvmName("signInWithEnterpriseSso")
suspend fun signInWithEnterpriseSsoCompat(
  auth: Auth,
  block: EnterpriseSsoBuilder.() -> Unit,
): ClerkResult<OAuthResult, ClerkErrorResponse> = auth.signInWithEnterpriseSso(block)

@Deprecated("Use Auth.signInWithTicket", level = DeprecationLevel.HIDDEN)
@JvmName("signInWithTicket")
suspend fun signInWithTicketCompat(
  auth: Auth,
  ticket: String,
): ClerkResult<SignIn, ClerkErrorResponse> = auth.signInWithTicket(ticket)
