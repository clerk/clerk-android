package com.clerk.phonenumber

import com.clerk.Clerk
import com.clerk.network.model.verification.Verification

/** Convenience function for telling whether or not the phone number is the primary one */
val PhoneNumber.isPrimary: Boolean
  get() = Clerk.userFlow.value?.primaryPhoneNumberId == this.id

/** Convenience function for telling whether or not the phone number is verified */
val PhoneNumber.isVerified: Boolean
  get() = this.verification?.status == Verification.Status.VERIFIED
