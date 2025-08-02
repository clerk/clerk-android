package com.clerk.api.emailaddress

import com.clerk.api.Clerk
import com.clerk.api.network.model.verification.Verification

/** Convenience function to tell whether the email is the user's primary email. */
val EmailAddress.isPrimary: Boolean
  get() = Clerk.userFlow.value?.primaryEmailAddressId == id

/** Convenience function to tell whether the email is verified. */
val EmailAddress.isVerified: Boolean
  get() = verification?.status == Verification.Status.VERIFIED
