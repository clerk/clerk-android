package com.clerk.ui.signin

import com.clerk.api.*
import kotlinx.serialization.json.JsonPrimitive

/** The shared mobile flow owns browser callback interpretation and transfer. */
internal suspend fun authenticateWithRedirect(
  clerk: Clerk,
  provider: OAuthProvider,
  transferable: Boolean,
): MobileAuthenticationResult =
  clerk.authenticateWithSSO(
    MobileSSOParams(
      strategy =
        SignInSSOParamsStrategy.fromJson(
          JsonPrimitive("oauth_${provider.rawValue}"),
          clerk.context.requireRuntime(),
        ),
      start = MobileSSOParamsStart.SignIn,
      transferable = transferable,
    )
  )
