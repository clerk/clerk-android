package com.clerk.emailaddress

import com.clerk.Clerk
import com.clerk.network.model.verification.Verification

/** Convenience function to tell whether the email is the user's primary email. */
val EmailAddress.isPrimary: Boolean
  get() = Clerk.user?.primaryEmailAddressId == id

/** Convenience function to tell whether the email is verified. */
val EmailAddress.isVerified: Boolean
  get() = verification?.status == Verification.Status.VERIFIED
