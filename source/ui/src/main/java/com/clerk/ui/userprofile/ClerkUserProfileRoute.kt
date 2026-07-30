package com.clerk.ui.userprofile

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Marks the root of Clerk's user profile inside a host-owned navigation back stack.
 *
 * Push this key to open the profile; register the profile's screens with [clerkUserProfileEntries].
 * The profile removes its own keys from the stack when the flow ends (root back navigation,
 * sign-out, account deletion).
 */
@Serializable object ClerkUserProfileRoute : NavKey
