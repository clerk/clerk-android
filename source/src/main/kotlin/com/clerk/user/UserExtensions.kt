package com.clerk.user

import com.clerk.Clerk
import com.clerk.log.ClerkLog
import com.clerk.network.model.account.oauthProviderType
import com.clerk.network.model.environment.allSocialProviders
import com.clerk.sso.OAuthProvider

val User.unconnectedProviders: List<OAuthProvider>
  get() {
    val socialProviders = Clerk.environment.allSocialProviders
    val verifiedExternalProviders = this.verifiedExternalAccounts.map { it.oauthProviderType }

    ClerkLog.e("Social providers: $socialProviders")
    ClerkLog.e("Verified providers: $verifiedExternalProviders")

    return socialProviders.filter { !verifiedExternalProviders.contains(it) }
  }

fun User.fullName(): String {
  return "$firstName $lastName"
}
