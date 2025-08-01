package com.clerk.api.user

import com.clerk.api.Clerk
import com.clerk.api.externalaccount.oauthProviderType
import com.clerk.api.network.model.environment.allSocialProviders
import com.clerk.api.sso.OAuthProvider

/**
 * Extension property that returns a list of OAuth providers that the user has not yet connected.
 *
 * This property compares all available social providers configured for the application with the
 * user's verified external accounts to determine which providers are still available for
 * connection.
 *
 * @return List of [OAuthProvider] that the user can still connect to their account
 */
val User.unconnectedProviders: List<OAuthProvider>
  get() {
    val socialProviders = Clerk.environment.allSocialProviders
    val verifiedExternalProviders = this.verifiedExternalAccounts.map { it.oauthProviderType }
    return socialProviders.filter { !verifiedExternalProviders.contains(it) }
  }

/**
 * Extension function that returns the user's full name by combining first and last names.
 *
 * This function concatenates the user's first name and last name with a space between them. If
 * either name is null, it will be treated as an empty string in the concatenation.
 *
 * @return The user's full name as a string, or a string with null values if names are not set
 */
fun User.fullName(): String {
  return "$firstName $lastName"
}
