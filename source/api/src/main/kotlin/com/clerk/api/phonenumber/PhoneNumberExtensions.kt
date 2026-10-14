package com.clerk.api.phonenumber

import com.clerk.api.Clerk
import com.clerk.api.network.model.verification.Verification

/** Convenience function for telling whether or not the phone number is the primary one */
public val PhoneNumber.isPrimary: Boolean
  get() = Clerk.userFlow.value?.primaryPhoneNumberId == this.id

/** Convenience function for telling whether or not the phone number is verified */
public val PhoneNumber.isVerified: Boolean
  get() = this.verification?.status == Verification.Status.VERIFIED
